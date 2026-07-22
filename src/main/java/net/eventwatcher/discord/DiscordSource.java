package net.eventwatcher.discord;

import net.eventwatcher.config.EventWatcherConfig.ServerEntry;

public interface DiscordSource {
   void start();

   void stop();

   boolean isConnected();

   @FunctionalInterface
   public interface MatchListener {
      void onMatch(DetectedMessage message, ServerEntry server);
   }

   /** A matched message plus who posted it, passed to the match listener. */
   public record DetectedMessage(String content, String keyword, String authorName, String authorId, String avatarHash) {
   }
}
