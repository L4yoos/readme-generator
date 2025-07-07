package com.example.readmegenerator.adapter.analyzer;

import com.example.readmegenerator.domain.port.ProjectAnalyzerPort;
import com.example.readmegenerator.domain.service.DependencyExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileProjectAnalyzer implements ProjectAnalyzerPort {

    private static final Logger logger = LoggerFactory.getLogger(FileProjectAnalyzer.class);

    private static final Set<String> TEXT_BASED_FILES = Set.of(
            "cmakelists.txt", "makefile"
    );

    private static final Set<String> BUILD_FILES = Set.of(
            "pom.xml", "build.gradle", "package.json", "requirements.txt", "docker-compose.yml"
    );

    private static final Set<String> DEPENDENCY_FILES = Set.of(
            "composer.json", "package.json", "requirements.txt", "pyproject.toml"
    );

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".php", ".js", ".py", ".cpp", ".c", ".h", ".hpp", ".ts"
    );

    private static final Set<String> CI_CD_PATTERNS = Set.of(
            ".github/workflows/",
            "gitlab-ci.yml",
            ".azure-pipelines/",
            "jenkinsfile",
            "circle.yml",
            ".travis.yml",
            "docker-compose.yml"
    );

    @Override
    public String analyze(List<Path> files) throws IOException {
        logger.debug("Starting analysis of {} files", files.size());

        StringBuilder sb = new StringBuilder();
        List<Path> ciCdFiles = new ArrayList<>();

        Path root = findCommonRoot(files);
        if (root != null) {
            logger.debug("Detected root directory: {}", root);
            List<String> subprojects = detectSubprojects(root);

            if (subprojects.size() > 1) {
                sb.append("This project appears to be a monorepo containing the following services:\n");
                for (String service : subprojects) {
                    sb.append("- ").append(service).append("\n");
                }
                sb.append("\n");
            }
        } else {
            logger.debug("Could not detect root directory. Analysis might be less precise.");
        }

        for (Path file : files) {
            String normalizedFullPath = file.normalize().toString().toLowerCase().replace("\\", "/");
            String fileName = file.getFileName().toString().toLowerCase();
            logger.debug("Inspecting file: {}", normalizedFullPath);

            if (fileName.contains("test")) {
                logger.debug("Skipping test file: {}", fileName);
                continue;
            }

            if (isCiCdFile(normalizedFullPath, fileName)) {
                ciCdFiles.add(file);
                logger.debug("Identified CI/CD file for processing: {}", normalizedFullPath);
            }

            if ("pom.xml".equals(fileName)) {
                appendMavenProjectSummary(sb, file);
            }
            else if ("docker-compose.yml".equals(fileName)) {
                appendDockerComposeSummary(sb, file);
            }
            else if (TEXT_BASED_FILES.contains(fileName)) {
                appendTextFileSummary(sb, fileName, file);
            }

            else if (DEPENDENCY_FILES.contains(fileName)) {
                logger.debug("Found dependency file: {}", fileName);
                String extracted = DependencyExtractor.extractDependencies(file);
                if (!extracted.isBlank()) {
                    sb.append("### Detected Dependencies from ").append(fileName).append(":\n");
                    sb.append(extracted).append("\n\n");
                }
            }

            else if (hasCodeExtension(fileName)) {
                appendCodeFileSummary(sb, fileName, file);
            }
        }

        if (!ciCdFiles.isEmpty()) {
            sb.append("---").append("\n");
            sb.append("### Detected CI/CD Tools and Configuration Files:\n\n");
            sb.append("The project utilizes the following CI/CD tools and configuration files:\n");
            sb.append("<ul>\n");

            for (Path ciCdFile : ciCdFiles) {
                String ciCdDisplayPath = root != null ? root.relativize(ciCdFile).toString() : ciCdFile.getFileName().toString();
                ciCdDisplayPath = ciCdDisplayPath.replace("\\", "/");

                String toolName = getCiCdToolName(ciCdFile); // Nowa metoda pomocnicza
                sb.append("  <li>**").append(toolName).append("**: `").append(ciCdDisplayPath).append("`</li>\n");
            }
            sb.append("</ul>\n\n");
        }

        return sb.toString();
    }

    private void appendMavenProjectSummary(StringBuilder sb, Path filePath) {
        sb.append("### Maven Project Configuration (pom.xml):\n");
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(filePath.toFile());
            doc.getDocumentElement().normalize();

            String groupId = getTagValue("groupId", doc.getDocumentElement());
            String artifactId = getTagValue("artifactId", doc.getDocumentElement());
            String version = getTagValue("version", doc.getDocumentElement());
            String name = getTagValue("name", doc.getDocumentElement());
            String description = getTagValue("description", doc.getDocumentElement());
            String javaVersion = getProperty("java.version", doc.getDocumentElement());

            if (name != null && !name.isBlank()) sb.append("  - Name: ").append(name).append("\n");
            if (groupId != null && !groupId.isBlank()) sb.append("  - Group ID: ").append(groupId).append("\n");
            if (artifactId != null && !artifactId.isBlank()) sb.append("  - Artifact ID: ").append(artifactId).append("\n");
            if (version != null && !version.isBlank()) sb.append("  - Version: ").append(version).append("\n");
            if (javaVersion != null && !javaVersion.isBlank()) sb.append("  - Java Version: ").append(javaVersion).append("\n");
            if (description != null && !description.isBlank()) sb.append("  - Description: ").append(description).append("\n");

            sb.append("  - Key Dependencies:\n");
            NodeList dependencyNodes = doc.getElementsByTagName("dependency");
            int count = 0;
            for (int i = 0; i < dependencyNodes.getLength() && count < 10; i++) {
                Node node = dependencyNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String depGroupId = getTagValue("groupId", element);
                    String depArtifactId = getTagValue("artifactId", element);
                    String depVersion = getTagValue("version", element);
                    String scope = getTagValue("scope", element);

                    if (scope != null && scope.toLowerCase().contains("test")) {
                        continue;
                    }

                    sb.append("    - ");
                    if (depGroupId != null) sb.append(depGroupId).append(":");
                    if (depArtifactId != null) sb.append(depArtifactId);
                    if (depVersion != null) sb.append(":").append(depVersion);
                    sb.append("\n");
                    count++;
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing pom.xml at {}: {}", filePath, e.getMessage());
            sb.append("  (Error parsing pom.xml: ").append(e.getMessage()).append(")\n");
        }
        sb.append("\n");
    }

    private void appendDockerComposeSummary(StringBuilder sb, Path filePath) {
        sb.append("### Docker Compose Configuration (").append(filePath.getFileName()).append("):\n");
        try (Reader reader = Files.newBufferedReader(filePath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(reader);

            if (data != null && data.containsKey("services")) {
                sb.append("  - Defined Services:\n");
                Map<String, Object> services = (Map<String, Object>) data.get("services");
                for (Map.Entry<String, Object> entry : services.entrySet()) {
                    String serviceName = entry.getKey();
                    Map<String, Object> serviceConfig = (Map<String, Object>) entry.getValue();
                    String image = (String) serviceConfig.get("image");
                    Object buildConfig = serviceConfig.get("build");

                    sb.append("    - ").append(serviceName);
                    if (image != null) {
                        sb.append(" (Image: `").append(image).append("`)");
                    } else if (buildConfig != null) {
                        if (buildConfig instanceof String) {
                            sb.append(" (Build Context: `").append(buildConfig).append("`)");
                        } else if (buildConfig instanceof Map) {
                            Map<String, Object> buildMap = (Map<String, Object>) buildConfig;
                            String context = (String) buildMap.get("context");
                            if (context != null) {
                                sb.append(" (Build Context: `").append(context).append("`)");
                            } else {
                                String dockerfile = (String) buildMap.get("dockerfile");
                                if (dockerfile != null) {
                                    sb.append(" (Dockerfile: `").append(dockerfile).append("`)");
                                }
                            }
                        }
                    }
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing docker-compose.yml at {}: {}", filePath, e.getMessage());
            sb.append("  (Error parsing docker-compose.yml: ").append(e.getMessage()).append(")\n");
        }
        sb.append("\n");
    }

    private void appendTextFileSummary(StringBuilder sb, String label, Path file) throws IOException {
        sb.append("### Content Summary of ").append(label).append(":\n");
        try (Stream<String> lines = Files.lines(file)) {
            List<String> meaningfulLines = lines
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#")) // Pomiń puste linie i komentarze
                    .limit(10)
                    .collect(Collectors.toList());

            if (!meaningfulLines.isEmpty()) {
                sb.append("```\n");
                meaningfulLines.forEach(line -> sb.append(line).append("\n"));
                sb.append("```\n");
            } else {
                sb.append("  (File appears empty or contains only comments/whitespace)\n");
            }
        }
        sb.append("\n\n");
    }

    private void appendCodeFileSummary(StringBuilder sb, String fileName, Path file) throws IOException {
        sb.append("### Code File Summary: `").append(file.getFileName()).append("`\n");
        List<String> lines = Files.readAllLines(file);

        Predicate<String> filter = getLanguageLineFilter(fileName);

        String summaryContent = lines.stream()
                .filter(filter)
                .map(String::trim)
                .limit(5)
                .collect(Collectors.joining("\n"));

        if (!summaryContent.isBlank()) {
            sb.append("```").append(getExtensionWithoutDot(fileName)).append("\n");
            sb.append(summaryContent).append("\n");
            sb.append("```\n");
        } else {
            sb.append("  (General code file, key structure: ").append(lines.stream().limit(3).collect(Collectors.joining(" "))).append("...)\n");
        }
        sb.append("\n");
    }

    private String getProperty(String propertyName, Element rootElement) {
        NodeList propertiesList = rootElement.getElementsByTagName("properties");
        if (propertiesList.getLength() > 0) {
            Element propertiesElement = (Element) propertiesList.item(0);
            NodeList propertyNodes = propertiesElement.getElementsByTagName(propertyName);
            if (propertyNodes.getLength() > 0) {
                return propertyNodes.item(0).getTextContent().trim();
            }
        }
        return null;
    }

    private String getCiCdToolName(Path ciCdFile) {
        String fileName = ciCdFile.getFileName().toString().toLowerCase();
        String fullPathNormalized = ciCdFile.normalize().toString().toLowerCase().replace("\\", "/");

        if (fullPathNormalized.contains(".github/workflows/")) {
            return "GitHub Actions";
        } else if (fileName.equals("gitlab-ci.yml")) {
            return "GitLab CI";
        } else if (fullPathNormalized.contains(".azure-pipelines/")) {
            return "Azure Pipelines";
        } else if (fileName.equals("jenkinsfile")) {
            return "Jenkins Pipeline";
        } else if (fileName.equals("circle.yml")) {
            return "CircleCI";
        } else if (fileName.equals(".travis.yml")) {
            return "Travis CI";
        } else if (fileName.equals("docker-compose.yml")) {
            return "Docker Compose"; // Uproszczona nazwa dla Readme
        }
        return "Unknown CI/CD Tool";
    }

    private String getExtensionWithoutDot(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    private boolean isCiCdFile(String normalizedFullPath, String fileName) {
        if (CI_CD_PATTERNS.contains(fileName)) {
            return true;
        }
        for (String pattern : CI_CD_PATTERNS) {
            if (pattern.endsWith("/") && normalizedFullPath.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private Predicate<String> getLanguageLineFilter(String fileName) {
        if (fileName.endsWith(".cpp") || fileName.endsWith(".c") || fileName.endsWith(".h") || fileName.endsWith(".hpp")) {
            return line -> line.matches("^\\s*(#include|#define|class\\s+\\w+|\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{).*");
        } else if (fileName.endsWith(".java")) {
            return line -> line.matches("^\\s*(public|protected|private)?\\s*(abstract|final|static)?\\s*(class|interface|enum)\\s+\\w+.*") ||
                    line.matches("^\\s*(public|protected|private)?\\s*(static|final)?\\s*\\w+\\s+\\w+\\s*\\(.*\\).*");
        } else if (fileName.endsWith(".py")) {
            return line -> line.matches("^\\s*(class|def)\\s+\\w+.*");
        } else if (fileName.endsWith(".js") || fileName.endsWith(".ts")) { // TypeScript też
            return line -> line.matches("^\\s*(class|function|const\\s+\\w+\\s*=|let\\s+\\w+\\s*=|var\\s+\\w+\\s*=|export).*");
        } else if (fileName.endsWith(".php")) {
            return line -> line.matches("^\\s*(class|interface|trait|function)\\s+\\w+.*");
        }
        return line -> false;
    }

    private List<String> detectSubprojects(Path root) throws IOException {
        List<String> subprojects = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    boolean hasBuildFile = Files.list(path)
                            .anyMatch(p -> BUILD_FILES.contains(p.getFileName().toString().toLowerCase()));
                    if (hasBuildFile) {
                        subprojects.add(path.getFileName().toString());
                    }
                }
            }
        }
        return subprojects;
    }

    private Path findCommonRoot(List<Path> files) {
        if (files.isEmpty()) return null;
        Path commonRoot = files.get(0).getParent();
        for (Path file : files) {
            Path parent = file.getParent();
            while (parent != null && !parent.startsWith(commonRoot)) {
                commonRoot = commonRoot.getParent();
            }
        }
        return commonRoot;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return null;
    }

    private boolean hasCodeExtension(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(lowerCaseFileName::endsWith);
    }
}