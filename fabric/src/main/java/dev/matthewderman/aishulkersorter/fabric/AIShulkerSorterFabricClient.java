package dev.matthewderman.aishulkersorter.fabric;

import dev.matthewderman.aishulkersorter.AIShulkerSorterClient;
import net.fabricmc.api.ClientModInitializer;

public final class AIShulkerSorterFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AIShulkerSorterClient.init();
        ChadInitCommand.register();
    }
}
