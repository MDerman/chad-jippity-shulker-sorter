package dev.matthewderman.aishulkersorter.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.matthewderman.aishulkersorter.config.ConfigScreen;
import dev.matthewderman.aishulkersorter.platform.Platforms;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Platforms.get().isModLoaded("yet_another_config_lib_v3")) {
            return ConfigScreen::create;
        }
        return parent -> null;
    }
}
