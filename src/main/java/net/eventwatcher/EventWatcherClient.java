package net.eventwatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.List;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.config.WatchConfig;
import net.eventwatcher.connect.ServerConnector;
import net.eventwatcher.discord.DiscordGatewayClient;
import net.eventwatcher.discord.DiscordSource;
import net.eventwatcher.discord.DiscordUserPoller;
import net.eventwatcher.gui.EventWatcherSettingsScreen;
import net.eventwatcher.notify.EventNotifications;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventWatcherClient implements ClientModInitializer {
   public static final String MODID = "eventwatcher";
   public static final Logger LOGGER = LoggerFactory.getLogger("eventwatcher");
   private static EventWatcherConfig config;
   private static final List<DiscordSource> sources = new ArrayList<>();
   private static KeyBinding openSettingsKey;

   public void onInitializeClient() {
      config = EventWatcherConfig.load();
      Category category = Category.create(Identifier.of("eventwatcher", "general"));
      openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.eventwatcher.open_settings", Type.KEYSYM, -1, category));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         while (openSettingsKey.wasPressed()) {
            client.setScreen(new EventWatcherSettingsScreen(client.currentScreen));
         }
      });
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register(
               ClientCommandManager.literal("ewjoin")
                  .executes(ctx -> {
                     ServerConnector.connectNow(defaultJoinTarget());
                     return 1;
                  })
                  .then(
                     ClientCommandManager.argument("server", StringArgumentType.greedyString())
                        .executes(ctx -> {
                           ServerConnector.connectNow(StringArgumentType.getString(ctx, "server"));
                           return 1;
                        })
                  )
            )
         );
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, mc) -> mc.execute(ServerConnector::flushPendingChat));
      startDiscord();
      LOGGER.info("EventWatcher initialized (MC 1.21.11).");
   }

   private static String defaultJoinTarget() {
      EventNotifications.PendingEvent pending = EventNotifications.get();
      if (pending != null && pending.server() != null && !pending.server().isBlank()) {
         return pending.server();
      }

      for (WatchConfig watch : config.watches) {
         if (watch.isConfigured()) {
            return watch.targetServer;
         }
      }

      return config.watches.isEmpty() ? "" : config.watches.get(0).targetServer;
   }

   public static void startDiscord() {
      stopDiscord();
      int started = 0;

      for (WatchConfig watch : config.watches) {
         if (watch.isConfigured()) {
            DiscordSource source = (DiscordSource)(watch.useUserToken
               ? new DiscordUserPoller(watch, ServerConnector::handleDetection)
               : new DiscordGatewayClient(watch, ServerConnector::handleDetection));
            sources.add(source);
            source.start();
            started++;
         }
      }

      if (started == 0) {
         LOGGER.warn("No watch has a Discord token/channel set — idle. Configure one in EventWatcher settings.");
      } else {
         LOGGER.info("Started {} Discord watch(es).", started);
      }
   }

   public static void stopDiscord() {
      for (DiscordSource source : sources) {
         source.stop();
      }

      sources.clear();
   }

   public static void restartDiscord() {
      startDiscord();
   }

   public static EventWatcherConfig getConfig() {
      return config;
   }

   public static void setConfig(EventWatcherConfig newConfig) {
      config = newConfig;
   }

   public static List<DiscordSource> getSources() {
      return List.copyOf(sources);
   }
}
