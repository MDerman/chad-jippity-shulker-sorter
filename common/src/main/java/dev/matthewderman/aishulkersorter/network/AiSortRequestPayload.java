package dev.matthewderman.aishulkersorter.network;

import com.google.gson.Gson;
import dev.matthewderman.aishulkersorter.AIShulkerSorter;
import dev.matthewderman.aishulkersorter.model.SortPlan;
import dev.matthewderman.aishulkersorter.platform.Platforms;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record AiSortRequestPayload(String snapshotHash, SortPlan plan) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();
    private static final int MAX_JSON_LENGTH = 262_144;
    public static final Type<AiSortRequestPayload> TYPE =
            Platforms.get().createPayloadType(AIShulkerSorter.MOD_ID, "ai_sort_request");
    public static final StreamCodec<FriendlyByteBuf, AiSortRequestPayload> CODEC =
            CustomPacketPayload.codec(AiSortRequestPayload::write, AiSortRequestPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(snapshotHash, 64);
        buf.writeUtf(GSON.toJson(plan), MAX_JSON_LENGTH);
    }

    private static AiSortRequestPayload read(FriendlyByteBuf buf) {
        String hash = buf.readUtf(64);
        try {
            return new AiSortRequestPayload(hash, GSON.fromJson(buf.readUtf(MAX_JSON_LENGTH), SortPlan.class));
        } catch (RuntimeException e) {
            return new AiSortRequestPayload(hash, new SortPlan(List.of(), List.of()));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
