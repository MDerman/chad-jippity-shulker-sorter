package dev.matthewderman.aishulkersorter.fabric;

import dev.matthewderman.aishulkersorter.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;

public final class FabricPlatform implements Platform {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createPayloadType(String namespace, String path) {
        // Direct reference is safe on Fabric: the class is remapped to its
        // version-stable intermediary name at build time.
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(namespace, path));
    }
}
