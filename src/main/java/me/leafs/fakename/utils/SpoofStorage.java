package me.leafs.fakename.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.leafs.fakename.FakeName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SpoofStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final Map<String, String> TARGETS = new LinkedHashMap<>();

    private static Path configPath;
    private static String pendingTarget;

    private SpoofStorage() {
    }

    public static synchronized void init(Path configDir) {
        configPath = configDir.resolve("name-spoofer.json");
        load();
    }

    public static synchronized Map<String, String> getTargets() {
        return Collections.unmodifiableMap(TARGETS);
    }

    public static synchronized void setTarget(String target, String replacement) {
        TARGETS.put(target, replacement);
        save();
    }

    public static synchronized void clearTargets() {
        TARGETS.clear();
        pendingTarget = null;
        save();
    }

    public static synchronized void setPendingTarget(String target) {
        pendingTarget = target;
        save();
    }

    public static synchronized Optional<String> getPendingTarget() {
        return Optional.ofNullable(pendingTarget);
    }

    private static void load() {
        TARGETS.clear();
        pendingTarget = null;

        if (configPath == null) {
            return;
        }

        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Map<String, String> data = GSON.fromJson(reader, MAP_TYPE);
            if (data != null) {
                TARGETS.putAll(data);
            }
        } catch (IOException e) {
            FakeName.LOGGER.error("Failed to read Name Spoofer config", e);
        }
    }

    private static void save() {
        if (configPath == null) {
            return;
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(TARGETS, MAP_TYPE, writer);
            }
        } catch (IOException e) {
            FakeName.LOGGER.error("Failed to save Name Spoofer config", e);
        }
    }
}
