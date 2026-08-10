package dev.matthewderman.aishulkersorter.config;

import java.util.ArrayList;
import java.util.List;

public class CategoryDefinition {
    private final String key;
    private final String labelPrefix;
    private List<String> patterns;

    public CategoryDefinition(String key, String labelPrefix, List<String> patterns) {
        this.key = key;
        this.labelPrefix = labelPrefix;
        this.patterns = new ArrayList<>(patterns);
    }

    public String getKey() {
        return key;
    }

    public String getLabelPrefix() {
        return labelPrefix;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = new ArrayList<>(patterns);
    }
}
