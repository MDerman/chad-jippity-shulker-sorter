package dev.matthewderman.aishulkersorter.screen;

import dev.matthewderman.aishulkersorter.client.OpenRouterClient;
import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ApiKeyScreen extends Screen {
    private final Screen parent;
    private EditBox keyField;
    private Component status = Component.empty();

    public ApiKeyScreen(Screen parent) {
        super(Component.translatable("config.aishulkersorter.api_key.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
        keyField = new EditBox(font, width / 2 - 140, height / 2 - 30, 280, 20,
                Component.translatable("config.aishulkersorter.api_key"));
        keyField.setMaxLength(256);
        keyField.setHint(Component.translatable(config.hasEnvironmentApiKey()
                ? "config.aishulkersorter.api_key.environment" : "config.aishulkersorter.api_key.hint"));
        keyField.addFormatter((value, offset) -> FormattedCharSequence.forward("*".repeat(value.length()), Style.EMPTY));
        addRenderableWidget(keyField);
        addRenderableWidget(Button.builder(Component.translatable("config.aishulkersorter.api_key.test"),
                button -> testApi()).bounds(width / 2 - 140, height / 2 + 2, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(width / 2 - 44, height / 2 + 2, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 + 52, height / 2 + 2, 88, 20).build());
    }

    private void save() {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
        if (!keyField.getValue().isBlank()) config.setSessionApiKey(keyField.getValue());
        config.save();
        onClose();
    }

    private void testApi() {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
        String candidate = keyField.getValue().isBlank() ? config.apiKey() : keyField.getValue().trim();
        status = Component.translatable("config.aishulkersorter.api_key.testing");
        CompletableFuture.runAsync(() -> {
            try {
                new OpenRouterClient().createPlan(List.of(new InventorySnapshot.ItemRef("l1", "minecraft:stone",
                        "Stone", 1, false, InventorySnapshot.Origin.LOOSE)), 1,
                        "Keep the test item loose.", config.model, candidate);
                minecraft.execute(() -> status = Component.translatable("config.aishulkersorter.api_key.success"));
            } catch (Exception e) {
                minecraft.execute(() -> status = Component.literal(e.getMessage() == null ? "API test failed." : e.getMessage()));
            }
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 62, 0xFFFFFF);
        graphics.drawCenteredString(font, status, width / 2, height / 2 + 32, 0xB0B0B0);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
