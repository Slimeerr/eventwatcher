package net.eventwatcher.discord;

public interface DiscordSource {
   void start();

   void stop();

   boolean isConnected();

   @FunctionalInterface
   public interface MatchListener {
      void onMatch(String var1, String var2);
   }
}
