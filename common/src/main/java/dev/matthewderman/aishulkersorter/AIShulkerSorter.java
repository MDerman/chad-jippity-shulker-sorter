package dev.matthewderman.aishulkersorter;

import dev.matthewderman.aishulkersorter.network.ServerSortHandler;
import dev.matthewderman.aishulkersorter.network.ServerUndoManager;
import dev.architectury.event.events.common.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIShulkerSorter {
    public static final String MOD_ID = "aishulkersorter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        ServerSortHandler.register();
        PlayerEvent.PLAYER_QUIT.register(player ->
                ServerUndoManager.get().clear(player.getUUID()));
        LOGGER.info("AIShulkerSorter initialized!");
    }
}
