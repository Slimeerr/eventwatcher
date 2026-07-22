package net.eventwatcher.connect;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.discord.DiscordSource.DetectedMessage;
import net.eventwatcher.notify.EventNotifications;
import net.eventwatcher.notify.EventStats;
import net.eventwatcher.sound.NotificationSound;
import net.eventwatcher.util.GradientText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ResourcePackPolicy;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.SystemToast.Type;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public final class ServerConnector {
   private static final int MAX_RETRIES = 10;
   private static final long RETRY_DELAY_MS = 3000L;
   @Nullable
   private static volatile Text pendingChat;
   private static final Map<String, Long> lastFiredByServer = new ConcurrentHashMap<>();
   private static final Set<String> activeRetries = ConcurrentHashMap.newKeySet();
   private static final ScheduledExecutorService RETRY = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "EventWatcher-Retry");
      t.setDaemon(true);
      return t;
   });

   private ServerConnector() {
   }

   public static void handleDetection(DetectedMessage msg, EventWatcherConfig.ServerEntry entry) {
      String cooldownKey = entry.displayName();
      if (onCooldown(cooldownKey, entry.cooldownSeconds)) {
         EventWatcherClient.LOGGER.info("Match for {} ignored — still within {}s cooldown.", cooldownKey, entry.cooldownSeconds);
         return;
      }

      String server = entry.address;
      if (server == null || server.isBlank()) {
         return;
      }

      EventStats.recordEvent();
      MinecraftClient client = MinecraftClient.getInstance();
      EventWatcherConfig config = EventWatcherClient.getConfig();
      boolean autoConnect = entry.autoConnect;
      boolean retry = entry.retryJoin;
      String author = msg.authorName();
      Text chatLine = buildChatMessage(msg.content(), msg.keyword(), author);
      client.execute(
         () -> {
            pendingChat = null;
            if (isAlreadyConnectedTo(client, server)) {
               EventNotifications.remove(server);
               if (client.player != null) {
                  client.player.sendMessage(chatLine, false);
               }

               EventWatcherClient.LOGGER.info("Keyword matched but already on {} — not rejoining.", server);
            } else {
               NotificationSound.play(config);
               if (autoConnect) {
                  pendingChat = chatLine;
                  if (retry) {
                     connectWithRetry(server);
                  } else {
                     connectNow(server);
                  }
               } else {
                  String who = author != null && !author.isBlank() ? author : "Someone";
                  String serverName = entry.displayName();
                  EventNotifications.add(serverName, server, author, msg.authorId(), msg.avatarHash());
                  SystemToast.show(
                     client.getToastManager(),
                     Type.PERIODIC_NOTIFICATION,
                     Text.literal("EventWatcher — " + serverName),
                     Text.literal(who + " · '" + msg.keyword() + "' · join " + server)
                  );
                  if (client.world != null && client.player != null) {
                     client.player.sendMessage(chatLine, false);
                     Text link = Text.literal("[Click to join " + server + "]")
                        .styled(s -> s.withClickEvent(new RunCommand("/ewjoin")).withColor(Formatting.GREEN));
                     client.player.sendMessage(Text.literal("[EventWatcher] ").formatted(Formatting.GRAY).append(link), false);
                  } else if (client.currentScreen instanceof TitleScreen) {
                     pendingChat = chatLine;
                     client.setScreen(new TitleScreen());
                  }
               }
            }
         }
      );
   }

   private static boolean onCooldown(String server, int cooldownSeconds) {
      if (cooldownSeconds <= 0 || server == null || server.isBlank()) {
         return false;
      }

      String key = server.toLowerCase(Locale.ROOT);
      long now = System.currentTimeMillis();
      Long last = lastFiredByServer.get(key);
      if (last != null && now - last < cooldownSeconds * 1000L) {
         return true;
      }

      lastFiredByServer.put(key, now);
      return false;
   }

   /** Fires a fake alert for a server so the user can test sound + notification without a real event. */
   public static void simulate(EventWatcherConfig config, EventWatcherConfig.ServerEntry entry) {
      MinecraftClient client = MinecraftClient.getInstance();
      String serverName = entry.displayName();
      String target = entry.address == null || entry.address.isBlank() ? "example.net" : entry.address;
      client.execute(
         () -> {
            NotificationSound.play(config);
            EventNotifications.add(serverName, target, "EventWatcher", "", "");
            SystemToast.show(
               client.getToastManager(),
               Type.PERIODIC_NOTIFICATION,
               Text.literal("EventWatcher — " + serverName),
               Text.literal("Test alert · join " + target)
            );
            if (client.world != null && client.player != null) {
               client.player.sendMessage(buildChatMessage("This is a test alert.", "test", "EventWatcher"), false);
            }
         }
      );
   }

   private static Text buildChatMessage(String content, String keyword, String author) {
      String body = content != null && !content.isBlank() ? content.strip() : "(matched keyword: " + keyword + ")";
      if (body.length() > 256) {
         body = body.substring(0, 256) + "...";
      }

      MutableText prefix = Text.literal("[EventWatcher] ").formatted(Formatting.GRAY);
      if (author != null && !author.isBlank()) {
         prefix.append(Text.literal(author + ": ").formatted(Formatting.AQUA));
      }

      return prefix.append(GradientText.of(body));
   }

   public static void flushPendingChat() {
      MinecraftClient client = MinecraftClient.getInstance();
      Text msg = pendingChat;
      pendingChat = null;
      if (msg != null && client.player != null) {
         client.player.sendMessage(msg, false);
      }
   }

   public static void connectNow(String serverAddress) {
      MinecraftClient client = MinecraftClient.getInstance();
      client.execute(() -> {
         EventNotifications.remove(serverAddress);
         if (isAlreadyConnectedTo(client, serverAddress)) {
            EventWatcherClient.LOGGER.info("Already connected to {} — not rejoining.", serverAddress);
         } else {
            boolean wasInWorld = client.world != null;
            if (wasInWorld) {
               client.disconnectWithProgressScreen();
            }

            ServerAddress address = ServerAddress.parse(serverAddress);
            ServerInfo info = new ServerInfo("EventWatcher Target", serverAddress, ServerType.OTHER);
            info.setResourcePackPolicy(ResourcePackPolicy.ENABLED);
            Screen parent = (Screen)(!wasInWorld && client.currentScreen != null ? client.currentScreen : new TitleScreen());
            EventWatcherClient.LOGGER.info("Connecting to {} ...", serverAddress);
            ConnectScreen.connect(parent, client, address, info, false, null);
         }
      });
   }

   /** Connects, then keeps re-trying while you're stuck at a menu (server full/queued) until you get in. */
   public static void connectWithRetry(String serverAddress) {
      connectNow(serverAddress);
      String key = serverAddress.toLowerCase(Locale.ROOT);
      if (activeRetries.add(key)) {
         scheduleRetry(serverAddress, key, 1);
      }
   }

   private static void scheduleRetry(String serverAddress, String key, int attempt) {
      RETRY.schedule(
         () -> MinecraftClient.getInstance().execute(() -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (isAlreadyConnectedTo(client, serverAddress)) {
               activeRetries.remove(key);
               EventWatcherClient.LOGGER.info("Joined {} after {} attempt(s).", serverAddress, attempt);
               return;
            }

            // A connect is still in flight — wait, don't interrupt it.
            if (client.currentScreen instanceof ConnectScreen) {
               scheduleRetry(serverAddress, key, attempt);
               return;
            }

            // The player joined a different world/server — stop chasing this one.
            if (client.world != null) {
               activeRetries.remove(key);
               return;
            }

            if (attempt >= MAX_RETRIES) {
               activeRetries.remove(key);
               EventWatcherClient.LOGGER.info("Gave up joining {} after {} attempts.", serverAddress, attempt);
               return;
            }

            EventWatcherClient.LOGGER.info("Retry {}/{} joining {} ...", attempt + 1, MAX_RETRIES, serverAddress);
            connectNow(serverAddress);
            scheduleRetry(serverAddress, key, attempt + 1);
         }),
         RETRY_DELAY_MS,
         TimeUnit.MILLISECONDS
      );
   }

   private static boolean isAlreadyConnectedTo(MinecraftClient client, String target) {
      if (client.world == null) {
         return false;
      } else {
         ServerInfo current = client.getCurrentServerEntry();
         if (current != null && current.address != null) {
            try {
               ServerAddress a = ServerAddress.parse(current.address);
               ServerAddress b = ServerAddress.parse(target);
               return a.getAddress().equalsIgnoreCase(b.getAddress()) && a.getPort() == b.getPort();
            } catch (Exception var5) {
               return current.address.equalsIgnoreCase(target);
            }
         } else {
            return false;
         }
      }
   }
}
