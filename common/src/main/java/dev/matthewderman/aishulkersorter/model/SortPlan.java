package dev.matthewderman.aishulkersorter.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record SortPlan(List<Category> categories, List<String> keepLoose) {
    public SortPlan {
        categories = List.copyOf(categories);
        keepLoose = List.copyOf(keepLoose);
    }

    public Set<String> allRefs() {
        Set<String> refs = new LinkedHashSet<>();
        categories.forEach(category -> refs.addAll(category.itemRefs()));
        refs.addAll(keepLoose);
        return refs;
    }

    public record Category(String id, String label, List<String> itemRefs) {
        public Category {
            id = sanitizeId(id);
            label = sanitizeLabel(label);
            itemRefs = List.copyOf(itemRefs);
        }

        private static String sanitizeId(String value) {
            String sanitized = value == null ? "category" : value.toLowerCase()
                    .replaceAll("[^a-z0-9_-]", "_").replaceAll("_+", "_");
            return sanitized.isBlank() ? "category" : sanitized.substring(0, Math.min(48, sanitized.length()));
        }

        private static String sanitizeLabel(String value) {
            String sanitized = value == null ? "Category" : value.replaceAll("[\\p{Cntrl}]", "").trim();
            return sanitized.isBlank() ? "Category" : sanitized.substring(0, Math.min(64, sanitized.length()));
        }
    }

    public List<String> duplicateRefs() {
        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (Category category : categories) {
            for (String ref : category.itemRefs()) if (!seen.add(ref)) duplicates.add(ref);
        }
        for (String ref : keepLoose) if (!seen.add(ref)) duplicates.add(ref);
        return duplicates;
    }
}
