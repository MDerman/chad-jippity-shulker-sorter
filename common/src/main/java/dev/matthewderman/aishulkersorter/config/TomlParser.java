package dev.matthewderman.aishulkersorter.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TomlParser {

    public static Map<String, Object> parse(Path path) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> currentTable = root;

        for (String rawLine : Files.readAllLines(path)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Table header [section] or [section.subsection]
            if (line.startsWith("[") && line.endsWith("]")) {
                String tableName = line.substring(1, line.length() - 1).trim();
                currentTable = getOrCreateTable(root, tableName);
                continue;
            }

            // Key = Value
            int eqIndex = line.indexOf('=');
            if (eqIndex == -1) continue;

            String key = line.substring(0, eqIndex).trim();
            String value = line.substring(eqIndex + 1).trim();
            currentTable.put(key, parseValue(value));
        }

        return root;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getOrCreateTable(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (String part : parts) {
            current = (Map<String, Object>) current.computeIfAbsent(part, k -> new LinkedHashMap<>());
        }
        return current;
    }

    private static Object parseValue(String value) {
        // Boolean
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;

        // Integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        // String (quoted)
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return unescape(value.substring(1, value.length() - 1));
        }

        // Array
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseArray(value.substring(1, value.length() - 1).trim());
        }

        // Unquoted string fallback
        return value;
    }

    private static List<String> parseArray(String inner) {
        List<String> result = new ArrayList<>();
        if (inner.isEmpty()) return result;

        for (String element : inner.split(",")) {
            String trimmed = element.trim();
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            result.add(trimmed);
        }
        return result;
    }

    public static void write(Path path, Map<String, Object> data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writeTable(writer, data, "");
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeTable(BufferedWriter writer, Map<String, Object> data, String prefix) throws IOException {
        // Write simple values first
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                writer.write(entry.getKey() + " = " + formatValue(entry.getValue()));
                writer.newLine();
            }
        }

        // Then write sub-tables
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Map) {
                String tableName = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                writer.newLine();
                writer.write("[" + tableName + "]");
                writer.newLine();
                writeTable(writer, (Map<String, Object>) entry.getValue(), tableName);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + escape((String) value) + "\"";
        } else if (value instanceof List) {
            List<String> list = (List<String>) value;
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (String s : list) {
                joiner.add("\"" + escape(s) + "\"");
            }
            return joiner.toString();
        }
        return String.valueOf(value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && c == '\\') { escaped = true; continue; }
            if (escaped) {
                result.append(switch (c) { case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> c; });
                escaped = false;
            } else result.append(c);
        }
        if (escaped) result.append('\\');
        return result.toString();
    }

    // Utility getters with type safety

    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : defaultValue;
    }

    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        return val instanceof Boolean ? (Boolean) val : defaultValue;
    }

    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        return val instanceof Integer ? (Integer) val : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getStringList(Map<String, Object> map, String key, List<String> defaultValue) {
        Object val = map.get(key);
        return val instanceof List ? (List<String>) val : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getTable(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }
}
