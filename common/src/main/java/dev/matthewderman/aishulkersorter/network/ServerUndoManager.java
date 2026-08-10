package dev.matthewderman.aishulkersorter.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ServerUndoManager {
    private static final ServerUndoManager INSTANCE = new ServerUndoManager();
    private final Map<UUID, List<ItemStack>> snapshots = new HashMap<>();

    private ServerUndoManager() {}

    public static ServerUndoManager get() { return INSTANCE; }

    public void saveSnapshot(ServerPlayer player) {
        List<ItemStack> snapshot = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            snapshot.add(player.getInventory().getItem(i).copy());
        }
        snapshots.put(player.getUUID(), snapshot);
    }

    public boolean restore(ServerPlayer player) {
        List<ItemStack> snapshot = snapshots.remove(player.getUUID());
        if (snapshot == null) return false;
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, snapshot.get(i).copy());
        }
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public boolean hasSnapshot(UUID uuid) {
        return snapshots.containsKey(uuid);
    }

    public void clear(UUID uuid) {
        snapshots.remove(uuid);
    }
}
