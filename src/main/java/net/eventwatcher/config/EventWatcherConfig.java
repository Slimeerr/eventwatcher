package net.eventwatcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.eventwatcher.EventWatcherClient;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

public class EventWatcherConfig {
   public static final List<String> DEFAULT_KEYWORDS = Arrays.asList("event", "parrot", "spoke", "flame", "wemmbu", "horace");
   public static final String DEFAULT_TARGET = "unstableevents.net";
   public List<WatchConfig> watches = new ArrayList<>();
   public boolean autoConnect = false;
   public boolean soundEnabled = true;
   public String soundType = "minecraft";
   public String minecraftSound = "block.note_block.pling";
   public String soundFilePath = "";
   @Nullable
   private String discordToken;
   @Nullable
   private String channelId;
   @Nullable
   private Boolean useUserToken;
   @Nullable
   private List<String> keywords;
   @Nullable
   private String targetServer;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("eventwatcher.json");
   }

   public static EventWatcherConfig load() {
      Path path = configPath();
      if (Files.exists(path)) {
         try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            EventWatcherConfig loaded = GSON.fromJson(reader, EventWatcherConfig.class);
            if (loaded != null) {
               loaded.sanitize();
               return loaded;
            }
         } catch (Exception var6) {
            EventWatcherClient.LOGGER.error("Failed to read {} — using defaults", path, var6);
         }
      }

      EventWatcherConfig fresh = new EventWatcherConfig();
      fresh.sanitize();
      fresh.save();
      return fresh;
   }

   public void save() {
      Path path = configPath();

      try {
         Files.createDirectories(path.getParent());

         try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
         }

         EventWatcherClient.LOGGER.info("Saved config to {}", path);
      } catch (Exception var7) {
         EventWatcherClient.LOGGER.error("Failed to save {}", path, var7);
      }
   }

   public void sanitize() {
      if (this.watches == null) {
         this.watches = new ArrayList<>();
      }

      this.watches.removeIf(Objects::isNull);
      this.migrateLegacyFields();
      if (this.watches.isEmpty()) {
         this.watches.add(new WatchConfig());
      }

      for (WatchConfig watch : this.watches) {
         watch.sanitize();
      }

      if (this.soundType == null || this.soundType.isBlank()) {
         this.soundType = "minecraft";
      }

      if (this.minecraftSound == null || this.minecraftSound.isBlank()) {
         this.minecraftSound = "block.note_block.pling";
      }

      if (this.soundFilePath == null) {
         this.soundFilePath = "";
      }
   }

   private void migrateLegacyFields() {
      if (this.discordToken != null || this.channelId != null || this.useUserToken != null || this.keywords != null || this.targetServer != null) {
         WatchConfig migrated = new WatchConfig();
         migrated.discordToken = this.discordToken == null ? "" : this.discordToken;
         migrated.channelId = this.channelId == null ? "" : this.channelId;
         migrated.useUserToken = this.useUserToken != null && this.useUserToken;
         if (this.keywords != null) {
            migrated.keywords = new ArrayList<>(this.keywords);
         }

         if (this.targetServer != null) {
            migrated.targetServer = this.targetServer;
         }

         this.watches.add(migrated);
         this.discordToken = null;
         this.channelId = null;
         this.useUserToken = null;
         this.keywords = null;
         this.targetServer = null;
         EventWatcherClient.LOGGER.info("Migrated legacy single-watch config to the multi-watch format.");
      }
   }

   public EventWatcherConfig copy() {
      EventWatcherConfig c = new EventWatcherConfig();
      c.watches = new ArrayList<>();

      for (WatchConfig watch : this.watches) {
         c.watches.add(watch.copy());
      }

      c.autoConnect = this.autoConnect;
      c.soundEnabled = this.soundEnabled;
      c.soundType = this.soundType;
      c.minecraftSound = this.minecraftSound;
      c.soundFilePath = this.soundFilePath;
      return c;
   }

   public boolean isConfigured() {
      return this.watches != null && this.watches.stream().anyMatch(WatchConfig::isConfigured);
   }
}
