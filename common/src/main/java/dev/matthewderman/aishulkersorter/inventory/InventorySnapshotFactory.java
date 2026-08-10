package dev.matthewderman.aishulkersorter.inventory;

import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.util.ShulkerBoxHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InventorySnapshotFactory {
    private InventorySnapshotFactory() {}

    public static InventorySnapshot create(Inventory inventory) {
        return resolve(inventory).snapshot();
    }

    public static ResolvedSnapshot resolve(Inventory inventory) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
        List<Aggregate> aggregates = new ArrayList<>();
        StringBuilder canonical = new StringBuilder();
        int availableShulkers = 0;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            canonical.append(slot).append(':').append(canonicalStack(stack)).append(';');
            if (ShulkerBoxHelper.isShulkerBox(stack)) {
                List<ItemStack> contents = ShulkerBoxHelper.getContents(stack);
                for (int boxSlot = 0; boxSlot < contents.size(); boxSlot++) {
                    canonical.append(slot).append('/').append(boxSlot).append(':')
                            .append(canonicalStack(contents.get(boxSlot))).append(';');
                }
                if (ShulkerBoxHelper.isLocked(stack) || (config.skipEmptyBoxes && ShulkerBoxHelper.isEmpty(stack))) continue;
                availableShulkers++;
                for (ItemStack item : contents) add(aggregates, item, InventorySnapshot.Origin.SHULKER, -1);
            } else if (config.includeLooseItems && !shouldKeepByTag(stack, config.looseItemIgnoreTag)) {
                add(aggregates, stack, InventorySnapshot.Origin.LOOSE, slot);
            }
        }

        Map<InventorySnapshot.Origin, Integer> counters = new LinkedHashMap<>();
        List<InventorySnapshot.ItemRef> refs = new ArrayList<>();
        Map<String, ResolvedItem> resolved = new LinkedHashMap<>();
        for (Aggregate aggregate : aggregates) {
            String prefix = aggregate.origin == InventorySnapshot.Origin.LOOSE ? "l" : "s";
            int number = counters.merge(aggregate.origin, 1, Integer::sum);
            String ref = prefix + number;
            refs.add(new InventorySnapshot.ItemRef(ref, itemId(aggregate.stack),
                    aggregate.stack.getHoverName().getString(), aggregate.count,
                    aggregate.stack.has(DataComponents.ENCHANTMENTS), aggregate.origin));
            resolved.put(ref, new ResolvedItem(aggregate.stack.copy(), aggregate.count,
                    aggregate.origin, List.copyOf(aggregate.looseSlots)));
        }
        InventorySnapshot snapshot = new InventorySnapshot(sha256(canonical.toString()), refs, availableShulkers);
        return new ResolvedSnapshot(snapshot, Map.copyOf(resolved));
    }

    public static String canonicalStack(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        return itemId(stack) + '|' + stack.getCount() + '|' + stack.getComponentsPatch();
    }

    private static void add(List<Aggregate> aggregates, ItemStack stack, InventorySnapshot.Origin origin, int looseSlot) {
        if (stack.isEmpty() || ShulkerBoxHelper.isShulkerBox(stack)) return;
        for (Aggregate aggregate : aggregates) {
            if (aggregate.origin == origin && ItemStack.isSameItemSameComponents(aggregate.stack, stack)) {
                aggregate.count += stack.getCount();
                if (looseSlot >= 0) aggregate.looseSlots.add(looseSlot);
                return;
            }
        }
        Aggregate aggregate = new Aggregate(stack.copy(), stack.getCount(), origin);
        if (looseSlot >= 0) aggregate.looseSlots.add(looseSlot);
        aggregates.add(aggregate);
    }

    private static boolean shouldKeepByTag(ItemStack stack, String tag) {
        if (stack.isEmpty() || ShulkerBoxHelper.isShulkerBox(stack)) return true;
        return !tag.isEmpty() && stack.has(DataComponents.CUSTOM_NAME)
                && stack.get(DataComponents.CUSTOM_NAME).getString().contains(tag);
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static final class Aggregate {
        private final ItemStack stack;
        private int count;
        private final InventorySnapshot.Origin origin;
        private final List<Integer> looseSlots = new ArrayList<>();

        private Aggregate(ItemStack stack, int count, InventorySnapshot.Origin origin) {
            this.stack = stack;
            this.count = count;
            this.origin = origin;
        }
    }

    public record ResolvedSnapshot(InventorySnapshot snapshot, Map<String, ResolvedItem> items) {}
    public record ResolvedItem(ItemStack prototype, int count, InventorySnapshot.Origin origin,
                               List<Integer> looseSlots) {}
}
