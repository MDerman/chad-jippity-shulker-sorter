package dev.matthewderman.aishulkersorter.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.matthewderman.aishulkersorter.config.AIShulkerSorterConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

public final class ChadInitCommand {
    private ChadInitCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("chadinit")
                        .then(ClientCommandManager.argument("api_key", StringArgumentType.word())
                                .executes(context -> {
                                    String apiKey = StringArgumentType.getString(context, "api_key").trim();
                                    AIShulkerSorterConfig config = AIShulkerSorterConfig.getInstance();
                                    config.rememberApiKey = true;
                                    config.setSessionApiKey(apiKey);
                                    config.save();

                                    context.getSource().getClient().gui.getChat().getRecentChat()
                                            .removeIf(entry -> entry.startsWith("/chadinit "));
                                    context.getSource().sendFeedback(Component.translatable(
                                            "aishulkersorter.command.chadinit.saved"));
                                    return 1;
                                }))));
    }
}
