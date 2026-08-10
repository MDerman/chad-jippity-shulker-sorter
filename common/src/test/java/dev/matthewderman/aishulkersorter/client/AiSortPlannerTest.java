package dev.matthewderman.aishulkersorter.client;

import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.model.SortPlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiSortPlannerTest {
    private static final InventorySnapshot.ItemRef LOOSE = new InventorySnapshot.ItemRef(
            "l1", "minecraft:bread", "Bread", 8, false, InventorySnapshot.Origin.LOOSE);
    private static final InventorySnapshot.ItemRef BOXED = new InventorySnapshot.ItemRef(
            "s1", "minecraft:stone", "Stone", 64, false, InventorySnapshot.Origin.SHULKER);

    @Test
    void acceptsExactCoverage() {
        SortPlan plan = new SortPlan(List.of(new SortPlan.Category("blocks", "Blocks", List.of("s1"))), List.of("l1"));
        assertDoesNotThrow(() -> AiSortPlanner.validate(plan, List.of(LOOSE, BOXED)));
    }

    @Test
    void rejectsDuplicateOrInventedRefs() {
        SortPlan duplicate = new SortPlan(List.of(new SortPlan.Category("blocks", "Blocks", List.of("s1", "s1"))), List.of("l1"));
        SortPlan invented = new SortPlan(List.of(new SortPlan.Category("blocks", "Blocks", List.of("s2"))), List.of("l1"));
        assertThrows(IOException.class, () -> AiSortPlanner.validate(duplicate, List.of(LOOSE, BOXED)));
        assertThrows(IOException.class, () -> AiSortPlanner.validate(invented, List.of(LOOSE, BOXED)));
    }

    @Test
    void rejectsKeepingBoxedItemLoose() {
        SortPlan plan = new SortPlan(List.of(new SortPlan.Category("food", "Food", List.of("l1"))), List.of("s1"));
        assertThrows(IOException.class, () -> AiSortPlanner.validate(plan, List.of(LOOSE, BOXED)));
    }
}
