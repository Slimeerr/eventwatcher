package net.eventwatcher.mixin;

import net.eventwatcher.connect.ServerConnector;
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
      EventNotifications.PendingEvent pending = EventNotifications.get();
      if (pending != null) {
         int joinW = Math.min(300, w - 12);
         ButtonWidget join = ButtonWidget.builder(Text.literal("Event detected! Join " + pending.server()), b -> {
            String server = pending.server();
            EventNotifications.clear();
            ServerConnector.connectNow(server);
         }).dimensions((w - joinW) / 2, 4, joinW, 20).build();
         invoker.eventwatcher$addDrawableChild(join);
      }
   }
}
