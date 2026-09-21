package dev.matthewderman.aishulkersorter.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.model.SortPlan;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class OpenRouterClient {
    private static final URI CHAT_COMPLETIONS_URI = URI.create("https://openrouter.ai/api/v1/chat/completions");
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient;

    public OpenRouterClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    OpenRouterClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SortPlan createPlan(List<InventorySnapshot.ItemRef> items, int availableShulkers,
                               String instructions, String model, String apiKey)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("No OpenRouter API key. Set OPENROUTER_API_KEY or add a session key in Mod Menu.");
        }
        JsonObject requestBody = requestBody(items, availableShulkers, instructions, model);
        HttpRequest request = HttpRequest.newBuilder(CHAT_COMPLETIONS_URI)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-OpenRouter-Title", "Chad Jippity Shulker Sorter")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) throw apiError(response.statusCode(), response.body());
        return parsePlan(response.body());
    }

    static JsonObject requestBody(List<InventorySnapshot.ItemRef> items, int availableShulkers,
                                  String instructions, String model) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("max_tokens", 4000);
        JsonObject reasoning = new JsonObject();
        reasoning.addProperty("effort", "low");
        root.add("reasoning", reasoning);

        JsonArray messages = new JsonArray();
        messages.add(message("system", "You organize Minecraft items. Group supplied item references into practical "
                + "shulker categories. Do not invent refs. Every ref must appear exactly once. Only refs whose "
                + "origin is LOOSE may appear in keepLoose. Prefer gameplay groupings. Respect user preferences."));
        JsonObject inventory = new JsonObject();
        inventory.addProperty("available_shulkers", availableShulkers);
        inventory.add("items", GSON.toJsonTree(items));
        messages.add(message("user", "USER PREFERENCES\n" + instructions + "\n\nINVENTORY\n" + GSON.toJson(inventory)));
        root.add("messages", messages);

        JsonObject jsonSchema = new JsonObject();
        jsonSchema.addProperty("name", "minecraft_sort_plan");
        jsonSchema.addProperty("strict", true);
        jsonSchema.add("schema", schema());
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_schema");
        responseFormat.add("json_schema", jsonSchema);
        root.add("response_format", responseFormat);

        JsonObject provider = new JsonObject();
        provider.addProperty("require_parameters", true);
        root.add("provider", provider);
        return root;
    }

    static SortPlan parsePlan(String responseBody) throws IOException {
        JsonObject response;
        try { response = JsonParser.parseString(responseBody).getAsJsonObject(); }
        catch (RuntimeException e) { throw new IOException("OpenRouter returned invalid JSON.", e); }
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) throw new IOException("OpenRouter response contained no choices.");
        for (JsonElement choiceElement : choices) {
            JsonObject message = choiceElement.getAsJsonObject().getAsJsonObject("message");
            if (message == null) continue;
            if (message.has("refusal") && !message.get("refusal").isJsonNull()) {
                throw new IOException("OpenRouter refused the sorting request.");
            }
            if (message.has("content") && message.get("content").isJsonPrimitive()) {
                try { return GSON.fromJson(message.get("content").getAsString(), SortPlan.class); }
                catch (RuntimeException e) { throw new IOException("OpenRouter returned an invalid sort plan.", e); }
            }
        }
        throw new IOException("OpenRouter response contained no sort plan.");
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static JsonObject schema() {
        JsonObject refArray = new JsonObject();
        refArray.addProperty("type", "array");
        JsonObject string = new JsonObject();
        string.addProperty("type", "string");
        refArray.add("items", string);

        JsonObject category = new JsonObject();
        category.addProperty("type", "object");
        JsonObject categoryProperties = new JsonObject();
        categoryProperties.add("id", string.deepCopy());
        categoryProperties.add("label", string.deepCopy());
        categoryProperties.add("itemRefs", refArray.deepCopy());
        category.add("properties", categoryProperties);
        category.add("required", GSON.toJsonTree(List.of("id", "label", "itemRefs")));
        category.addProperty("additionalProperties", false);

        JsonObject categories = new JsonObject();
        categories.addProperty("type", "array");
        categories.add("items", category);
        JsonObject properties = new JsonObject();
        properties.add("categories", categories);
        properties.add("keepLoose", refArray);
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", GSON.toJsonTree(List.of("categories", "keepLoose")));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private static IOException apiError(int status, String body) {
        String message = "OpenRouter request failed (HTTP " + status + ").";
        try {
            JsonObject error = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error");
            if (error != null && error.has("message")) message += " " + error.get("message").getAsString();
        } catch (RuntimeException ignored) {}
        return new IOException(message.length() > 300 ? message.substring(0, 300) : message);
    }
}
