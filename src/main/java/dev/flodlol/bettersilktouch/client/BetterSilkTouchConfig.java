package dev.flodlol.bettersilktouch.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class BetterSilkTouchConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("better-silk-touch.json");

    public static final BetterSilkTouchConfig INSTANCE = new BetterSilkTouchConfig();

    private final LinkedHashSet<String> blockIds = new LinkedHashSet<>();

    public static void load() {
        if (Files.notExists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            StoredConfig stored = GSON.fromJson(reader, StoredConfig.class);
            INSTANCE.blockIds.clear();
            if (stored != null && stored.blockIds != null) {
                for (String rawId : stored.blockIds) {
                    INSTANCE.add(rawId);
                }
            }
        } catch (IOException | JsonParseException ignored) {
            INSTANCE.blockIds.clear();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(new StoredConfig(new ArrayList<>(INSTANCE.blockIds)), writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Better Silk Touch config", e);
        }
    }

    public boolean contains(Identifier blockId) {
        return blockIds.contains(blockId.toString());
    }

    public boolean add(String rawId) {
        String normalized = normalize(rawId);
        if (normalized == null) {
            return false;
        }

        Identifier id = Identifier.tryParse(normalized);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return false;
        }

        return blockIds.add(id.toString());
    }

    public void remove(String rawId) {
        String normalized = normalize(rawId);
        if (normalized != null) {
            blockIds.remove(normalized);
        }
    }

    public List<String> getBlockIds() {
        return List.copyOf(blockIds);
    }

    public void replaceAll(List<String> newBlockIds) {
        blockIds.clear();
        for (String blockId : newBlockIds) {
            add(blockId);
        }
    }

    public static String normalize(String rawId) {
        if (rawId == null) {
            return null;
        }

        String trimmed = rawId.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }
        return trimmed;
    }

    private record StoredConfig(List<String> blockIds) {
    }
}
