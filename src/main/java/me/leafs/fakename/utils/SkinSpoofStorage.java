package me.leafs.fakename.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import me.leafs.fakename.FakeName;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SkinSpoofStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, SkinEntry>>() { }
            .getType();
    private static final String CONFIG_FILE_NAME = "name-spoofer-skins.json";

    private static final Map<String, SkinEntry> ENTRIES = new LinkedHashMap<>();

    private static Path configPath;

    private SkinSpoofStorage() {
    }

    public static void init(Path configDir) {
        List<SkinEntry> loadedEntries;
        synchronized (SkinSpoofStorage.class) {
            configPath = configDir.resolve(CONFIG_FILE_NAME);
            loadedEntries = loadLocked();
        }

        for (SkinEntry entry : loadedEntries) {
            entry.ensureReady();
        }
    }

    public static Optional<SkinTextures> getTextures(GameProfile profile) {
        if (profile == null) {
            return Optional.empty();
        }

        SkinEntry entry;
        synchronized (SkinSpoofStorage.class) {
            entry = findEntryLocked(buildKeys(profile));
        }

        if (entry == null) {
            return Optional.empty();
        }

        entry.ensureReady();
        return Optional.ofNullable(entry.toSkinTextures());
    }

    public static boolean setOverride(GameProfile targetProfile, GameProfile sourceProfile, SkinTextures textures) {
        if (targetProfile == null || textures == null) {
            return false;
        }

        Set<String> keys = buildKeys(targetProfile);
        if (keys.isEmpty()) {
            return false;
        }

        SkinEntry entry = new SkinEntry();
        entry.setKeys(keys);
        entry.update(textures, sourceProfile);

        synchronized (SkinSpoofStorage.class) {
            SkinEntry existing = findEntryLocked(keys);
            if (existing != null) {
                removeEntryLocked(existing);
            }

            registerEntryLocked(entry);
            saveLocked();
        }

        entry.ensureReady();
        return true;
    }

    public static boolean remove(GameProfile profile) {
        if (profile == null) {
            return false;
        }

        return removeByKeys(buildKeys(profile));
    }

    public static boolean remove(String rawKey) {
        String normalized = normalizeKey(rawKey);
        if (normalized == null) {
            return false;
        }

        Set<String> keys = new LinkedHashSet<>();
        keys.add(normalized);
        return removeByKeys(keys);
    }

    public static void clear() {
        synchronized (SkinSpoofStorage.class) {
            ENTRIES.clear();
            saveLocked();
        }
    }

    private static boolean removeByKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return false;
        }

        synchronized (SkinSpoofStorage.class) {
            SkinEntry entry = findEntryLocked(keys);
            if (entry == null) {
                return false;
            }

            boolean removed = removeEntryLocked(entry);
            if (removed) {
                saveLocked();
            }

            return removed;
        }
    }

    private static Set<String> buildKeys(GameProfile profile) {
        Set<String> keys = new LinkedHashSet<>();
        UUID uuid = profile.getId();
        if (uuid != null) {
            keys.add(uuid.toString());
        }

        String name = profile.getName();
        if (name != null && !name.isEmpty()) {
            String normalizedName = normalizeKey(name);
            if (normalizedName != null) {
                keys.add(normalizedName);
            }
        }

        return keys;
    }

    private static SkinEntry findEntryLocked(Collection<String> keys) {
        if (keys == null) {
            return null;
        }

        for (String key : keys) {
            if (key == null) {
                continue;
            }

            SkinEntry entry = ENTRIES.get(key);
            if (entry != null) {
                return entry;
            }
        }

        return null;
    }

    private static void registerEntryLocked(SkinEntry entry) {
        for (String key : entry.getAllKeys()) {
            ENTRIES.put(key, entry);
        }
    }

    private static boolean removeEntryLocked(SkinEntry entry) {
        if (entry == null) {
            return false;
        }

        boolean removed = false;
        for (String key : entry.getAllKeys()) {
            SkinEntry current = ENTRIES.get(key);
            if (current == entry) {
                ENTRIES.remove(key);
                removed = true;
            }
        }

        return removed;
    }

    private static List<SkinEntry> loadLocked() {
        ENTRIES.clear();

        List<SkinEntry> loaded = new ArrayList<>();
        if (configPath == null) {
            return loaded;
        }

        if (!Files.exists(configPath)) {
            saveLocked();
            return loaded;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Map<String, SkinEntry> data;
            try {
                data = GSON.fromJson(reader, MAP_TYPE);
            } catch (RuntimeException parseError) {
                FakeName.LOGGER.error("Malformed Name Spoofer skin config at path: {}", configPath, parseError);
                return loaded;
            }
            if (data == null) {
                return loaded;
            }

            for (Map.Entry<String, SkinEntry> entry : data.entrySet()) {
                String primaryKey = normalizeKey(entry.getKey());
                SkinEntry skinEntry = entry.getValue();
                if (primaryKey == null || skinEntry == null) {
                    continue;
                }

                skinEntry.initialize(primaryKey);
                registerEntryLocked(skinEntry);
                loaded.add(skinEntry);
            }
        } catch (IOException e) {
            FakeName.LOGGER.error("Failed to read Name Spoofer skin config at path: {}", configPath, e);
        }

        return loaded;
    }

    private static void saveLocked() {
        if (configPath == null) {
            return;
        }

        Map<String, SkinEntry> unique = new LinkedHashMap<>();
        for (SkinEntry entry : ENTRIES.values()) {
            if (entry == null) {
                continue;
            }

            String primaryKey = entry.getPrimaryKey();
            if (primaryKey == null || unique.containsKey(primaryKey)) {
                continue;
            }

            unique.put(primaryKey, entry);
        }

        try {
            Files.createDirectories(configPath.getParent());
            Path tmp = Files.createTempFile(configPath.getParent(), "name-spoofer-skins", ".json.tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(unique, MAP_TYPE, writer);
            }

            Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            FakeName.LOGGER.error("Failed to save Name Spoofer skin config to path: {}", configPath, e);
        }
    }

    private static String normalizeKey(String rawKey) {
        if (rawKey == null) {
            return null;
        }

        String trimmed = rawKey.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(trimmed).toString();
        } catch (IllegalArgumentException ignored) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
    }

    private static void requestRefresh(SkinEntry entry) {
        if (entry == null || !entry.markRefreshing()) {
            return;
        }

        GameProfile sourceProfile = entry.createSourceProfile();
        if (sourceProfile == null) {
            entry.finishRefreshing();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            entry.finishRefreshing();
            return;
        }

        PlayerSkinProvider provider = client.getSkinProvider();
        provider.fetchSkinTextures(sourceProfile).thenAccept(textures -> {
            if (textures == null) {
                entry.finishRefreshing();
                return;
            }

            client.execute(() -> {
                synchronized (SkinSpoofStorage.class) {
                    entry.update(textures, sourceProfile);
                }

                CompletableFuture.runAsync(() -> {
                    synchronized (SkinSpoofStorage.class) {
                        saveLocked();
                    }
                }, Util.getIoWorkerExecutor());

                entry.finishRefreshing();
            });
        }).exceptionally(error -> {
            entry.finishRefreshing();
            FakeName.LOGGER.error("Failed to refresh spoofed skin for {}", entry.getPrimaryKey(), error);
            return null;
        });
    }

    public static final class SkinEntry {
        private String primaryKey;
        private List<String> aliases = new ArrayList<>();

        private String texture;
        private String textureUrl;
        private String capeTexture;
        private String elytraTexture;
        private String model;
        private boolean secure;

        private String sourceName;
        private String sourceUuid;

        private transient SkinTextures cached;
        private transient boolean refreshing;

        public SkinEntry() {
        }

        void initialize(String primaryKey) {
            this.primaryKey = primaryKey;
            if (this.aliases == null) {
                this.aliases = new ArrayList<>();
            }

            List<String> sanitized = new ArrayList<>();
            for (String alias : this.aliases) {
                String normalized = normalizeKey(alias);
                if (normalized != null && !normalized.equals(primaryKey) && !sanitized.contains(normalized)) {
                    sanitized.add(normalized);
                }
            }

            this.aliases = sanitized;
        }

        void setKeys(Collection<String> keys) {
            List<String> sanitized = new ArrayList<>();
            for (String key : keys) {
                String normalized = normalizeKey(key);
                if (normalized != null && !sanitized.contains(normalized)) {
                    sanitized.add(normalized);
                }
            }

            if (sanitized.isEmpty()) {
                return;
            }

            this.primaryKey = sanitized.get(0);
            this.aliases = sanitized.size() > 1 ? new ArrayList<>(sanitized.subList(1, sanitized.size())) : new ArrayList<>();
        }

        String getPrimaryKey() {
            return primaryKey;
        }

        List<String> getAliases() {
            return aliases;
        }

        List<String> getAllKeys() {
            List<String> keys = new ArrayList<>();
            if (primaryKey != null) {
                keys.add(primaryKey);
            }

            if (aliases != null) {
                keys.addAll(aliases);
            }

            return keys;
        }

        synchronized SkinTextures toSkinTextures() {
            if (cached != null) {
                return cached;
            }

            Identifier textureId = Identifier.tryParse(texture);
            if (textureId == null) {
                return null;
            }

            Identifier capeId = Identifier.tryParse(capeTexture);
            Identifier elytraId = Identifier.tryParse(elytraTexture);
            SkinTextures.Model modelType = model == null ? SkinTextures.Model.WIDE : SkinTextures.Model.fromName(model);
            if (modelType == null) {
                modelType = SkinTextures.Model.WIDE;
            }

            cached = new SkinTextures(textureId, textureUrl, capeId, elytraId, modelType, secure);
            return cached;
        }

        synchronized void update(SkinTextures textures, GameProfile sourceProfile) {
            if (textures != null) {
                Identifier textureId = textures.texture();
                Identifier capeId = textures.capeTexture();
                Identifier elytraId = textures.elytraTexture();

                this.texture = textureId == null ? null : textureId.toString();
                this.textureUrl = textures.textureUrl();
                this.capeTexture = capeId == null ? null : capeId.toString();
                this.elytraTexture = elytraId == null ? null : elytraId.toString();
                this.model = textures.model() == null ? SkinTextures.Model.WIDE.getName() : textures.model().getName();
                this.secure = textures.secure();
                this.cached = null;
            }

            if (sourceProfile != null) {
                this.sourceName = sourceProfile.getName();
                UUID sourceUuid = sourceProfile.getId();
                if (sourceUuid != null) {
                    this.sourceUuid = sourceUuid.toString();
                }
            }
        }

        void ensureReady() {
            if (texture == null || texture.isEmpty()) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return;
            }

            Identifier identifier = Identifier.tryParse(texture);
            if (identifier == null) {
                return;
            }

            AbstractTexture textureInstance = client.getTextureManager().getTexture(identifier);
            if (textureInstance != null && textureInstance.getGlId() != 0) {
                return;
            }

            requestRefresh(this);
        }

        synchronized boolean markRefreshing() {
            if (refreshing) {
                return false;
            }

            refreshing = true;
            return true;
        }

        synchronized void finishRefreshing() {
            refreshing = false;
        }

        GameProfile createSourceProfile() {
            UUID uuid = parseUuid(sourceUuid);
            if (uuid == null && (sourceName == null || sourceName.isEmpty())) {
                return null;
            }

            return new GameProfile(uuid, sourceName);
        }

        private UUID parseUuid(String rawUuid) {
            if (rawUuid == null || rawUuid.isEmpty()) {
                return null;
            }

            try {
                return UUID.fromString(rawUuid);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
