package dev.matthewderman.aishulkersorter.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.matthewderman.aishulkersorter.client.SemanticCache;
import dev.matthewderman.aishulkersorter.screen.ApiKeyScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen {

    public static Screen create(Screen parent) {
        AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();

        // Build the categories tab dynamically
        ConfigCategory.Builder categoriesTab = ConfigCategory.createBuilder()
                .name(Component.translatable("config.aishulkersorter.category.categories"));

        // Category order list
        ListOption<String> categoryOrderList = ListOption.<String>createBuilder()
                .name(Component.translatable("config.aishulkersorter.category_order"))
                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.category_order.tooltip")))
                .binding(
                        new ArrayList<>(List.of(
                                "redstone", "transport", "nature", "mob_loot", "decoration",
                                "blocks", "tools", "food", "ores", "brewing", "misc"
                        )),
                        () -> new ArrayList<>(config.categoryOrder),
                        val -> config.categoryOrder = new ArrayList<>(val)
                )
                .controller(StringControllerBuilder::create)
                .initial("")
                .collapsed(true)
                .build();
        categoriesTab.group(categoryOrderList);

        // Per-category groups
        for (String catKey : config.categoryOrder) {
            CategoryDefinition catDef = config.categories.get(catKey);
            if (catDef == null) continue;

            // Group with enabled checkbox
            OptionGroup enabledGroup = OptionGroup.createBuilder()
                    .name(Component.translatable(catDef.getLabelPrefix()))
                    .collapsed(false)
                    .option(Option.<Boolean>createBuilder()
                            .name(Component.translatable("config.aishulkersorter.category.enabled"))
                            .description(OptionDescription.of(Component.translatable("config.aishulkersorter.category.enabled.tooltip")))
                            .binding(
                                    true,
                                    () -> !config.disabledCategories.contains(catKey),
                                    val -> {
                                        if (val) {
                                            config.disabledCategories.remove(catKey);
                                        } else {
                                            config.disabledCategories.add(catKey);
                                        }
                                    }
                            )
                            .controller(TickBoxControllerBuilder::create)
                            .build())
                    .build();
            categoriesTab.group(enabledGroup);

            // Patterns list for this category
            final CategoryDefinition finalCatDef = catDef;
            ListOption<String> patternsList = ListOption.<String>createBuilder()
                    .name(Component.translatable("config.aishulkersorter.category.patterns"))
                    .description(OptionDescription.of(Component.translatable("config.aishulkersorter.category.patterns.tooltip")))
                    .binding(
                            new ArrayList<>(finalCatDef.getPatterns()),
                            () -> new ArrayList<>(finalCatDef.getPatterns()),
                            val -> finalCatDef.setPatterns(val)
                    )
                    .controller(StringControllerBuilder::create)
                    .initial("")
                    .collapsed(true)
                    .build();
            categoriesTab.group(patternsList);
        }

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.aishulkersorter.title"))

                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.aishulkersorter.category.ai"))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.model"))
                                .binding("z-ai/glm-5.3", () -> config.model, val -> config.model = val.trim())
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.instructions"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.instructions.tooltip")))
                                .binding("", () -> config.sortingInstructions,
                                        val -> config.sortingInstructions = val.replace("\\n", "\n"))
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable("config.aishulkersorter.api_key"))
                                .text(Component.translatable("config.aishulkersorter.api_key.open"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.api_key.tooltip")))
                                .action(screen -> Minecraft.getInstance().setScreen(new ApiKeyScreen(screen)))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.remember_api_key"))
                                .binding(false, () -> config.rememberApiKey, val -> config.rememberApiKey = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.preview"))
                                .binding(true, () -> config.preview, val -> config.preview = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.semantic_cache"))
                                .binding(true, () -> config.semanticCache, val -> config.semanticCache = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Component.translatable("config.aishulkersorter.clear_cache"))
                                .text(Component.translatable("config.aishulkersorter.clear_cache.action"))
                                .action(screen -> SemanticCache.clear())
                                .build())
                        .build())

                // === Sorting Settings ===
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.aishulkersorter.category.sorting"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.auto_label"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.auto_label.tooltip")))
                                .binding(true, () -> config.autoLabel, val -> config.autoLabel = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.include_loose_items"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.include_loose_items.tooltip")))
                                .binding(true, () -> config.includeLooseItems, val -> config.includeLooseItems = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.skip_empty_boxes"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.skip_empty_boxes.tooltip")))
                                .binding(false, () -> config.skipEmptyBoxes, val -> config.skipEmptyBoxes = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<OverflowMode>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.overflow_mode"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.overflow_mode.tooltip")))
                                .binding(OverflowMode.FILL, () -> config.overflowMode, val -> config.overflowMode = val)
                                .controller(opt -> CyclingListControllerBuilder.create(opt)
                                        .values(OverflowMode.values())
                                        .valueFormatter(mode -> Component.translatable(
                                                "config.aishulkersorter.overflow_mode." + mode.name().toLowerCase())))
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.loose_item_ignore_tag"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.loose_item_ignore_tag.tooltip")))
                                .binding("[KEEP]", () -> config.looseItemIgnoreTag, val -> config.looseItemIgnoreTag = val)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.locked_tag"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.locked_tag.tooltip")))
                                .binding("[LOCKED]", () -> config.lockedTag, val -> config.lockedTag = val)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())

                // === Tooltip Settings ===
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.aishulkersorter.category.tooltip"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.tooltip_enabled"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.tooltip_enabled.tooltip")))
                                .binding(true, () -> config.tooltipEnabled, val -> config.tooltipEnabled = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.tooltip_max_lines"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.tooltip_max_lines.tooltip")))
                                .binding(5, () -> config.tooltipMaxLines, val -> config.tooltipMaxLines = val)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 27).step(1))
                                .build())
                        .build())

                // === Feedback Settings ===
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.aishulkersorter.category.feedback"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.enable_chat_notifications"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.enable_chat_notifications.tooltip")))
                                .binding(true, () -> config.enableChatNotifications, val -> config.enableChatNotifications = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.enable_sound_effects"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.enable_sound_effects.tooltip")))
                                .binding(true, () -> config.enableSoundEffects, val -> config.enableSoundEffects = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.aishulkersorter.enable_hud_overlay"))
                                .description(OptionDescription.of(Component.translatable("config.aishulkersorter.enable_hud_overlay.tooltip")))
                                .binding(true, () -> config.enableHudOverlay, val -> config.enableHudOverlay = val)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())

                // === Categories ===
                .category(categoriesTab.build())

                .save(config::save)
                .build()
                .generateScreen(parent);
    }
}
