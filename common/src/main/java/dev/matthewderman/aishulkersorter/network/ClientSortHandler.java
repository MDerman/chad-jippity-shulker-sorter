package dev.matthewderman.aishulkersorter.network;

import dev.matthewderman.aishulkersorter.util.NotificationHelper;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;

public class ClientSortHandler {

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SortResultPayload.TYPE, SortResultPayload.CODEC,
                (payload, context) -> context.queue(() -> handleResult(payload)));
    }

    private static void handleResult(SortResultPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (payload.errorKey() != null && payload.errorKey().startsWith("aishulkersorter.message.undo")) {
            NotificationHelper.sendInfo(client.player, payload.errorKey());
        } else if (payload.success()) {
            NotificationHelper.sendSuccess(client.player, payload.boxesSorted(), payload.itemsMoved());
        } else {
            NotificationHelper.sendError(client.player, payload.errorKey());
        }
    }
}
