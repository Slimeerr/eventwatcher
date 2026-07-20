package net.eventwatcher.discord;

import net.eventwatcher.config.WatchConfig;

public interface DiscordSource {
   void start();

   void stop();

   boolean isConnected();

   @FunctionalInterface
   public interface MatchListener {
      void onMatch(WatchConfig watch, String message, String keyword);
   }
}
