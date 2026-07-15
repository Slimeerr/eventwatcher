package net.eventwatcher.connect;

import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.notify.EventNotifications;
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
   @Nullable
   private static volatile Text pendingChat;

   private ServerConnector() {
   }

   public static void handleDetection(String messageContent, String keyword) {
      MinecraftClient client = MinecraftClient.getInstance();
      EventWatcherConfig config = EventWatcherClient.getConfig();
      String server = config.targetServer;
      Text chatLine = buildChatMessage(messageContent, keyword);
      client.execute(
         () -> {
            pendingChat = null;
            if (isAlreadyConnectedTo(client, server)) {
               EventNotifications.clear();
               if (client.player != null) {
                  client.player.sendMessage(chatLine, false);
               }

               EventWatcherClient.LOGGER.info("Keyword matched but already on {} — not rejoining.", server);
            } else {
               NotificationSound.play(config);
               if (config.autoConnect) {
                  pendingChat = chatLine;
                  connectNow(server);
               } else {
                  String message = "Event detected! Click to join " + server;
                  EventNotifications.set(message, server);
                  SystemToast.show(
                     client.getToastManager(),
                     Type.PERIODIC_NOTIFICATION,
                     Text.literal("EventWatcher"),
                     Text.literal("Event '" + keyword + "' detected — join " + server)
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

   private static Text buildChatMessage(String content, String keyword) {
      String body = content != null && !content.isBlank() ? content.strip() : "(matched keyword: " + keyword + ")";
      if (body.length() > 256) {
         body = body.substring(0, 256) + "...";
      }

      MutableText prefix = Text.literal("[EventWatcher] ").formatted(Formatting.GRAY);
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
         EventNotifications.clear();
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
