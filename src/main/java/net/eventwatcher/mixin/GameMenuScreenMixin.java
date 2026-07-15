package net.eventwatcher.mixin;

import net.eventwatcher.gui.EventWatcherSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameMenuScreen.class})
public class GameMenuScreenMixin {
   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void eventwatcher$addButton(CallbackInfo ci) {
      GameMenuScreen self = (GameMenuScreen)(Object)this;
      ScreenInvoker invoker = (ScreenInvoker)this;
      ButtonWidget button = ButtonWidget.builder(
            Text.literal("EventWatcher"), b -> MinecraftClient.getInstance().setScreen(new EventWatcherSettingsScreen(self))
         )
         .dimensions(self.width - 120, 6, 114, 20)
         .build();
      invoker.eventwatcher$addDrawableChild(button);
   }
}
