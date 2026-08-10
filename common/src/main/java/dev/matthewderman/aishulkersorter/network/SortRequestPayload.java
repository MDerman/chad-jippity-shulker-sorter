package dev.matthewderman.aishulkersorter.network;

import dev.matthewderman.aishulkersorter.AIShulkerSorter;
import dev.matthewderman.aishulkersorter.platform.Platforms;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SortRequestPayload(Action action) implements CustomPacketPayload {

    public enum Action { SORT, UNDO }

    public static final Type<SortRequestPayload> TYPE =
            Platforms.get().createPayloadType(AIShulkerSorter.MOD_ID, "sort_request");

    public static final StreamCodec<FriendlyByteBuf, SortRequestPayload> CODEC =
            CustomPacketPayload.codec(SortRequestPayload::write, SortRequestPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeByte(action.ordinal());
    }

    private static SortRequestPayload read(FriendlyByteBuf buf) {
        return new SortRequestPayload(Action.values()[buf.readByte()]);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
