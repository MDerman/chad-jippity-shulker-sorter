package dev.matthewderman.aishulkersorter.util;

import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class NotificationHelper {

    public static void sendSuccess(Player player, int boxCount, int itemCount) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();

        if (config.enableChatNotifications) {
            Component message = Component.translatable("aishulkersorter.message.success", boxCount, itemCount)
                    .withStyle(ChatFormatting.GREEN);
            player.displayClientMessage(message, false);
        }

        if (config.enableSoundEffects) {
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS,
                    0.5f,
                    1.0f
            );
        }
    }

    public static void sendInfo(Player player, String messageKey) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
        if (config.enableChatNotifications) {
            Component text = Component.translatable(messageKey)
                    .withStyle(ChatFormatting.YELLOW);
            player.displayClientMessage(text, false);
        }
    }

    public static void sendError(Player player, String message) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();

        if (config.enableChatNotifications) {
            Component text = Component.translatable(message)
                    .withStyle(ChatFormatting.RED);
            player.displayClientMessage(text, false);
        }
    }
}
