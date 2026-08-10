package dev.matthewderman.aishulkersorter.fabric;

import dev.matthewderman.aishulkersorter.AIShulkerSorter;
import net.fabricmc.api.ModInitializer;

public final class AIShulkerSorterFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AIShulkerSorter.init();
    }
}
