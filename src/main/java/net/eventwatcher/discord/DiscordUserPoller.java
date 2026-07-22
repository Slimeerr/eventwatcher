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
import net.eventwatcher.config.EventWatcherConfig;
import org.jetbrains.annotations.Nullable;

public class DiscordUserPoller implements DiscordSource {
   private static final String API = "https://discord.com/api/v10";
   private static final long POLL_INTERVAL_MS = 3000L;
   private static final long STAGGER_MS = 350L;
   private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
   private final String token;
   private final EventWatcherConfig config;
   private final DiscordSource.MatchListener listener;
   private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "EventWatcher-DiscordUser");
      t.setDaemon(true);
      return t;
   });
   private volatile boolean running;
   private volatile boolean connected;
   private final java.util.Map<String, String> lastByChannel = new java.util.concurrent.ConcurrentHashMap<>();
   private final java.util.Map<String, Long> retryAtByChannel = new java.util.concurrent.ConcurrentHashMap<>();
   @Nullable
   private volatile ScheduledFuture<?> task;

   public DiscordUserPoller(EventWatcherConfig config, DiscordSource.MatchListener listener) {
      this.config = config;
      this.token = config.discordToken;
      this.listener = listener;
   }

   @Override
   public void start() {
      if (!this.running) {
         this.running = true;
         EventWatcherClient.LOGGER
            .warn("Starting Discord USER-TOKEN poller (selfbot mode). This violates Discord's ToS — use a throwaway/alt account, not your main.");
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

   private void poll() {
      if (this.running) {
         java.util.List<String> channels = this.config.watchedChannels();

         for (int i = 0; i < channels.size(); i++) {
            if (!this.running) {
               return;
            }

            String channelId = channels.get(i);

            try {
               this.pollChannel(channelId);
            } catch (Exception var5) {
               this.connected = false;
               EventWatcherClient.LOGGER.warn("Poll of channel {} failed (will retry): {}", channelId, var5.toString());
            }

            // Spread requests out so many channels don't burst the rate limit at once.
            if (i < channels.size() - 1 && channels.size() > 1) {
               try {
                  Thread.sleep(STAGGER_MS);
               } catch (InterruptedException var4) {
                  Thread.currentThread().interrupt();
                  return;
               }
            }
         }
      }
   }

   private void pollChannel(String channelId) throws Exception {
      Long retryAt = this.retryAtByChannel.get(channelId);
      if (retryAt != null) {
         if (System.currentTimeMillis() < retryAt) {
            return;
         }

         this.retryAtByChannel.remove(channelId);
      }

      String last = this.lastByChannel.get(channelId);
      boolean firstRun = last == null;
      String query = firstRun ? "?limit=1" : "?limit=50&after=" + last;
      HttpRequest req = HttpRequest.newBuilder()
         .uri(URI.create("https://discord.com/api/v10/channels/" + channelId + "/messages" + query))
         .timeout(Duration.ofSeconds(10L))
         .header("Authorization", this.token)
         .header("User-Agent", USER_AGENT)
         .header("Accept", "application/json")
         .GET()
         .build();
      HttpResponse<String> resp = this.http.send(req, BodyHandlers.ofString());
      int sc = resp.statusCode();
      switch (sc) {
         case 200:
            this.connected = true;
            this.handleMessages(channelId, resp.body(), firstRun);
            break;
         case 401:
            this.halt("Discord rejected the USER token (401 Unauthorized) — wrong/expired token.");
            break;
         case 403:
            EventWatcherClient.LOGGER.warn("No access to channel {} (403) — skipping it. Is the alt in that server and able to see the channel?", channelId);
            break;
         case 404:
            EventWatcherClient.LOGGER.warn("Channel {} not found (404) — skipping it. Check the Channel ID.", channelId);
            break;
         case 429:
            this.connected = true;
            long retryMs = retryAfterMs(resp);
            this.retryAtByChannel.put(channelId, System.currentTimeMillis() + retryMs);
            EventWatcherClient.LOGGER.warn("Discord rate-limited the poller (429) on channel {}; backing off ~{} ms.", channelId, retryMs);
            break;
         default:
            EventWatcherClient.LOGGER.warn("Discord poll of channel {} returned HTTP {} — will retry.", channelId, sc);
      }
   }

   private void handleMessages(String channelId, String body, boolean firstRun) {
      JsonElement parsed = JsonParser.parseString(body);
      if (parsed.isJsonArray()) {
         JsonArray arr = parsed.getAsJsonArray();
         if (!arr.isEmpty()) {
            String newest = this.lastByChannel.get(channelId);

            for (JsonElement el : arr) {
               if (el.isJsonObject()) {
                  JsonObject msg = el.getAsJsonObject();
                  String id = msg.has("id") && !msg.get("id").isJsonNull() ? msg.get("id").getAsString() : null;
                  if (id != null && (newest == null || compareSnowflake(id, newest) > 0)) {
                     newest = id;
                  }

                  if (!firstRun) {
                     this.scan(channelId, msg);
                  }
               }
            }

            if (newest != null) {
               this.lastByChannel.put(channelId, newest);
            }

            if (firstRun) {
               EventWatcherClient.LOGGER.info("Discord user poller now watching channel {}.", channelId);
            }
         }
      }
   }

   private void scan(String channelId, JsonObject msg) {
      String text = MessageScanner.extractText(msg);
      MessageScanner.ServerMatch matched = MessageScanner.matchServer(text, channelId, this.config.servers);
      if (matched != null) {
         EventWatcherClient.LOGGER.info("Keyword '{}' detected — target {}.", matched.keyword(), matched.server().displayName());
         MessageScanner.Author author = MessageScanner.extractAuthor(msg);
         DiscordSource.DetectedMessage dm = new DiscordSource.DetectedMessage(
            text, matched.keyword(), author.name(), author.id(), author.avatarHash()
         );

         try {
            this.listener.onMatch(dm, matched.server());
         } catch (Exception var6) {
            EventWatcherClient.LOGGER.error("Keyword listener threw", var6);
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
