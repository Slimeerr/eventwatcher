package net.eventwatcher.config;

import java.util.ArrayList;
import java.util.List;

public class WatchConfig {
   public String label = "";
   public String discordToken = "";
   public String channelId = "";
   public boolean useUserToken = false;
   public List<String> keywords = new ArrayList<>(EventWatcherConfig.DEFAULT_KEYWORDS);
   public String targetServer = EventWatcherConfig.DEFAULT_TARGET;

   public boolean isConfigured() {
      return this.discordToken != null && !this.discordToken.isBlank() && this.channelId != null && !this.channelId.isBlank();
   }

   public String describe() {
      if (this.label != null && !this.label.isBlank()) {
         return this.label;
      } else if (this.targetServer != null && !this.targetServer.isBlank()) {
         return this.targetServer;
      } else {
         return this.channelId == null || this.channelId.isBlank() ? "unnamed watch" : "channel " + this.channelId;
      }
   }

   public void sanitize() {
      this.label = this.label == null ? "" : this.label.trim();
      this.discordToken = this.discordToken == null ? "" : this.discordToken.trim();
      this.channelId = this.channelId == null ? "" : this.channelId.trim();
      if (this.keywords == null) {
         this.keywords = new ArrayList<>(EventWatcherConfig.DEFAULT_KEYWORDS);
      }

      this.targetServer = this.targetServer == null || this.targetServer.isBlank() ? EventWatcherConfig.DEFAULT_TARGET : this.targetServer.trim();
   }

   public WatchConfig copy() {
      WatchConfig w = new WatchConfig();
      w.label = this.label;
      w.discordToken = this.discordToken;
      w.channelId = this.channelId;
      w.useUserToken = this.useUserToken;
      w.keywords = new ArrayList<>(this.keywords);
      w.targetServer = this.targetServer;
      return w;
   }
}
