package dev.matthewderman.aishulkersorter.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.file.Path;

/**
 * Fabric adapter for loader API calls needed by shared code.
 */
public interface Platform {

    Path getConfigDir();

    boolean isModLoaded(String modId);

    /**
     * Creates a namespaced payload type without shared code referencing version-specific id classes.
     */
    <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createPayloadType(String namespace, String path);
}
