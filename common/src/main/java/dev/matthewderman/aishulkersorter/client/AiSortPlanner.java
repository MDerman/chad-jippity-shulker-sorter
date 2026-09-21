package dev.matthewderman.aishulkersorter.client;

import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.model.SortPlan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class AiSortPlanner {
    private final OpenRouterClient client;

    public AiSortPlanner() { this(new OpenRouterClient()); }
    AiSortPlanner(OpenRouterClient client) { this.client = client; }

    public CompletableFuture<SortPlan> plan(InventorySnapshot snapshot) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
        return CompletableFuture.supplyAsync(() -> {
            try { return planBlocking(snapshot, config); }
            catch (Exception e) { throw new CompletionException(e); }
        });
    }

    SortPlan planBlocking(InventorySnapshot snapshot, AIShulkerSorterConfig config) throws Exception {
        if (snapshot.availableShulkers() == 0) throw new IOException("No sortable shulker boxes found.");
        if (snapshot.items().isEmpty()) throw new IOException("No sortable items found.");

        Map<String, SemanticCache.Decision> decisions = new LinkedHashMap<>();
        List<InventorySnapshot.ItemRef> unknown = new ArrayList<>();
        for (InventorySnapshot.ItemRef item : snapshot.items()) {
            SemanticCache.Decision cached = config.semanticCache
                    ? SemanticCache.get(item.id(), config.sortingInstructions) : null;
            if (cached == null || (cached.keepLoose() && item.origin() != InventorySnapshot.Origin.LOOSE)) unknown.add(item);
            else decisions.put(item.ref(), cached);
        }

        if (!unknown.isEmpty()) {
            SortPlan partial = client.createPlan(unknown, snapshot.availableShulkers(), config.sortingInstructions,
                    config.model, config.apiKey());
            validate(partial, unknown);
            Map<String, InventorySnapshot.ItemRef> unknownByRef = unknown.stream().collect(
                    java.util.stream.Collectors.toMap(InventorySnapshot.ItemRef::ref, item -> item));
            for (SortPlan.Category category : partial.categories()) {
                for (String ref : category.itemRefs()) {
                    SemanticCache.Decision decision = new SemanticCache.Decision(category.id(), category.label(), false);
                    decisions.put(ref, decision);
                    if (config.semanticCache) SemanticCache.put(unknownByRef.get(ref).id(), config.sortingInstructions, decision);
                }
            }
            for (String ref : partial.keepLoose()) {
                SemanticCache.Decision decision = new SemanticCache.Decision("", "", true);
                decisions.put(ref, decision);
                if (config.semanticCache) SemanticCache.put(unknownByRef.get(ref).id(), config.sortingInstructions, decision);
            }
        }

        Map<String, CategoryAccumulator> categories = new LinkedHashMap<>();
        List<String> keepLoose = new ArrayList<>();
        for (InventorySnapshot.ItemRef item : snapshot.items()) {
            SemanticCache.Decision decision = decisions.get(item.ref());
            if (decision == null) throw new IOException("Sort plan omitted " + item.ref() + '.');
            if (decision.keepLoose()) keepLoose.add(item.ref());
            else categories.computeIfAbsent(decision.categoryId(), ignored ->
                    new CategoryAccumulator(decision.categoryId(), decision.label())).refs.add(item.ref());
        }
        List<SortPlan.Category> resultCategories = categories.values().stream()
                .map(category -> new SortPlan.Category(category.id, category.label, category.refs)).toList();
        return new SortPlan(resultCategories, keepLoose);
    }

    static void validate(SortPlan plan, List<InventorySnapshot.ItemRef> requested) throws IOException {
        if (plan == null) throw new IOException("OpenRouter returned an empty sort plan.");
        if (!plan.duplicateRefs().isEmpty()) throw new IOException("Sort plan contains duplicate item references.");
        Set<String> expected = requested.stream().map(InventorySnapshot.ItemRef::ref)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!plan.allRefs().equals(expected)) throw new IOException("Sort plan omitted or invented item references.");
        Map<String, InventorySnapshot.ItemRef> items = requested.stream().collect(
                java.util.stream.Collectors.toMap(InventorySnapshot.ItemRef::ref, item -> item));
        for (String ref : plan.keepLoose()) {
            if (items.get(ref).origin() != InventorySnapshot.Origin.LOOSE)
                throw new IOException("Sort plan tried to move boxed items into inventory.");
        }
        if (plan.categories().stream().anyMatch(category -> category.itemRefs().isEmpty()))
            throw new IOException("Sort plan contains an empty category.");
    }

    private static final class CategoryAccumulator {
        private final String id;
        private final String label;
        private final List<String> refs = new ArrayList<>();
        private CategoryAccumulator(String id, String label) { this.id = id; this.label = label; }
    }
}
