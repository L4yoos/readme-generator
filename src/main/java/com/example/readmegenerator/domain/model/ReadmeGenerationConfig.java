package com.example.readmegenerator.domain.model;

public class ReadmeGenerationConfig {

    public enum HeaderAlignment {
        LEFT, CENTER, RIGHT
    }

    public enum ListStyle {
        BULLET, NUMBERED
    }

    private final HeaderAlignment headerAlignment;
    private final ListStyle listStyle;

    public ReadmeGenerationConfig(HeaderAlignment headerAlignment, ListStyle listStyle) {
        this.headerAlignment = headerAlignment;
        this.listStyle = listStyle;
    }

    public HeaderAlignment getHeaderAlignment() {
        return headerAlignment;
    }

    public ListStyle getListStyle() {
        return listStyle;
    }
}
