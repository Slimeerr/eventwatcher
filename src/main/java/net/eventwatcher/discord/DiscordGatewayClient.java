package net.eventwatcher.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.WatchConfig;
import org.jetbrains.annotations.Nullable;

public class DiscordGatewayClient implements DiscordSource {
   private static final int OP_DISPATCH = 0;
   private static final int OP_HEARTBEAT = 1;
   private static final int OP_IDENTIFY = 2;
   private static final int OP_RESUME = 6;
   private static final int OP_RECONNECT = 7;
   private static final int OP_INVALID_SESSION = 9;
   private static final int OP_HELLO = 10;
   private static final int OP_HEARTBEAT_ACK = 11;
   private static final int INTENTS = 37376;
   private static final String GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";
   private static final long RECONNECT_DELAY_MS = 5000L;
   private static final long RATE_LIMIT_RECONNECT_DELAY_MS = 15000L;
   private final String token;
   private final WatchConfig watch;
   private final DiscordSource.MatchListener listener;
   private final HttpClient http = HttpClient.newHttpClient();
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "EventWatcher-Discord");
      t.setDaemon(true);
      return t;
   });
   private final Object bufferLock = new Object();
   private final StringBuilder textBuffer = new StringBuilder();
   private final Object sendLock = new Object();
   private final AtomicBoolean reconnectPending = new AtomicBoolean(false);
   private volatile WebSocket webSocket;
   private volatile boolean running;
   private volatile boolean connected;
   private volatile boolean heartbeatAcked = true;
   @Nullable
   private volatile Integer lastSequence;
   @Nullable
   private volatile String sessionId;
   @Nullable
   private volatile String resumeGatewayUrl;
   private volatile boolean shouldResume;
   @Nullable
   private volatile ScheduledFuture<?> heartbeatTask;

   public DiscordGatewayClient(WatchConfig watch, DiscordSource.MatchListener listener) {
      this.watch = watch;
      this.token = watch.discordToken;
      this.listener = listener;
   }

   @Override
   public void start() {
      if (!this.running) {
         this.running = true;
         this.shouldResume = false;
         EventWatcherClient.LOGGER.info("Starting Discord gateway client for '{}'...", this.watch.describe());
         this.openSocket();
      }
   }

   @Override
   public void stop() {
      this.running = false;
      this.connected = false;
      this.cancelHeartbeat();
      WebSocket old = this.webSocket;
      this.webSocket = null;
      if (old != null) {
         try {
            old.sendClose(1000, "EventWatcher shutting down");
         } catch (Exception var3) {
         }
      }

      this.scheduler.shutdownNow();
      EventWatcherClient.LOGGER.info("Discord gateway client stopped.");
   }

   @Override
   public boolean isConnected() {
      return this.running && this.connected;
   }

   private void openSocket() {
      if (this.running) {
         String url = this.shouldResume && this.resumeGatewayUrl != null ? this.resumeGatewayUrl + "/?v=10&encoding=json" : GATEWAY_URL;
         EventWatcherClient.LOGGER.info("Connecting to Discord gateway ({})...", this.shouldResume ? "resume" : "fresh");

         try {
            this.http.newWebSocketBuilder().buildAsync(URI.create(url), new DiscordGatewayClient.Listener()).whenComplete((ws, err) -> {
               if (err != null) {
                  EventWatcherClient.LOGGER.warn("Failed to open Discord gateway: {}", err.toString());
                  if (this.running) {
                     this.scheduleReconnect(true);
                  }
               }
            });
         } catch (Exception var3) {
            EventWatcherClient.LOGGER.warn("Failed to start gateway connection: {}", var3.toString());
            if (this.running) {
               this.scheduleReconnect(true);
            }
         }
      }
   }

   private void scheduleReconnect(boolean resume) {
      this.scheduleReconnect(resume, RECONNECT_DELAY_MS);
   }

   private void scheduleReconnect(boolean resume, long delayMs) {
      if (this.running) {
         this.cancelHeartbeat();
         this.connected = false;
         this.shouldResume = resume && this.sessionId != null && this.resumeGatewayUrl != null;
         WebSocket old = this.webSocket;
         this.webSocket = null;
         if (old != null) {
            try {
               old.abort();
            } catch (Exception var6) {
            }
         }

         if (this.reconnectPending.compareAndSet(false, true)) {
            try {
               this.scheduler.schedule(() -> {
                  this.reconnectPending.set(false);
                  this.openSocket();
               }, delayMs, TimeUnit.MILLISECONDS);
            } catch (Exception var5) {
               this.reconnectPending.set(false);
            }
         }
      }
   }

   private void startHeartbeat(long intervalMs) {
      this.cancelHeartbeat();
      this.heartbeatAcked = true;
      long initialDelay = (long)(intervalMs * Math.random());
      this.heartbeatTask = this.scheduler.scheduleAtFixedRate(() -> {
         if (!this.heartbeatAcked) {
            EventWatcherClient.LOGGER.warn("No heartbeat ACK from Discord — reconnecting.");
            this.scheduleReconnect(true);
         } else {
            this.heartbeatAcked = false;
            this.sendHeartbeat();
         }
      }, initialDelay, intervalMs, TimeUnit.MILLISECONDS);
   }

   private void cancelHeartbeat() {
      ScheduledFuture<?> task = this.heartbeatTask;
      if (task != null) {
         task.cancel(false);
         this.heartbeatTask = null;
      }
   }

   private void sendHeartbeat() {
      JsonObject payload = new JsonObject();
      payload.addProperty("op", 1);
      if (this.lastSequence == null) {
         payload.add("d", JsonNull.INSTANCE);
      } else {
         payload.addProperty("d", this.lastSequence);
      }

      this.send(payload);
   }

   private void sendIdentify() {
      this.lastSequence = null;
      JsonObject properties = new JsonObject();
      properties.addProperty("os", System.getProperty("os.name", "unknown"));
      properties.addProperty("browser", "eventwatcher");
      properties.addProperty("device", "eventwatcher");
      JsonObject d = new JsonObject();
      d.addProperty("token", this.token);
      d.addProperty("intents", INTENTS);
      d.add("properties", properties);
      JsonObject payload = new JsonObject();
      payload.addProperty("op", 2);
      payload.add("d", d);
      EventWatcherClient.LOGGER.info("Identifying with Discord gateway...");
      this.send(payload);
   }

   private void sendResume() {
      JsonObject d = new JsonObject();
      d.addProperty("token", this.token);
      d.addProperty("session_id", this.sessionId);
      if (this.lastSequence != null) {
         d.addProperty("seq", this.lastSequence);
      } else {
         d.add("seq", JsonNull.INSTANCE);
      }

      JsonObject payload = new JsonObject();
      payload.addProperty("op", 6);
      payload.add("d", d);
      EventWatcherClient.LOGGER.info("Resuming Discord session {}...", this.sessionId);
      this.send(payload);
   }

   private void send(JsonObject payload) {
      WebSocket ws = this.webSocket;
      if (ws != null) {
         synchronized (this.sendLock) {
            try {
               ws.sendText(payload.toString(), true).get(10L, TimeUnit.SECONDS);
            } catch (Exception var6) {
               EventWatcherClient.LOGGER.warn("Failed to send gateway payload: {}", var6.toString());
            }
         }
      }
   }

   private void handlePayload(WebSocket source, String text) {
      if (source == this.webSocket) {
         JsonObject payload;
         try {
            payload = JsonParser.parseString(text).getAsJsonObject();
         } catch (Exception var7) {
            EventWatcherClient.LOGGER.warn("Unparseable gateway payload: {}", var7.toString());
            return;
         }

         if (payload.has("s") && !payload.get("s").isJsonNull()) {
            this.lastSequence = payload.get("s").getAsInt();
         }

         int op = payload.has("op") ? payload.get("op").getAsInt() : -1;
         switch (op) {
            case 0:
               this.handleDispatch(
                  payload.has("t") && !payload.get("t").isJsonNull() ? payload.get("t").getAsString() : "",
                  payload.has("d") && payload.get("d").isJsonObject() ? payload.getAsJsonObject("d") : new JsonObject()
               );
               break;
            case 1:
               this.sendHeartbeat();
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 8:
            default:
               break;
            case 7:
               EventWatcherClient.LOGGER.info("Discord requested a reconnect.");
               this.scheduleReconnect(true);
               break;
            case 9:
               JsonElement invalidSessionData = payload.get("d");
               boolean resumable = invalidSessionData != null && invalidSessionData.isJsonPrimitive() && invalidSessionData.getAsBoolean();
               EventWatcherClient.LOGGER.warn("Invalid session (resumable={}).", resumable);
               if (!resumable) {
                  this.sessionId = null;
               }

               this.scheduleReconnect(resumable);
               break;
            case 10:
               long interval = payload.getAsJsonObject("d").get("heartbeat_interval").getAsLong();
               this.startHeartbeat(interval);
               if (this.shouldResume && this.sessionId != null) {
                  this.sendResume();
               } else {
                  this.sendIdentify();
               }
               break;
            case 11:
               this.heartbeatAcked = true;
         }
      }
   }

   private void handleDispatch(String type, JsonObject d) {
      switch (type) {
         case "READY":
            this.sessionId = d.has("session_id") ? d.get("session_id").getAsString() : null;
            if (d.has("resume_gateway_url")) {
               this.resumeGatewayUrl = d.get("resume_gateway_url").getAsString();
            }

            this.connected = true;
            String name = "?";

            try {
               name = d.getAsJsonObject("user").get("username").getAsString();
            } catch (Exception var7) {
            }

            EventWatcherClient.LOGGER.info("Discord gateway READY (logged in as {}).", name);
            break;
         case "RESUMED":
            this.connected = true;
            EventWatcherClient.LOGGER.info("Discord session resumed.");
            break;
         case "MESSAGE_CREATE":
            this.handleMessage(d);
      }
   }

   private void handleMessage(JsonObject d) {
      String channelId = d.has("channel_id") ? d.get("channel_id").getAsString() : "";
      String target = this.watch.channelId == null ? "" : this.watch.channelId.trim();
      if (!target.isEmpty() && target.equals(channelId)) {
         String text = MessageScanner.extractText(d);
         String matched = MessageScanner.matchKeyword(text, this.watch.keywords);
         if (matched != null) {
            EventWatcherClient.LOGGER.info("Keyword '{}' detected in monitored channel ({}).", matched, this.watch.describe());

            try {
               this.listener.onMatch(this.watch, text, matched);
            } catch (Exception var7) {
               EventWatcherClient.LOGGER.error("Keyword listener threw", var7);
            }
         }
      }
   }

   private void handleCloseCode(int code) {
      switch (code) {
         case 4004:
            EventWatcherClient.LOGGER
               .error("Discord rejected the token (4004 Authentication Failed). Check the BOT token in EventWatcher settings. Not retrying.");
            this.haltAfterFatalClose();
            break;
         case 4005:
         case 4006:
         case 4010:
         case 4011:
         case 4012:
         default:
            if (this.running) {
               this.scheduleReconnect(true);
            }
            break;
         case 4007:
         case 4009:
            this.sessionId = null;
            if (this.running) {
               this.scheduleReconnect(false);
            }
            break;
         case 4008:
            EventWatcherClient.LOGGER.warn("Discord rate-limited the gateway (4008). Backing off before reconnect.");
            if (this.running) {
               this.scheduleReconnect(true, RATE_LIMIT_RECONNECT_DELAY_MS);
            }
            break;
         case 4013:
            EventWatcherClient.LOGGER.error("Discord reported invalid intents (4013). Not retrying.");
            this.haltAfterFatalClose();
            break;
         case 4014:
            EventWatcherClient.LOGGER
               .error(
                  "Discord reported DISALLOWED intents (4014). Enable the 'MESSAGE CONTENT INTENT' for your bot in the Developer Portal (Bot tab). Not retrying."
               );
            this.haltAfterFatalClose();
      }
   }

   private void haltAfterFatalClose() {
      this.running = false;
      this.connected = false;
      this.cancelHeartbeat();
      this.webSocket = null;
   }

   private class Listener implements WebSocket.Listener {
      @Override
      public void onOpen(WebSocket ws) {
         DiscordGatewayClient.this.webSocket = ws;
         synchronized (DiscordGatewayClient.this.bufferLock) {
            DiscordGatewayClient.this.textBuffer.setLength(0);
         }

         ws.request(Long.MAX_VALUE);
         EventWatcherClient.LOGGER.info("Discord gateway socket opened.");
      }

      @Override
      public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
         if (ws == DiscordGatewayClient.this.webSocket) {
            String complete = null;
            synchronized (DiscordGatewayClient.this.bufferLock) {
               DiscordGatewayClient.this.textBuffer.append(data);
               if (last) {
                  complete = DiscordGatewayClient.this.textBuffer.toString();
                  DiscordGatewayClient.this.textBuffer.setLength(0);
               }
            }

            if (complete != null) {
               try {
                  DiscordGatewayClient.this.handlePayload(ws, complete);
               } catch (Exception var7) {
                  EventWatcherClient.LOGGER.error("Gateway payload handling failed", var7);
               }
            }
         }

         return null;
      }

      @Override
      public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
         if (ws == DiscordGatewayClient.this.webSocket) {
            DiscordGatewayClient.this.connected = false;
            EventWatcherClient.LOGGER.warn("Discord gateway closed: {} {}", statusCode, reason);
            DiscordGatewayClient.this.handleCloseCode(statusCode);
         }

         return null;
      }

      @Override
      public void onError(WebSocket ws, Throwable error) {
         if (ws == DiscordGatewayClient.this.webSocket) {
            DiscordGatewayClient.this.connected = false;
            EventWatcherClient.LOGGER.warn("Discord gateway error: {}", error.toString());
            if (DiscordGatewayClient.this.running) {
               DiscordGatewayClient.this.scheduleReconnect(true);
            }
         }
      }
   }
}
