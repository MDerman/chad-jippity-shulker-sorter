package dev.matthewderman.aishulkersorter.util;

import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

public class ShulkerBoxHelper {

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static List<ItemStack> getContents(ItemStack shulkerStack) {
        if (!isShulkerBox(shulkerStack)) return List.of();

        ItemContainerContents container = shulkerStack.getOrDefault(
                DataComponents.CONTAINER, ItemContainerContents.EMPTY);

        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        container.copyInto(items);

        return new ArrayList<>(items);
    }

    public static void setContents(ItemStack shulkerStack, List<ItemStack> contents) {
        if (!isShulkerBox(shulkerStack)) return;

        // Pad to 27 slots
        NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(contents.size(), 27); i++) {
            items.set(i, contents.get(i));
        }

        ItemContainerContents container = ItemContainerContents.fromItems(items);
        shulkerStack.set(DataComponents.CONTAINER, container);
    }

    public static boolean isLocked(ItemStack shulkerStack) {
        Component name = getCustomName(shulkerStack);
        if (name == null) return false;
        String nameStr = name.getString();
        return nameStr.contains(AIShulkerSorterConfig.getInstance().lockedTag);
    }

    public static boolean isEmpty(ItemStack shulkerStack) {
        List<ItemStack> contents = getContents(shulkerStack);
        for (ItemStack item : contents) {
            if (!item.isEmpty()) return false;
        }
        return true;
    }

    public static Component getCustomName(ItemStack shulkerStack) {
        return shulkerStack.get(DataComponents.CUSTOM_NAME);
    }

    public static void setCustomName(ItemStack shulkerStack, Component name) {
        shulkerStack.set(DataComponents.CUSTOM_NAME, name);
    }

    public static List<ShulkerBoxInfo> findShulkerBoxes(Inventory inventory) {
        List<ShulkerBoxInfo> result = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (isShulkerBox(stack)) {
                result.add(new ShulkerBoxInfo(i, stack));
            }
        }
        return result;
    }

    public record ShulkerBoxInfo(int slot, ItemStack stack) {}
}
