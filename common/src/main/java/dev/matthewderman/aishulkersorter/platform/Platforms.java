package dev.matthewderman.aishulkersorter.platform;

import java.util.ServiceLoader;

public final class Platforms {
    private static final Platform INSTANCE = ServiceLoader.load(Platform.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No AIShulkerSorter Platform implementation on classpath"));

    private Platforms() {
    }

    public static Platform get() {
        return INSTANCE;
    }
}
