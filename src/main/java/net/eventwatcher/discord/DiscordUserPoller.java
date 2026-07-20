package net.eventwatcher.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.WatchConfig;
import org.jetbrains.annotations.Nullable;

public class DiscordUserPoller implements DiscordSource {
   private static final String API = "https://discord.com/api/v10";
   private static final long POLL_INTERVAL_MS = 3000L;
   private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
   private final String token;
   private final WatchConfig watch;
   private final DiscordSource.MatchListener listener;
   private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "EventWatcher-DiscordUser");
      t.setDaemon(true);
      return t;
   });
   private volatile boolean running;
   private volatile boolean connected;
   private volatile boolean primed;
   private volatile long pausedUntil;
   @Nullable
   private volatile String lastMessageId;
   @Nullable
   private volatile ScheduledFuture<?> task;

   public DiscordUserPoller(WatchConfig watch, DiscordSource.MatchListener listener) {
      this.watch = watch;
      this.token = watch.discordToken;
      this.listener = listener;
   }

   @Override
   public void start() {
      if (!this.running) {
         this.running = true;
         EventWatcherClient.LOGGER
            .warn(
               "Starting Discord USER-TOKEN poller for '{}' (selfbot mode). This violates Discord's ToS — use a throwaway/alt account, not your main.",
               this.watch.describe()
            );
         this.task = this.scheduler.scheduleWithFixedDelay(this::pollSafely, 0L, 3000L, TimeUnit.MILLISECONDS);
      }
   }

   @Override
   public void stop() {
      this.running = false;
      this.connected = false;
      ScheduledFuture<?> t = this.task;
      if (t != null) {
         t.cancel(false);
         this.task = null;
      }

      this.scheduler.shutdownNow();
      EventWatcherClient.LOGGER.info("Discord user poller stopped.");
   }

   @Override
   public boolean isConnected() {
      return this.running && this.connected;
   }

   private void halt(String why) {
      EventWatcherClient.LOGGER.error("{} Stopping the EventWatcher poller; fix it in settings and Save to retry.", why);
      this.connected = false;
      this.running = false;
      ScheduledFuture<?> t = this.task;
      if (t != null) {
         t.cancel(false);
         this.task = null;
      }
   }

   private void pollSafely() {
      try {
         this.poll();
      } catch (Exception var2) {
         this.connected = false;
         EventWatcherClient.LOGGER.warn("Discord poll failed (will retry): {}", var2.toString());
      }
   }

   private void poll() throws Exception {
      if (this.running && System.currentTimeMillis() >= this.pausedUntil) {
         String channelId = this.watch.channelId == null ? "" : this.watch.channelId.trim();
         if (!channelId.isEmpty()) {
            if (!channelId.chars().allMatch(Character::isDigit)) {
               this.halt("Channel ID '" + channelId + "' is not a numeric Discord channel ID.");
               return;
            }

            boolean baseline = !this.primed;
            String anchor = this.lastMessageId;
            String query = anchor != null ? "?limit=50&after=" + anchor : (baseline ? "?limit=1" : "?limit=50");
            HttpRequest req = HttpRequest.newBuilder()
               .uri(URI.create("https://discord.com/api/v10/channels/" + channelId + "/messages" + query))
               .timeout(Duration.ofSeconds(10L))
               .header("Authorization", this.token)
               .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
               .header("Accept", "application/json")
               .GET()
               .build();
            HttpResponse<String> resp = this.http.send(req, BodyHandlers.ofString());
            int sc = resp.statusCode();
            switch (sc) {
               case 200:
                  this.connected = true;
                  this.handleMessages(resp.body(), baseline);
                  if (baseline) {
                     this.primed = true;
                     EventWatcherClient.LOGGER.info("Discord user poller connected; watching channel for new messages.");
                  }
                  break;
               case 401:
                  this.halt("Discord rejected the USER token (401 Unauthorized) — wrong/expired token.");
                  break;
               case 403:
                  this.halt("No access to channel " + channelId + " (403 Forbidden) — is the alt account in that server and able to see the channel?");
                  break;
               case 404:
                  this.halt("Channel " + channelId + " not found (404) — check the Channel ID.");
                  break;
               case 429:
                  this.connected = true;
                  long retryMs = retryAfterMs(resp);
                  this.pausedUntil = System.currentTimeMillis() + retryMs;
                  EventWatcherClient.LOGGER.warn("Discord rate-limited the poller (429); pausing polls for ~{} ms.", retryMs);
                  break;
               default:
                  EventWatcherClient.LOGGER.warn("Discord poll returned HTTP {} — will retry.", sc);
            }
         }
      }
   }

   private void handleMessages(String body, boolean baseline) {
      JsonElement parsed = JsonParser.parseString(body);
      if (parsed.isJsonArray()) {
         JsonArray arr = parsed.getAsJsonArray();
         if (!arr.isEmpty()) {
            String newest = this.lastMessageId;

            for (JsonElement el : arr) {
               if (el.isJsonObject()) {
                  JsonObject msg = el.getAsJsonObject();
                  String id = msg.has("id") && !msg.get("id").isJsonNull() ? msg.get("id").getAsString() : null;
                  if (id != null && (newest == null || compareSnowflake(id, newest) > 0)) {
                     newest = id;
                  }

                  if (!baseline) {
                     this.scan(msg);
                  }
               }
            }

            if (newest != null) {
               this.lastMessageId = newest;
            }
         }
      }
   }

   private void scan(JsonObject msg) {
      String text = MessageScanner.extractText(msg);
      String matched = MessageScanner.matchKeyword(text, this.watch.keywords);
      if (matched != null) {
         EventWatcherClient.LOGGER.info("Keyword '{}' detected in monitored channel ({}).", matched, this.watch.describe());

         try {
            this.listener.onMatch(this.watch, text, matched);
         } catch (Exception var5) {
            EventWatcherClient.LOGGER.error("Keyword listener threw", var5);
         }
      }
   }

   private static int compareSnowflake(String a, String b) {
      try {
         return Long.compareUnsigned(Long.parseUnsignedLong(a), Long.parseUnsignedLong(b));
      } catch (NumberFormatException var3) {
         return a.compareTo(b);
      }
   }

   private static long retryAfterMs(HttpResponse<String> resp) {
      Optional<String> ra = resp.headers().firstValue("Retry-After");
      if (ra.isPresent()) {
         try {
            return (long)(Double.parseDouble(ra.get()) * 1000.0);
         } catch (NumberFormatException var3) {
         }
      }

      return 3000L;
   }
}
