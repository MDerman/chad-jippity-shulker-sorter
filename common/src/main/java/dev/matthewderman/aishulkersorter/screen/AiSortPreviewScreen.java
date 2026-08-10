package dev.matthewderman.aishulkersorter.screen;

import dev.architectury.networking.NetworkManager;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.model.SortPlan;
import dev.matthewderman.aishulkersorter.network.AiSortRequestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public final class AiSortPreviewScreen extends Screen {
    private final Screen parent;
    private final InventorySnapshot snapshot;
    private final SortPlan plan;
    private final List<Line> lines = new ArrayList<>();
    private int page;
    private int pageSize;
    private Button previous;
    private Button next;

    public AiSortPreviewScreen(Screen parent, InventorySnapshot snapshot, SortPlan plan) {
        super(Component.translatable("aishulkersorter.preview.title"));
        this.parent = parent;
        this.snapshot = snapshot;
        this.plan = plan;
        buildLines();
    }

    @Override
    protected void init() {
        int y = height - 32;
        addRenderableWidget(Button.builder(Component.translatable("aishulkersorter.preview.sort"), button -> {
            NetworkManager.sendToServer(new AiSortRequestPayload(snapshot.hash(), plan));
            onClose();
        }).bounds(width / 2 - 104, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 + 4, y, 100, 20).build());
        pageSize = Math.max(1, (height - 92) / 10);
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            updatePageButtons();
        }).bounds(16, y, 20, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = Math.min(pageCount() - 1, page + 1);
            updatePageButtons();
        }).bounds(width - 36, y, 20, 20).build());
        updatePageButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        int x = Math.max(16, width / 2 - 210);
        int y = 36;
        int start = page * pageSize;
        int end = Math.min(lines.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            Line line = lines.get(i);
            graphics.drawString(font, line.text, x, y, line.color);
            y += 10;
        }
        graphics.drawCenteredString(font, (page + 1) + " / " + pageCount(), width / 2, height - 25, 0x909090);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    private void buildLines() {
        Map<String, InventorySnapshot.ItemRef> items = snapshot.byRef();
        if (!plan.keepLoose().isEmpty()) {
            lines.add(new Line(Component.translatable("aishulkersorter.preview.inventory"), 0x70E090));
            for (String ref : plan.keepLoose()) lines.add(new Line(Component.literal("  " + items.get(ref).name()), 0xD0D0D0));
        }
        for (SortPlan.Category category : plan.categories()) {
            lines.add(new Line(Component.literal(category.label()), 0x70B8F0));
            StringBuilder current = new StringBuilder("  ");
            for (String ref : category.itemRefs()) {
                String name = items.get(ref).name();
                if (current.length() + name.length() > 62) {
                    lines.add(new Line(Component.literal(current.toString()), 0xD0D0D0));
                    current = new StringBuilder("  ");
                }
                if (current.length() > 2) current.append(", ");
                current.append(name);
            }
            if (current.length() > 2) lines.add(new Line(Component.literal(current.toString()), 0xD0D0D0));
        }
    }

    private int pageCount() { return Math.max(1, (lines.size() + pageSize - 1) / pageSize); }

    private void updatePageButtons() {
        previous.active = page > 0;
        next.active = page + 1 < pageCount();
    }

    private record Line(Component text, int color) {}
}
