package dev.matthewderman.aishulkersorter.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.matthewderman.aishulkersorter.AIShulkerSorter;
import dev.matthewderman.aishulkersorter.platform.Platforms;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

public final class SemanticCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, Decision>>() {}.getType();
    private static final Path PATH = Platforms.get().getConfigDir().resolve("aishulkersorter-cache.json");
    private static Map<String, Decision> entries;

    private SemanticCache() {}

    public static synchronized Decision get(String itemId, String instructions) {
        load();
        return entries.get(key(itemId, instructions));
    }

    public static synchronized void put(String itemId, String instructions, Decision decision) {
        load();
        entries.put(key(itemId, instructions), decision);
        save();
    }

    public static synchronized void clear() {
        entries = new HashMap<>();
        try { Files.deleteIfExists(PATH); }
        catch (IOException e) { AIShulkerSorter.LOGGER.warn("Could not clear semantic cache: {}", e.getMessage()); }
    }

    private static void load() {
        if (entries != null) return;
        if (!Files.exists(PATH)) { entries = new HashMap<>(); return; }
        try {
            Map<String, Decision> loaded = GSON.fromJson(Files.readString(PATH), TYPE);
            entries = loaded == null ? new HashMap<>() : new HashMap<>(loaded);
        } catch (Exception e) {
            AIShulkerSorter.LOGGER.warn("Ignoring invalid semantic cache: {}", e.getMessage());
            entries = new HashMap<>();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(entries), StandardCharsets.UTF_8);
        } catch (IOException e) {
            AIShulkerSorter.LOGGER.warn("Could not save semantic cache: {}", e.getMessage());
        }
    }

    private static String key(String itemId, String instructions) {
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(instructions.getBytes(StandardCharsets.UTF_8)));
            return hash + ':' + itemId;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Decision(String categoryId, String label, boolean keepLoose) {}
}
