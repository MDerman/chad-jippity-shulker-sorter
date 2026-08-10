package dev.matthewderman.aishulkersorter.client;

import com.google.gson.JsonObject;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.model.SortPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiClientTest {
    @Test
    void requestUsesStrictResponsesApiSchemaWithoutKey() {
        JsonObject body = OpenAiClient.requestBody(List.of(new InventorySnapshot.ItemRef(
                "l1", "minecraft:stone", "Stone", 64, false, InventorySnapshot.Origin.LOOSE)),
                2, "Keep stone loose", "gpt-test");

        assertEquals("gpt-test", body.get("model").getAsString());
        assertFalse(body.get("store").getAsBoolean());
        assertEquals("json_schema", body.getAsJsonObject("text").getAsJsonObject("format")
                .get("type").getAsString());
        assertTrue(body.getAsJsonObject("text").getAsJsonObject("format").get("strict").getAsBoolean());
        assertFalse(body.toString().contains("Authorization"));
    }

    @Test
    void parsesOutputTextPlan() throws Exception {
        String response = """
                {"output":[{"type":"message","content":[{"type":"output_text","text":"{\\"categories\\":[{\\"id\\":\\"blocks\\",\\"label\\":\\"Blocks\\",\\"itemRefs\\":[\\"s1\\"]}],\\"keepLoose\\":[\\"l1\\"]}"}]}]}
                """;
        SortPlan plan = OpenAiClient.parsePlan(response);
        assertEquals(List.of("s1"), plan.categories().getFirst().itemRefs());
        assertEquals(List.of("l1"), plan.keepLoose());
    }
}
