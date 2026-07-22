package net.eventwatcher.mixin;

import java.util.List;
import net.eventwatcher.connect.ServerConnector;
import net.eventwatcher.gui.EventJoinWidget;
import net.eventwatcher.gui.EventWatcherSettingsScreen;
import net.eventwatcher.notify.EventNotifications;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class})
public class TitleScreenMixin {
   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void eventwatcher$addWidgets(CallbackInfo ci) {
      TitleScreen self = (TitleScreen)(Object)this;
      ScreenInvoker invoker = (ScreenInvoker)this;
      int w = self.width;
      ButtonWidget settings = ButtonWidget.builder(
            Text.literal("EventWatcher"), b -> MinecraftClient.getInstance().setScreen(new EventWatcherSettingsScreen(self))
         )
         .dimensions(6, 6, 114, 20)
         .build();
      invoker.eventwatcher$addDrawableChild(settings);
      List<EventNotifications.PendingEvent> pendingEvents = EventNotifications.all();
      int joinW = Math.min(220, w - 12);

      // Stack join alerts down the left side, below the settings button, so they clear the logo.
      for (int i = 0; i < pendingEvents.size(); i++) {
         EventNotifications.PendingEvent pending = pendingEvents.get(i);
         String server = pending.server();
         EventJoinWidget join = new EventJoinWidget(
            MinecraftClient.getInstance().textRenderer, 6, 30 + i * 24, joinW, 22, pending.serverName(), pending.authorName(), pending.authorId(), pending.avatarHash(), () -> {
               EventNotifications.remove(server);
               ServerConnector.connectNow(server);
            }
         );
         invoker.eventwatcher$addDrawableChild(join);
      }
   }
}
