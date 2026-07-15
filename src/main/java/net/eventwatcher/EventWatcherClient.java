package net.eventwatcher;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.connect.ServerConnector;
import net.eventwatcher.discord.DiscordGatewayClient;
import net.eventwatcher.discord.DiscordSource;
import net.eventwatcher.discord.DiscordUserPoller;
import net.eventwatcher.gui.EventWatcherSettingsScreen;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventWatcherClient implements ClientModInitializer {
   public static final String MODID = "eventwatcher";
   public static final Logger LOGGER = LoggerFactory.getLogger("eventwatcher");
   private static EventWatcherConfig config;
   @Nullable
   private static DiscordSource discord;
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
               (LiteralArgumentBuilder)ClientCommandManager.literal("ewjoin").executes(ctx -> {
                  ServerConnector.connectNow(getConfig().targetServer);
                  return 1;
               })
            )
         );
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, mc) -> mc.execute(ServerConnector::flushPendingChat));
      startDiscord();
      LOGGER.info("EventWatcher initialized (MC 1.21.11).");
   }

   public static void startDiscord() {
      stopDiscord();
      if (!config.isConfigured()) {
         LOGGER.warn("Discord token/channel not set — idle. Configure it in EventWatcher settings.");
      } else {
         discord = (DiscordSource)(config.useUserToken
            ? new DiscordUserPoller(config, ServerConnector::handleDetection)
            : new DiscordGatewayClient(config, ServerConnector::handleDetection));
         discord.start();
      }
   }

   public static void stopDiscord() {
      if (discord != null) {
         discord.stop();
         discord = null;
      }
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

   @Nullable
   public static DiscordSource getDiscord() {
      return discord;
   }
}
