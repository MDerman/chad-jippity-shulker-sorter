package dev.matthewderman.aishulkersorter.model;

import java.util.List;
import java.util.Map;

public record InventorySnapshot(String hash, List<ItemRef> items, int availableShulkers) {
    public InventorySnapshot {
        items = List.copyOf(items);
    }

    public Map<String, ItemRef> byRef() {
        return items.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(ItemRef::ref, item -> item));
    }

    public record ItemRef(String ref, String id, String name, int count, boolean enchanted, Origin origin) {}

    public enum Origin { LOOSE, SHULKER }
}
