package dev.matthewderman.aishulkersorter.tooltip;

import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import dev.matthewderman.aishulkersorter.util.ShulkerBoxHelper;
import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShulkerTooltipRenderer {

    public static void register() {
        ClientTooltipEvent.ITEM.register((stack, lines, context, type) -> {
            AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
            if (!config.tooltipEnabled) return;
            if (!ShulkerBoxHelper.isShulkerBox(stack)) return;

            List<ItemStack> contents = ShulkerBoxHelper.getContents(stack);

            // Aggregate items by type
            Map<String, Integer> itemCounts = new LinkedHashMap<>();
            List<String> itemNames = new ArrayList<>();

            for (ItemStack item : contents) {
                if (item.isEmpty()) continue;
                String name = item.getHoverName().getString();
                itemCounts.merge(name, item.getCount(), Integer::sum);
                if (!itemNames.contains(name)) {
                    itemNames.add(name);
                }
            }

            if (itemCounts.isEmpty()) {
                lines.add(Component.literal("  ").append(Component.translatable("aishulkersorter.tooltip.empty")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return;
            }

            lines.add(Component.literal("  ").append(Component.translatable("aishulkersorter.tooltip.contents")).withStyle(ChatFormatting.GRAY));

            int shown = 0;
            int maxLines = config.tooltipMaxLines;
            int remaining = 0;

            for (String name : itemNames) {
                if (shown >= maxLines) {
                    remaining += itemCounts.get(name);
                    continue;
                }
                int count = itemCounts.get(name);
                lines.add(Component.literal("  " + name + " x" + count)
                        .withStyle(ChatFormatting.GRAY));
                shown++;
            }

            if (remaining > 0) {
                lines.add(Component.literal("  ").append(Component.translatable("aishulkersorter.tooltip.more", remaining))
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        });
    }
}
