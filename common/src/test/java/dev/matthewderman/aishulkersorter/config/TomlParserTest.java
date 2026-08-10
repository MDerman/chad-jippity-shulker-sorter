package dev.matthewderman.aishulkersorter.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TomlParserTest {
    @Test
    void roundTripsEscapedInstructions(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("config.toml");
        String instructions = "Keep tools loose.\nGroup \\\"rare\\\" blocks.";
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sorting_instructions", instructions);
        TomlParser.write(file, values);
        assertEquals(instructions, TomlParser.getString(TomlParser.parse(file), "sorting_instructions", ""));
    }
}
