package dev.matthewderman.aishulkersorter.keybind;

import dev.matthewderman.aishulkersorter.client.AiSortPlanner;
import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.hud.SortingHudOverlay;
import dev.matthewderman.aishulkersorter.inventory.InventorySnapshotFactory;
import dev.matthewderman.aishulkersorter.model.InventorySnapshot;
import dev.matthewderman.aishulkersorter.network.AiSortRequestPayload;
import dev.matthewderman.aishulkersorter.screen.AiSortPreviewScreen;
import dev.matthewderman.aishulkersorter.sort.AIShulkerSorterEngine;
import dev.matthewderman.aishulkersorter.sort.SortResult;
import dev.matthewderman.aishulkersorter.network.SortRequestPayload;
import dev.matthewderman.aishulkersorter.network.ServerUndoManager;
import dev.matthewderman.aishulkersorter.undo.SortUndoManager;
import dev.matthewderman.aishulkersorter.util.NotificationHelper;
import dev.matthewderman.aishulkersorter.util.ShulkerBoxHelper;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class SortKeybindHandler {
    // Vanilla category avoids version-specific identifier types across supported Minecraft versions.
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.INVENTORY;

    private static KeyMapping sortKeybind;

    public static void register() {
        sortKeybind = new KeyMapping(
                "key.aishulkersorter.sort",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        );
        KeyMappingRegistry.register(sortKeybind);

        ClientTickEvent.CLIENT_POST.register(SortKeybindHandler::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
        while (sortKeybind.consumeClick()) {
            if (client.player == null) continue;

            boolean shiftHeld = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (shiftHeld) {
                handleUndo(client);
                continue;
            }

            boolean altHeld = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_LEFT_ALT)
                    || InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
            if (!altHeld) {
                handleAiSort(client);
                continue;
            }

            MinecraftServer integratedServer = client.getSingleplayerServer();
            if (integratedServer != null) {
                sortOnServer(client, integratedServer);
            } else if (NetworkManager.canServerReceive(SortRequestPayload.TYPE)) {
                SortingHudOverlay.show();
                NetworkManager.sendToServer(new SortRequestPayload(SortRequestPayload.Action.SORT));
            } else if (client.gameMode != null && client.gameMode.getPlayerMode() == GameType.CREATIVE) {
                SortUndoManager.get().saveSnapshot(client.player.getInventory());
                sortClientAndSyncCreative(client);
            } else {
                NotificationHelper.sendError(client.player, "aishulkersorter.message.error.multiplayer_no_mod");
            }
        }
    }

    private static void handleAiSort(Minecraft client) {
        if (!NetworkManager.canServerReceive(AiSortRequestPayload.TYPE)) {
            NotificationHelper.sendError(client.player, "aishulkersorter.message.error.multiplayer_no_mod");
            return;
        }
        InventorySnapshot snapshot = InventorySnapshotFactory.create(client.player.getInventory());
        SortingHudOverlay.show();
        NotificationHelper.sendInfo(client.player, "aishulkersorter.message.ai_planning");
        new AiSortPlanner().plan(snapshot).whenComplete((plan, error) -> client.execute(() -> {
            if (error != null) {
                String message = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                NotificationHelper.sendError(client.player, message == null ? "AI sorting failed." : message);
                NotificationHelper.sendInfo(client.player, "aishulkersorter.message.ai_fallback");
                return;
            }
            if (AIShulkerSorterConfig.getInstance().preview) {
                client.setScreen(new AiSortPreviewScreen(client.screen, snapshot, plan));
            } else {
                NetworkManager.sendToServer(new AiSortRequestPayload(snapshot.hash(), plan));
            }
        }));
    }

    private static void handleUndo(Minecraft client) {
        MinecraftServer integratedServer = client.getSingleplayerServer();
        if (integratedServer != null) {
            undoOnServer(client, integratedServer);
        } else if (NetworkManager.canServerReceive(SortRequestPayload.TYPE)) {
            NetworkManager.sendToServer(new SortRequestPayload(SortRequestPayload.Action.UNDO));
        } else if (client.gameMode != null && client.gameMode.getPlayerMode() == GameType.CREATIVE) {
            undoClientAndSyncCreative(client);
        } else {
            NotificationHelper.sendInfo(client.player, "aishulkersorter.message.undo_nothing");
        }
    }

    private static void sortOnServer(Minecraft client, MinecraftServer server) {
        SortingHudOverlay.show();
        UUID playerUUID = client.player.getUUID();

        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerUUID);
            if (serverPlayer == null) return;

            ServerUndoManager.get().saveSnapshot(serverPlayer);
            SortResult result = AIShulkerSorterEngine.sort(serverPlayer.getInventory());
            serverPlayer.inventoryMenu.broadcastChanges();

            if (result.success()) {
                NotificationHelper.sendSuccess(serverPlayer, result.boxesSorted(), result.itemsMoved());
            } else {
                ServerUndoManager.get().clear(serverPlayer.getUUID());
                Minecraft.getInstance().execute(() -> SortUndoManager.get().clear());
                NotificationHelper.sendError(serverPlayer, result.errorMessage());
            }
        });
    }

    private static void sortClientAndSyncCreative(Minecraft client) {
        SortingHudOverlay.show();

        SortResult result = AIShulkerSorterEngine.sort(client.player.getInventory());

        if (result.success()) {
            MultiPlayerGameMode gameMode = client.gameMode;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = client.player.getInventory().getItem(i);
                if (ShulkerBoxHelper.isShulkerBox(stack)) {
                    int containerSlot = i < 9 ? i + 36 : i;
                    gameMode.handleCreativeModeItemAdd(stack.copy(), containerSlot);
                }
            }
            NotificationHelper.sendSuccess(client.player, result.boxesSorted(), result.itemsMoved());
        } else {
            SortUndoManager.get().clear();
            NotificationHelper.sendError(client.player, result.errorMessage());
        }
    }

    private static void undoOnServer(Minecraft client, MinecraftServer server) {
        UUID playerUUID = client.player.getUUID();
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(playerUUID);
            if (serverPlayer == null) return;
            boolean restored = ServerUndoManager.get().restore(serverPlayer);
            NotificationHelper.sendInfo(serverPlayer, restored
                    ? "aishulkersorter.message.undo_success" : "aishulkersorter.message.undo_nothing");
        });
    }

    private static void undoClientAndSyncCreative(Minecraft client) {
        List<ItemStack> snapshot = SortUndoManager.get().getSnapshot();
        SortUndoManager.get().clear();

        if (snapshot == null) {
            NotificationHelper.sendInfo(client.player, "aishulkersorter.message.undo_nothing");
            return;
        }

        for (int i = 0; i < 36; i++) {
            client.player.getInventory().setItem(i, snapshot.get(i).copy());
        }
        MultiPlayerGameMode gameMode = client.gameMode;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = snapshot.get(i);
            if (!stack.isEmpty()) {
                int containerSlot = i < 9 ? i + 36 : i;
                gameMode.handleCreativeModeItemAdd(stack.copy(), containerSlot);
            }
        }
        NotificationHelper.sendInfo(client.player, "aishulkersorter.message.undo_success");
    }
}
