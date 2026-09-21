package dev.matthewderman.aishulkersorter.client;

import com.google.gson.JsonObject;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.model.SortPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenRouterClientTest {
    @Test
    void requestUsesStrictChatCompletionsSchemaWithoutKey() {
        JsonObject body = OpenRouterClient.requestBody(List.of(new InventorySnapshot.ItemRef(
                "l1", "minecraft:stone", "Stone", 64, false, InventorySnapshot.Origin.LOOSE)),
                2, "Keep stone loose", "z-ai/glm-test");

        assertEquals("z-ai/glm-test", body.get("model").getAsString());
        assertEquals(2, body.getAsJsonArray("messages").size());
        assertEquals("json_schema", body.getAsJsonObject("response_format").get("type").getAsString());
        assertTrue(body.getAsJsonObject("response_format").getAsJsonObject("json_schema")
                .get("strict").getAsBoolean());
        assertTrue(body.getAsJsonObject("provider").get("require_parameters").getAsBoolean());
        assertFalse(body.toString().contains("Authorization"));
    }

    @Test
    void parsesChatCompletionPlan() throws Exception {
        String response = """
                {"choices":[{"message":{"role":"assistant","content":"{\\"categories\\":[{\\"id\\":\\"blocks\\",\\"label\\":\\"Blocks\\",\\"itemRefs\\":[\\"s1\\"]}],\\"keepLoose\\":[\\"l1\\"]}"}}]}
                """;
        SortPlan plan = OpenRouterClient.parsePlan(response);
        assertEquals(List.of("s1"), plan.categories().getFirst().itemRefs());
        assertEquals(List.of("l1"), plan.keepLoose());
    }
}
