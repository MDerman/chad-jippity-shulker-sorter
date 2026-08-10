package dev.matthewderman.aishulkersorter;

import dev.matthewderman.aishulkersorter.hud.SortingHudOverlay;
import dev.matthewderman.aishulkersorter.keybind.SortKeybindHandler;
import dev.matthewderman.aishulkersorter.tooltip.ShulkerTooltipRenderer;
import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.network.ClientSortHandler;
import dev.matthewderman.aishulkersorter.undo.SortUndoManager;
import dev.architectury.event.events.client.ClientPlayerEvent;

public class AIShulkerSorterClient {

    public static void init() {
        AIShulkerSorterConfig.getInstance();
        SortKeybindHandler.register();
        ShulkerTooltipRenderer.register();
        SortingHudOverlay.register();
        ClientSortHandler.register();
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> SortUndoManager.get().clear());

        AIShulkerSorter.LOGGER.info("AIShulkerSorter client initialized!");
    }
}
