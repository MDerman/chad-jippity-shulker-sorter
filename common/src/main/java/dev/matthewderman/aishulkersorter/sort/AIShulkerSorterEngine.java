package dev.matthewderman.aishulkersorter.sort;

import dev.matthewderman.aishulkersorter.config.CategoryDefinition;
import dev.matthewderman.aishulkersorter.config.OverflowMode;
import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.util.ShulkerBoxHelper;
import dev.matthewderman.aishulkersorter.util.ShulkerBoxHelper.ShulkerBoxInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class AIShulkerSorterEngine {

    public static SortResult sort(Inventory inventory) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();

        // Phase 1: SCAN - find sortable shulker boxes
        List<ShulkerBoxInfo> allBoxes = ShulkerBoxHelper.findShulkerBoxes(inventory);
        List<ShulkerBoxInfo> sortableBoxes = new ArrayList<>();
        int lockedCount = 0;
        for (ShulkerBoxInfo box : allBoxes) {
            if (ShulkerBoxHelper.isLocked(box.stack())) { lockedCount++; continue; }
            sortableBoxes.add(box);
        }
        // Filter out empty boxes if configured
        if (config.skipEmptyBoxes) {
            sortableBoxes.removeIf(box -> ShulkerBoxHelper.isEmpty(box.stack()));
        }
        if (sortableBoxes.isEmpty()) {
            return lockedCount > 0
                ? SortResult.error("aishulkersorter.message.error.all_locked")
                : SortResult.error("aishulkersorter.message.error.no_boxes");
        }

        int totalSlotsAvailable = sortableBoxes.size() * 27;

        // Phase 2a: CATEGORIZE box items only
        Map<String, List<ItemStack>> categorizedItems = new LinkedHashMap<>();
        for (String cat : config.categoryOrder) categorizedItems.put(cat, new ArrayList<>());

        Map<Integer, String> boxPrimaryCategory = new HashMap<>();
        int totalItemCount = 0;

        for (int boxIdx = 0; boxIdx < sortableBoxes.size(); boxIdx++) {
            List<ItemStack> contents = ShulkerBoxHelper.getContents(sortableBoxes.get(boxIdx).stack());
            Map<String, Integer> catCounts = new HashMap<>();
            for (ItemStack item : contents) {
                if (item.isEmpty()) continue;
                String cat = ItemCategorizer.categorize(item);
                categorizedItems.computeIfAbsent(cat, k -> new ArrayList<>()).add(item.copy());
                totalItemCount += item.getCount();
                catCounts.merge(cat, item.getCount(), Integer::sum);
            }
            if (!catCounts.isEmpty()) {
                boxPrimaryCategory.put(boxIdx, catCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).get().getKey());
            }
        }

        // Phase 3a: MERGE box items
        for (Map.Entry<String, List<ItemStack>> e : categorizedItems.entrySet())
            e.setValue(mergeStacks(e.getValue()));

        // Hard validate: box items alone must always fit
        int boxSlotsNeeded = categorizedItems.values().stream().mapToInt(List::size).sum();
        if (boxSlotsNeeded > totalSlotsAvailable)
            return SortResult.error("aishulkersorter.message.error.not_enough_space");

        // Phase 2b: Collect loose items filtered by overflow mode and remaining capacity
        List<Integer> looseItemSlots = new ArrayList<>();
        int looseCapacity = totalSlotsAvailable - boxSlotsNeeded;

        if (config.includeLooseItems && looseCapacity > 0) {
            totalItemCount += collectLooseItems(inventory, config, categorizedItems, looseItemSlots, looseCapacity);
        }

        if (totalItemCount == 0)
            return SortResult.error("aishulkersorter.message.error.all_empty");

        // Phase 3b: MERGE combined (loose items may merge with box items)
        for (Map.Entry<String, List<ItemStack>> e : categorizedItems.entrySet())
            e.setValue(mergeStacks(e.getValue()));

        // Phase 5: DISTRIBUTE
        List<List<ItemStack>> newBoxContents = new ArrayList<>();
        for (int i = 0; i < sortableBoxes.size(); i++)
            newBoxContents.add(new ArrayList<>(Collections.nCopies(27, ItemStack.EMPTY)));

        String[] boxCategories = new String[sortableBoxes.size()];
        Set<Integer> assignedBoxes = new HashSet<>();
        Map<String, List<ItemStack>> overflow = new LinkedHashMap<>();

        // Pass 1: fill affinity boxes (boxes that previously held that category)
        for (String cat : config.categoryOrder) {
            List<ItemStack> items = categorizedItems.get(cat);
            if (items == null || items.isEmpty()) { overflow.put(cat, new ArrayList<>()); continue; }
            int itemIdx = 0;
            for (int boxIdx = 0; boxIdx < sortableBoxes.size() && itemIdx < items.size(); boxIdx++) {
                if (assignedBoxes.contains(boxIdx) || !cat.equals(boxPrimaryCategory.get(boxIdx))) continue;
                assignedBoxes.add(boxIdx); boxCategories[boxIdx] = cat;
                int slotIdx = 0;
                while (slotIdx < 27 && itemIdx < items.size())
                    newBoxContents.get(boxIdx).set(slotIdx++, items.get(itemIdx++));
            }
            overflow.put(cat, itemIdx < items.size()
                ? new ArrayList<>(items.subList(itemIdx, items.size()))
                : new ArrayList<>());
        }

        // Pass 2: mode-dependent free-box distribution
        Set<Integer> affinityBoxes = new HashSet<>(assignedBoxes);
        boolean ok = config.overflowMode == OverflowMode.DOMINANT
            ? distributePassDominant(sortableBoxes, newBoxContents, boxCategories, assignedBoxes, overflow)
            : distributePassFill(sortableBoxes, newBoxContents, boxCategories, assignedBoxes, affinityBoxes, overflow, config);

        if (!ok) return SortResult.error("aishulkersorter.message.error.internal");

        // Phase 6: LABEL
        Map<String, Integer> categoryCounters = new LinkedHashMap<>();
        List<Component> newNames = new ArrayList<>();
        for (int i = 0; i < sortableBoxes.size(); i++) {
            String cat = boxCategories[i];
            if (cat != null && config.autoLabel) {
                CategoryDefinition catDef = config.getCategory(cat);
                String prefix = catDef != null ? catDef.getLabelPrefix() : cat;
                int count = categoryCounters.merge(cat, 1, Integer::sum);
                newNames.add(Component.translatable(prefix).append(Component.literal(" #" + count)));
            } else {
                newNames.add(null);
            }
        }

        // Phase 7: APPLY
        int boxesSorted = 0;
        for (int i = 0; i < sortableBoxes.size(); i++) {
            ShulkerBoxInfo box = sortableBoxes.get(i);
            ShulkerBoxHelper.setContents(box.stack(), newBoxContents.get(i));
            if (newNames.get(i) != null) {
                ShulkerBoxHelper.setCustomName(box.stack(), newNames.get(i));
                boxesSorted++;
            }
        }
        // Only clear the loose item slots that were actually sorted into boxes
        for (int slot : looseItemSlots) inventory.setItem(slot, ItemStack.EMPTY);

        return SortResult.success(boxesSorted, totalItemCount);
    }

    // Collects loose items from inventory into categorizedItems based on the overflow mode.
    // Returns the total item count added.
    private static int collectLooseItems(
            Inventory inventory, AIShulkerSorterConfig config,
            Map<String, List<ItemStack>> categorizedItems,
            List<Integer> selectedSlots, int capacity) {

        String ignoreTag = config.looseItemIgnoreTag;
        int totalCount = 0;

        if (config.overflowMode == OverflowMode.FILL) {
            // Add loose items in categoryOrder priority until capacity is reached
            Map<String, List<Integer>> looseByCat = new LinkedHashMap<>();
            for (int i = 0; i < 36; i++) {
                ItemStack stack = inventory.getItem(i);
                if (shouldSkip(stack, ignoreTag)) continue;
                looseByCat.computeIfAbsent(ItemCategorizer.categorize(stack), k -> new ArrayList<>()).add(i);
            }
            int added = 0;
            outer:
            for (String cat : config.categoryOrder) {
                List<Integer> slots = looseByCat.get(cat);
                if (slots == null) continue;
                for (int slot : slots) {
                    if (added >= capacity) break outer;
                    ItemStack stack = inventory.getItem(slot);
                    categorizedItems.computeIfAbsent(ItemCategorizer.categorize(stack), k -> new ArrayList<>()).add(stack.copy());
                    totalCount += stack.getCount();
                    selectedSlots.add(slot);
                    added++;
                }
            }
        } else { // DOMINANT: only sort items from the top N categories by total item count
            // Count combined (box + loose) items per category to determine planned categories
            Map<String, Integer> combinedCounts = new LinkedHashMap<>();
            for (Map.Entry<String, List<ItemStack>> e : categorizedItems.entrySet()) {
                int cnt = e.getValue().stream().mapToInt(ItemStack::getCount).sum();
                if (cnt > 0) combinedCounts.put(e.getKey(), cnt);
            }
            Map<String, List<Integer>> looseSlotsByCat = new LinkedHashMap<>();
            for (int i = 0; i < 36; i++) {
                ItemStack stack = inventory.getItem(i);
                if (shouldSkip(stack, ignoreTag)) continue;
                String cat = ItemCategorizer.categorize(stack);
                combinedCounts.merge(cat, stack.getCount(), Integer::sum);
                looseSlotsByCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(i);
            }

            // N = number of "boxes" worth of capacity available
            int N = Math.max(1, (capacity + 26) / 27);
            Set<String> plannedCats = combinedCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(N)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            int added = 0;
            for (String cat : config.categoryOrder) {
                if (!plannedCats.contains(cat)) continue;
                List<Integer> slots = looseSlotsByCat.get(cat);
                if (slots == null) continue;
                for (int slot : slots) {
                    if (added >= capacity) break;
                    ItemStack stack = inventory.getItem(slot);
                    categorizedItems.computeIfAbsent(cat, k -> new ArrayList<>()).add(stack.copy());
                    totalCount += stack.getCount();
                    selectedSlots.add(slot);
                    added++;
                }
            }
        }
        return totalCount;
    }

    private static boolean shouldSkip(ItemStack stack, String ignoreTag) {
        if (stack.isEmpty() || ShulkerBoxHelper.isShulkerBox(stack)) return true;
        if (!ignoreTag.isEmpty() && stack.has(DataComponents.CUSTOM_NAME)) {
            return stack.get(DataComponents.CUSTOM_NAME).getString().contains(ignoreTag);
        }
        return false;
    }

    // FILL: shared box+slot pointer persists across categories so scarce boxes don't cause a crash.
    // When all unassigned boxes are claimed, backfill into overflow/misc boxes only — never
    // into affinity boxes that already hold a dedicated category.
    private static boolean distributePassFill(
            List<ShulkerBoxInfo> sortableBoxes,
            List<List<ItemStack>> newBoxContents,
            String[] boxCategories,
            Set<Integer> assignedBoxes,
            Set<Integer> affinityBoxes,
            Map<String, List<ItemStack>> overflow,
            AIShulkerSorterConfig config) {

        int pass2Box = -1, pass2Slot = 0;
        List<int[]> backfillSlots = null;
        int backfillIdx = 0;

        for (String cat : config.categoryOrder) {
            List<ItemStack> remaining = overflow.get(cat);
            if (remaining == null || remaining.isEmpty()) continue;
            for (ItemStack item : remaining) {
                if (backfillSlots != null) {
                    if (backfillIdx >= backfillSlots.size()) return false;
                    int[] pos = backfillSlots.get(backfillIdx++);
                    newBoxContents.get(pos[0]).set(pos[1], item);
                    continue;
                }
                if (pass2Box == -1 || pass2Slot >= 27) {
                    pass2Box = -1;
                    for (int boxIdx = 0; boxIdx < sortableBoxes.size(); boxIdx++) {
                        if (!assignedBoxes.contains(boxIdx)) {
                            pass2Box = boxIdx; pass2Slot = 0;
                            assignedBoxes.add(boxIdx); boxCategories[boxIdx] = cat;
                            break;
                        }
                    }
                    if (pass2Box == -1) {
                        // No unassigned boxes left — only backfill into:
                        // 1) overflow boxes (assigned in Pass 2, not affinity)
                        // 2) misc boxes (catch-all, even if affinity-assigned)
                        backfillSlots = new ArrayList<>();
                        for (int b = 0; b < sortableBoxes.size(); b++) {
                            if (affinityBoxes.contains(b) && !"misc".equals(boxCategories[b])) continue;
                            List<ItemStack> content = newBoxContents.get(b);
                            for (int s = 0; s < 27; s++) {
                                if (content.get(s).isEmpty()) backfillSlots.add(new int[]{b, s});
                            }
                        }
                        if (backfillIdx >= backfillSlots.size()) return false;
                        int[] pos = backfillSlots.get(backfillIdx++);
                        newBoxContents.get(pos[0]).set(pos[1], item);
                        continue;
                    }
                }
                newBoxContents.get(pass2Box).set(pass2Slot++, item);
            }
        }
        return true;
    }

    // DOMINANT: each category claims its own box; items that don't fit a dedicated box are
    // packed into remaining slots so box-extracted items are never lost.
    private static boolean distributePassDominant(
            List<ShulkerBoxInfo> sortableBoxes,
            List<List<ItemStack>> newBoxContents,
            String[] boxCategories,
            Set<Integer> assignedBoxes,
            Map<String, List<ItemStack>> overflow) {

        List<ItemStack> spillover = new ArrayList<>();

        for (Map.Entry<String, List<ItemStack>> entry : overflow.entrySet()) {
            List<ItemStack> remaining = entry.getValue();
            if (remaining.isEmpty()) continue;
            int itemIdx = 0;
            for (int boxIdx = 0; boxIdx < sortableBoxes.size() && itemIdx < remaining.size(); boxIdx++) {
                if (assignedBoxes.contains(boxIdx)) continue;
                assignedBoxes.add(boxIdx); boxCategories[boxIdx] = entry.getKey();
                int slotIdx = 0;
                while (slotIdx < 27 && itemIdx < remaining.size())
                    newBoxContents.get(boxIdx).set(slotIdx++, remaining.get(itemIdx++));
            }
            while (itemIdx < remaining.size()) spillover.add(remaining.get(itemIdx++));
        }

        // Spillover: box items that had no dedicated box; pack into any remaining space
        for (ItemStack item : spillover) {
            boolean placed = false;
            for (int boxIdx = 0; boxIdx < sortableBoxes.size() && !placed; boxIdx++) {
                List<ItemStack> content = newBoxContents.get(boxIdx);
                for (int slotIdx = 0; slotIdx < 27 && !placed; slotIdx++) {
                    if (content.get(slotIdx).isEmpty()) { content.set(slotIdx, item); placed = true; }
                }
            }
            if (!placed) return false;
        }
        return true;
    }

    private static List<ItemStack> mergeStacks(List<ItemStack> items) {
        if (items.isEmpty()) return items;
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            boolean found = false;
            for (ItemStack existing : merged) {
                if (ItemStack.isSameItemSameComponents(existing, item)) {
                    int canAdd = existing.getMaxStackSize() - existing.getCount();
                    if (canAdd > 0) {
                        int toAdd = Math.min(canAdd, item.getCount());
                        existing.grow(toAdd);
                        item.shrink(toAdd);
                        if (item.isEmpty()) { found = true; break; }
                    }
                }
            }
            if (!found && !item.isEmpty()) merged.add(item.copy());
        }
        return merged;
    }
}
