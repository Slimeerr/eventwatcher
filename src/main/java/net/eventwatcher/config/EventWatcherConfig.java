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
import net.eventwatcher.EventWatcherClient;
import net.fabricmc.loader.api.FabricLoader;

public class EventWatcherConfig {
   public static final List<String> DEFAULT_KEYWORDS = Arrays.asList("event", "parrot", "spoke", "flame", "wemmbu", "horace");
   public static final String DEFAULT_TARGET = "unstableevents.net";
   public String discordToken = "";
   public String channelId = "";
   public boolean useUserToken = false;
   public List<String> keywords = new ArrayList<>(DEFAULT_KEYWORDS);
   public boolean autoConnect = false;
   public String targetServer = "unstableevents.net";
   public boolean soundEnabled = true;
   public String soundType = "minecraft";
   public String minecraftSound = "block.note_block.pling";
   public String soundFilePath = "";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("eventwatcher.json");
   }

   public static EventWatcherConfig load() {
      Path path = configPath();
      if (Files.exists(path)) {
         try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            EventWatcherConfig loaded = (EventWatcherConfig)GSON.fromJson(reader, EventWatcherConfig.class);
            if (loaded != null) {
               loaded.sanitize();
               return loaded;
            }
         } catch (Exception var6) {
            EventWatcherClient.LOGGER.error("Failed to read {} — using defaults", path, var6);
         }
      }

      EventWatcherConfig fresh = new EventWatcherConfig();
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
      if (this.discordToken == null) {
         this.discordToken = "";
      }

      if (this.channelId == null) {
         this.channelId = "";
      }

      if (this.keywords == null) {
         this.keywords = new ArrayList<>(DEFAULT_KEYWORDS);
      }

      if (this.targetServer == null || this.targetServer.isBlank()) {
         this.targetServer = "unstableevents.net";
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

   public EventWatcherConfig copy() {
      EventWatcherConfig c = new EventWatcherConfig();
      c.discordToken = this.discordToken;
      c.channelId = this.channelId;
      c.useUserToken = this.useUserToken;
      c.keywords = new ArrayList<>(this.keywords);
      c.autoConnect = this.autoConnect;
      c.targetServer = this.targetServer;
      c.soundEnabled = this.soundEnabled;
      c.soundType = this.soundType;
      c.minecraftSound = this.minecraftSound;
      c.soundFilePath = this.soundFilePath;
      return c;
   }

   public boolean isConfigured() {
      return this.discordToken != null && !this.discordToken.isBlank() && this.channelId != null && !this.channelId.isBlank();
   }
}
