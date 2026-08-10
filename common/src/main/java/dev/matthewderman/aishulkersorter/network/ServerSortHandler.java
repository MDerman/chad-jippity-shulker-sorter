package dev.matthewderman.aishulkersorter.network;

import dev.matthewderman.aishulkersorter.sort.AIShulkerSorterEngine;
import dev.matthewderman.aishulkersorter.sort.AiSortEngine;
import dev.matthewderman.aishulkersorter.sort.SortResult;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;

public class ServerSortHandler {

    public static void register() {
        // C2S: registering the receiver here (common init, runs on both sides)
        // also registers the payload type on the client so it can be sent.
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SortRequestPayload.TYPE, SortRequestPayload.CODEC,
                (payload, context) -> {
                    ServerPlayer player = (ServerPlayer) context.getPlayer();
                    context.queue(() -> handleRequest(player, payload));
                });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, AiSortRequestPayload.TYPE, AiSortRequestPayload.CODEC,
                (payload, context) -> {
                    ServerPlayer player = (ServerPlayer) context.getPlayer();
                    context.queue(() -> handleAiSort(player, payload));
                });

        // S2C: the client registers a receiver (see ClientSortHandler); a dedicated
        // server only needs the payload type registered so it can send it.
        if (dev.architectury.platform.Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(SortResultPayload.TYPE, SortResultPayload.CODEC);
        }
    }

    private static void handleRequest(ServerPlayer player, SortRequestPayload payload) {
        switch (payload.action()) {
            case SORT -> handleSort(player);
            case UNDO -> handleUndo(player);
        }
    }

    private static void handleSort(ServerPlayer player) {
        ServerUndoManager.get().saveSnapshot(player);

        SortResult result = AIShulkerSorterEngine.sort(player.getInventory());
        player.inventoryMenu.broadcastChanges();

        if (!result.success()) {
            ServerUndoManager.get().clear(player.getUUID());
        }

        NetworkManager.sendToPlayer(player, new SortResultPayload(
                result.success(), result.boxesSorted(), result.itemsMoved(), result.errorMessage()));
    }

    private static void handleUndo(ServerPlayer player) {
        boolean restored = ServerUndoManager.get().restore(player);
        if (restored) {
            NetworkManager.sendToPlayer(player, new SortResultPayload(true, 0, 0, "aishulkersorter.message.undo_success"));
        } else {
            NetworkManager.sendToPlayer(player, new SortResultPayload(false, 0, 0, "aishulkersorter.message.undo_nothing"));
        }
    }

    private static void handleAiSort(ServerPlayer player, AiSortRequestPayload payload) {
        ServerUndoManager.get().saveSnapshot(player);
        SortResult result = AiSortEngine.sort(player.getInventory(), payload.snapshotHash(), payload.plan());
        player.inventoryMenu.broadcastChanges();
        if (!result.success()) ServerUndoManager.get().clear(player.getUUID());
        NetworkManager.sendToPlayer(player, new SortResultPayload(
                result.success(), result.boxesSorted(), result.itemsMoved(), result.errorMessage()));
    }
}
