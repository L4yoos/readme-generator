package com.example.readmegenerator.adapter.prompt;

import com.example.readmegenerator.domain.model.ReadmeGenerationConfig;
import com.example.readmegenerator.domain.port.PromptBuilderPort;

public class DefaultPromptBuilder implements PromptBuilderPort {

    @Override
    public String build(String summary, String projectName, ReadmeGenerationConfig config) {
        if (summary.length() > 10000) {
            summary = summary.substring(0, 10000) + "\n\n[...Content trimmed due to size limits...]";
        }

        String alignAttr = switch (config.getHeaderAlignment()) {
            case CENTER -> "center";
            case RIGHT -> "right";
            default -> "left";
        };

        String listTag = config.getListStyle() == ReadmeGenerationConfig.ListStyle.NUMBERED ? "ol" : "ul";
        String listTypeNote = config.getListStyle() == ReadmeGenerationConfig.ListStyle.NUMBERED
                ? "- Use `<ol>` and `<li>` for all lists to generate numbered lists.\n"
                : "- Use `<ul>` and `<li>` for all lists to generate bullet points.\n";

        return "You are an expert open-source documentation writer.\n\n" +
                "Your task is to generate a professional `README.md` file in **Markdown**, using **HTML for structure**.\n\n" +
                "# 📘 Style Guide:\n\n" +
                "- Begin with:\n" +
                "  `<h1 align=\"" + alignAttr + "\">" + projectName + "</h1> to projectName add emoji similar`\n" +
                "  `<p align=\"" + alignAttr + "\">A catchy one-liner description</p>`\n\n" +
                "- Include GitHub badges inside:\n" +
                "```html\n" +
                "<p align=\"" + alignAttr + "\">\n" +
                "  <a href=\"https://github.com/{user}/{project}/commits/main\">\n" +
                "    <img src=\"https://img.shields.io/github/last-commit/{user}/{project}\" alt=\"Last Commit\">\n" +
                "  </a>\n" +
                "  <a href=\"https://github.com/{user}/{project}\">\n" +
                "    <img src=\"https://img.shields.io/github/languages/top/{user}/{project}\" alt=\"Top Language\">\n" +
                "  </a>\n" +
                "  <a href=\"https://github.com/{user}/{project}\">\n" +
                "    <img src=\"https://img.shields.io/github/languages/count/{user}/{project}\" alt=\"Language Count\">\n" +
                "  </a>\n" +
                "</p>\n" +
                "```\n\n" +
                "- Use the following section structure:\n" +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"overview\">🚀 Overview</h2>` + `<p align=\"" + alignAttr + "\">...</p>` + `<" + listTag + " align=\"" + alignAttr + "\">`\n" +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"built-with\">📦 Built With</h2>` + `<p align=\"" + alignAttr + "\">...</p>` + `<" + listTag + " align=\"" + alignAttr + "\">`\n" +
                "  `<p align=\"" + alignAttr + "\">This project is built using the following core technologies, frameworks, libraries, and **CI/CD tools and configurations**:</p>` + " +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"table-of-contents\">📚 Table of Contents</h2>`\n" +
                "    `<p align=\"" + alignAttr + "\">This README is organized into the following sections:</p>`\n" +
                "    `<" + listTag + " align=\"" + alignAttr + "\">\n" +
                "      <li><a href=\"#overview\">Overview</a></li>\n" +
                "      <li><a href=\"#built-with\">Built With</a></li>\n" +
                "      <li><a href=\"#table-of-contents\">Table of Contents</a></li>\n" +
                "      <li><a href=\"#architecture\">Architecture</a></li>\n" +
                "      <li><a href=\"#prerequisites\">Prerequisites</a></li>\n" +
                "      <li><a href=\"#installation\">Installation</a></li>\n" +
                "      <li><a href=\"#usage\">Usage</a></li>\n" +
                "      <li><a href=\"#testing\">Testing</a></li>\n" +
                "      <li><a href=\"#demo\">Demo</a></li>\n" +
                "    </" + listTag + ">`\n" +
                "  - In the `<h2 id=\"architecture\">🏗️ Architecture</h2>` section:\n" +
                "  - Analyze the summary and detect architectural patterns like `layered architecture`, `hexagonal`, `onion`, `clean`, or `microservices`.\n" +
                "  - Describe the architecture with a short paragraph.\n" +
                "  - If no architecture is mentioned, provide a general structure suggestion based on typical project layout.\n\n" +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"prerequisites\">✅ Prerequisites</h2>` + `<p align=\"" + alignAttr + "\">...</p>` + `<" + listTag + " align=\"" + alignAttr + "\">`\n" +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"installation\">🛠️ Installation</h2>` + `<p align=\"" + alignAttr + "\">...</p>` + `<" + listTag + " align=\"" + alignAttr + "\">`\n" +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"usage\">🚀 Usage</h2>` + `<p align=\"" + alignAttr + "\">...</p>` + `<" + listTag + " align=\"" + alignAttr + "\">`\n" +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"testing\">🧪 Testing</h2>` + `<p align=\"" + alignAttr + "\">...</p>` + `<" + listTag + " align=\"" + alignAttr + "\">`\n" +
                "  - `<h2 align=\"" + alignAttr + "\" id=\"demo\">🎬 Demo</h2>` + `<p align=\"" + alignAttr + "\">...</p>`\n\n" +
                "- Insert `<hr/>` between each section.\n" +
                "- Use only HTML tags: `<h1>`, `<h2>`, `<p>`, `<" + listTag + ">`, `<li>`, `<hr/>`.\n" +
                "- Every block must include `align=\"" + alignAttr + "\"`.\n" +
                listTypeNote +
                "- Format code with backticks and keep tone technical but friendly.\n\n" +
                "# 📝 INPUT (Project Summary):\n\n" + summary + "\n\n" +
                "# 🎯 OUTPUT FORMAT:\n\n" +
                "- Respond with raw HTML/Markdown representing the README.\n" +
                "- Do NOT include explanations or markdown comments.\n\n" +
                "➕ At the end, include this note:\n" +
                "`> 📝 **Note**: Replace {user} and {project} in the badge URLs with your actual GitHub username and repository name.`";
    }
}
