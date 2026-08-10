package dev.matthewderman.aishulkersorter.sort;

import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.inventory.InventorySnapshotFactory;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.model.SortPlan;
import dev.matthewderman.aishulkersorter.util.ShulkerBoxHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AiSortEngine {
    private AiSortEngine() {}

    public static SortResult sort(Inventory inventory, String expectedHash, SortPlan plan) {
        InventorySnapshotFactory.ResolvedSnapshot resolved = InventorySnapshotFactory.resolve(inventory);
        InventorySnapshot snapshot = resolved.snapshot();
        if (!snapshot.hash().equals(expectedHash)) return SortResult.error("aishulkersorter.message.error.inventory_changed");
        String validationError = validate(snapshot, plan);
        if (validationError != null) return SortResult.error(validationError);

        List<ShulkerBoxHelper.ShulkerBoxInfo> boxes = sortableBoxes(inventory);
        if (boxes.isEmpty()) return SortResult.error("aishulkersorter.message.error.no_boxes");

        Map<String, List<ItemStack>> stacksByCategory = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        Set<Integer> looseSlotsToClear = new HashSet<>();
        int itemCount = 0;
        for (SortPlan.Category category : plan.categories()) {
            List<ItemStack> stacks = stacksByCategory.computeIfAbsent(category.id(), ignored -> new ArrayList<>());
            labels.putIfAbsent(category.id(), category.label());
            for (String ref : category.itemRefs()) {
                InventorySnapshotFactory.ResolvedItem item = resolved.items().get(ref);
                split(item.prototype(), item.count(), stacks);
                looseSlotsToClear.addAll(item.looseSlots());
                itemCount += item.count();
            }
        }
        int slotsNeeded = stacksByCategory.values().stream().mapToInt(List::size).sum();
        if (slotsNeeded > boxes.size() * 27) return SortResult.error("aishulkersorter.message.error.not_enough_space");

        List<List<ItemStack>> newContents = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++)
            newContents.add(new ArrayList<>(Collections.nCopies(27, ItemStack.EMPTY)));
        String[] boxCategory = new String[boxes.size()];
        int boxIndex = 0;
        int slotIndex = 0;
        for (Map.Entry<String, List<ItemStack>> entry : stacksByCategory.entrySet()) {
            for (ItemStack stack : entry.getValue()) {
                if (slotIndex == 27) { boxIndex++; slotIndex = 0; }
                if (boxCategory[boxIndex] == null) boxCategory[boxIndex] = entry.getKey();
                newContents.get(boxIndex).set(slotIndex++, stack);
            }
        }

        Map<String, Integer> labelCounters = new HashMap<>();
        int boxesSorted = 0;
        for (int i = 0; i < boxes.size(); i++) {
            ShulkerBoxHelper.ShulkerBoxInfo box = boxes.get(i);
            ShulkerBoxHelper.setContents(box.stack(), newContents.get(i));
            String category = boxCategory[i];
            if (category != null && AIShulkerSorterConfig.getInstance().autoLabel) {
                int number = labelCounters.merge(category, 1, Integer::sum);
                ShulkerBoxHelper.setCustomName(box.stack(), Component.literal(labels.get(category) + " #" + number));
            }
            if (category != null) boxesSorted++;
        }
        looseSlotsToClear.forEach(slot -> inventory.setItem(slot, ItemStack.EMPTY));
        return SortResult.success(boxesSorted, itemCount);
    }

    static String validate(InventorySnapshot snapshot, SortPlan plan) {
        if (plan == null || !plan.duplicateRefs().isEmpty()) return "aishulkersorter.message.error.invalid_plan";
        Set<String> expected = new LinkedHashSet<>(snapshot.byRef().keySet());
        if (!plan.allRefs().equals(expected)) return "aishulkersorter.message.error.invalid_plan";
        for (String ref : plan.keepLoose()) {
            if (snapshot.byRef().get(ref).origin() != InventorySnapshot.Origin.LOOSE)
                return "aishulkersorter.message.error.invalid_plan";
        }
        if (plan.categories().stream().anyMatch(category -> category.itemRefs().isEmpty()))
            return "aishulkersorter.message.error.invalid_plan";
        return null;
    }

    private static List<ShulkerBoxHelper.ShulkerBoxInfo> sortableBoxes(Inventory inventory) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
        List<ShulkerBoxHelper.ShulkerBoxInfo> boxes = new ArrayList<>();
        for (ShulkerBoxHelper.ShulkerBoxInfo box : ShulkerBoxHelper.findShulkerBoxes(inventory)) {
            if (ShulkerBoxHelper.isLocked(box.stack())) continue;
            if (config.skipEmptyBoxes && ShulkerBoxHelper.isEmpty(box.stack())) continue;
            boxes.add(box);
        }
        return boxes;
    }

    private static void split(ItemStack prototype, int count, List<ItemStack> destination) {
        int remaining = count;
        while (remaining > 0) {
            ItemStack stack = prototype.copy();
            int stackCount = Math.min(stack.getMaxStackSize(), remaining);
            stack.setCount(stackCount);
            destination.add(stack);
            remaining -= stackCount;
        }
    }
}
