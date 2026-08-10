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

public final class OpenAiClient {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private static final Gson GSON = new Gson();
    private final HttpClient httpClient;

    public OpenAiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    OpenAiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SortPlan createPlan(List<InventorySnapshot.ItemRef> items, int availableShulkers,
                               String instructions, String model, String apiKey)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("No OpenAI API key. Set OPENAI_API_KEY or add a session key in Mod Menu.");
        }
        JsonObject requestBody = requestBody(items, availableShulkers, instructions, model);
        HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
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
        root.addProperty("store", false);
        root.addProperty("max_output_tokens", 4000);
        JsonObject reasoning = new JsonObject();
        reasoning.addProperty("effort", "low");
        root.add("reasoning", reasoning);

        JsonArray input = new JsonArray();
        input.add(message("system", "You organize Minecraft items. Group supplied item references into practical "
                + "shulker categories. Do not invent refs. Every ref must appear exactly once. Only refs whose "
                + "origin is LOOSE may appear in keepLoose. Prefer gameplay groupings. Respect user preferences."));
        JsonObject inventory = new JsonObject();
        inventory.addProperty("available_shulkers", availableShulkers);
        inventory.add("items", GSON.toJsonTree(items));
        input.add(message("user", "USER PREFERENCES\n" + instructions + "\n\nINVENTORY\n" + GSON.toJson(inventory)));
        root.add("input", input);

        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "minecraft_sort_plan");
        format.addProperty("strict", true);
        format.add("schema", schema());
        JsonObject text = new JsonObject();
        text.add("format", format);
        root.add("text", text);
        return root;
    }

    static SortPlan parsePlan(String responseBody) throws IOException {
        JsonObject response;
        try { response = JsonParser.parseString(responseBody).getAsJsonObject(); }
        catch (RuntimeException e) { throw new IOException("OpenAI returned invalid JSON.", e); }
        JsonArray outputItems = response.getAsJsonArray("output");
        if (outputItems == null) throw new IOException("OpenAI response contained no output.");
        for (JsonElement outputElement : outputItems) {
            JsonObject output = outputElement.getAsJsonObject();
            if (!output.has("content")) continue;
            for (JsonElement contentElement : output.getAsJsonArray("content")) {
                JsonObject content = contentElement.getAsJsonObject();
                String type = content.has("type") ? content.get("type").getAsString() : "";
                if ("refusal".equals(type)) throw new IOException("OpenAI refused the sorting request.");
                if ("output_text".equals(type) && content.has("text")) {
                    try { return GSON.fromJson(content.get("text").getAsString(), SortPlan.class); }
                    catch (RuntimeException e) { throw new IOException("OpenAI returned an invalid sort plan.", e); }
                }
            }
        }
        throw new IOException("OpenAI response contained no sort plan.");
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
        String message = "OpenAI request failed (HTTP " + status + ").";
        try {
            JsonObject error = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error");
            if (error != null && error.has("message")) message += " " + error.get("message").getAsString();
        } catch (RuntimeException ignored) {}
        return new IOException(message.length() > 300 ? message.substring(0, 300) : message);
    }
}
