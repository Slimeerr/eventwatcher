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

   // Shared connection settings
   public String discordToken = "";
   public List<String> channelIds = new ArrayList<>();
   public boolean useUserToken = false;

   // Each watched server carries its own keywords and Notify/Auto-Connect mode.
   // A message routes to the first server whose keywords it matches.
   public List<ServerEntry> servers = new ArrayList<>();

   // Shared sound settings
   public boolean soundEnabled = true;
   public String soundType = "minecraft";
   public String minecraftSound = "block.note_block.pling";
   public String soundFilePath = "";

   // Legacy single-server fields. Kept only so old configs migrate forward.
   public List<String> keywords = new ArrayList<>(DEFAULT_KEYWORDS);
   public boolean autoConnect = false;
   public String targetServer = "unstableevents.net";
   public String channelId = "";

   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   public static class ServerEntry {
      public String name = "";
      public String address = "";
      public List<String> channelIds = new ArrayList<>();
      public List<String> keywords = new ArrayList<>();
      public boolean autoConnect = false;
      // Match a keyword only as a whole word (so "event" won't fire on "prevent").
      public boolean wholeWord = false;
      // Ignore repeat matches for this server for N seconds (0 = off).
      public int cooldownSeconds = 0;
      // Keep retrying the join if the server is full/queued, until you get in (auto-connect only).
      public boolean retryJoin = false;

      public ServerEntry() {
      }

      public ServerEntry(String name, String address, List<String> keywords, boolean autoConnect) {
         this.name = name;
         this.address = address;
         this.keywords = new ArrayList<>(keywords);
         this.autoConnect = autoConnect;
      }

      public String displayName() {
         if (this.name != null && !this.name.isBlank()) {
            return this.name;
         }
         return this.address != null && !this.address.isBlank() ? this.address : "New server";
      }

      public boolean isBlank() {
         return (this.address == null || this.address.isBlank()) && (this.keywords == null || this.keywords.isEmpty());
      }

      public ServerEntry copy() {
         ServerEntry e = new ServerEntry();
         e.name = this.name;
         e.address = this.address;
         e.channelIds = new ArrayList<>(this.channelIds == null ? List.of() : this.channelIds);
         e.keywords = new ArrayList<>(this.keywords == null ? List.of() : this.keywords);
         e.autoConnect = this.autoConnect;
         e.wholeWord = this.wholeWord;
         e.cooldownSeconds = this.cooldownSeconds;
         e.retryJoin = this.retryJoin;
         return e;
      }

      /** Trimmed, de-duplicated, non-blank channel IDs this server watches. */
      public List<String> channels() {
         List<String> out = new ArrayList<>();
         if (this.channelIds != null) {
            for (String c : this.channelIds) {
               if (c != null && !c.isBlank()) {
                  String t = c.trim();
                  if (!out.contains(t)) {
                     out.add(t);
                  }
               }
            }
         }

         return out;
      }

      void sanitize() {
         if (this.name == null) {
            this.name = "";
         }

         if (this.address == null) {
            this.address = "";
         }

         if (this.channelIds == null) {
            this.channelIds = new ArrayList<>();
         }

         if (this.keywords == null) {
            this.keywords = new ArrayList<>();
         }

         if (this.cooldownSeconds < 0) {
            this.cooldownSeconds = 0;
         }
      }
   }

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
      if (this.discordToken == null) {
         this.discordToken = "";
      }

      if (this.channelIds == null) {
         this.channelIds = new ArrayList<>();
      }

      // Migrate a legacy single channel into the channel list.
      if (this.channelIds.isEmpty() && this.channelId != null && !this.channelId.isBlank()) {
         this.channelIds.add(this.channelId.trim());
      }

      if (this.servers == null) {
         this.servers = new ArrayList<>();
      }

      // Migrate a legacy single-server config into the servers list.
      if (this.servers.isEmpty()) {
         List<String> legacyKeywords = this.keywords == null || this.keywords.isEmpty()
            ? new ArrayList<>(DEFAULT_KEYWORDS)
            : new ArrayList<>(this.keywords);
         String legacyTarget = this.targetServer == null || this.targetServer.isBlank() ? DEFAULT_TARGET : this.targetServer;
         this.servers.add(new ServerEntry("Server 1", legacyTarget, legacyKeywords, this.autoConnect));
      }

      for (ServerEntry entry : this.servers) {
         entry.sanitize();
      }

      // Migrate the old global channel list into any server that has no channel of its own,
      // so a previously global-channel setup keeps watching the same channels per server.
      if (!this.channelIds.isEmpty()) {
         List<String> globalChannels = new ArrayList<>();
         for (String c : this.channelIds) {
            if (c != null && !c.isBlank() && !globalChannels.contains(c.trim())) {
               globalChannels.add(c.trim());
            }
         }

         for (ServerEntry entry : this.servers) {
            if (entry.channels().isEmpty()) {
               entry.channelIds = new ArrayList<>(globalChannels);
            }
         }
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
      c.channelIds = new ArrayList<>(this.channelIds == null ? List.of() : this.channelIds);
      c.channelId = this.channelId;
      c.useUserToken = this.useUserToken;
      c.servers = new ArrayList<>();

      for (ServerEntry entry : this.servers) {
         c.servers.add(entry.copy());
      }

      c.soundEnabled = this.soundEnabled;
      c.soundType = this.soundType;
      c.minecraftSound = this.minecraftSound;
      c.soundFilePath = this.soundFilePath;
      c.keywords = new ArrayList<>(this.keywords == null ? List.of() : this.keywords);
      c.autoConnect = this.autoConnect;
      c.targetServer = this.targetServer;
      return c;
   }

   public boolean isConfigured() {
      return this.discordToken != null && !this.discordToken.isBlank() && !this.watchedChannels().isEmpty();
   }

   /** Union of every server's channels — the full set of channels to watch. */
   public List<String> watchedChannels() {
      List<String> out = new ArrayList<>();
      if (this.servers != null) {
         for (ServerEntry entry : this.servers) {
            for (String c : entry.channels()) {
               if (!out.contains(c)) {
                  out.add(c);
               }
            }
         }
      }

      return out;
   }

   public String firstServerAddress() {
      if (this.servers != null) {
         for (ServerEntry entry : this.servers) {
            if (entry.address != null && !entry.address.isBlank()) {
               return entry.address;
            }
         }
      }

      return "";
   }

   /**
    * Keywords (lower-cased) that appear on more than one server. When a message
    * matches a shared keyword, the earliest server in the list wins, so these are
    * worth flagging to the user.
    */
   public List<String> duplicateKeywords() {
      java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
      if (this.servers != null) {
         for (ServerEntry entry : this.servers) {
            if (entry.keywords == null) {
               continue;
            }

            java.util.Set<String> seenInEntry = new java.util.HashSet<>();
            for (String kw : entry.keywords) {
               if (kw != null && !kw.isBlank()) {
                  String key = kw.trim().toLowerCase(java.util.Locale.ROOT);
                  // Count a keyword once per server, so intra-server repeats don't count.
                  if (seenInEntry.add(key)) {
                     counts.merge(key, 1, Integer::sum);
                  }
               }
            }
         }
      }

      List<String> dupes = new ArrayList<>();
      for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
         if (e.getValue() > 1) {
            dupes.add(e.getKey());
         }
      }

      return dupes;
   }
}
