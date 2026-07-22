package net.eventwatcher.gui;

import java.util.List;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.sound.NotificationSound;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class SoundSettingsScreen extends Screen {
   @Nullable
   private final Screen parent;
   private final EventWatcherConfig working;
   private int panelLeft;
   private int panelTop;
   private int panelWidth;
   private int panelHeight;
   private ButtonWidget enabledButton;
   private ButtonWidget mcSoundButton;

   public SoundSettingsScreen(@Nullable Screen parent, EventWatcherConfig working) {
      super(Text.literal("Sound Settings"));
      this.parent = parent;
      this.working = working;
   }

   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 320);
      this.panelHeight = Math.min(this.height - 20, 200);
      this.panelLeft = (this.width - this.panelWidth) / 2;
      this.panelTop = (this.height - this.panelHeight) / 2;
      int w = this.panelWidth - 40;
      int x = (this.width - w) / 2;
      int y = this.panelTop + 34;
      this.enabledButton = ButtonWidget.builder(this.enabledLabel(), b -> {
         this.working.soundEnabled = !this.working.soundEnabled;
         this.enabledButton.setMessage(this.enabledLabel());
      }).dimensions(x, y, w, 20).build();
      this.addDrawableChild(this.enabledButton);
      this.addDrawableChild(ButtonWidget.builder(this.sourceLabel(), b -> {
         this.working.soundType = "file".equals(this.working.soundType) ? "minecraft" : "file";
         this.client.setScreen(new SoundSettingsScreen(this.parent, this.working));
      }).dimensions(x, y + 24, w, 20).build());
      if ("file".equals(this.working.soundType)) {
         TextFieldWidget fileField = new TextFieldWidget(this.textRenderer, x, y + 52, w, 20, Text.literal("WAV path"));
         fileField.setMaxLength(512);
         fileField.setText(this.working.soundFilePath);
         fileField.setChangedListener(s -> this.working.soundFilePath = s.trim());
         this.addDrawableChild(fileField);
      } else {
         this.mcSoundButton = ButtonWidget.builder(this.mcSoundLabel(), b -> this.cycleSound()).dimensions(x, y + 52, w, 20).build();
         this.addDrawableChild(this.mcSoundButton);
      }

      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Test sound"), b -> NotificationSound.play(this.working)).dimensions(x, y + 84, w, 20).build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Done"), b -> this.close())
            .dimensions(this.width / 2 - 75, this.panelTop + this.panelHeight - 26, 150, 20)
            .build()
      );
   }

   private void cycleSound() {
      List<String> list = NotificationSound.MINECRAFT_SOUNDS;
      int idx = list.indexOf(this.working.minecraftSound);
      idx = (idx + 1) % list.size();
      this.working.minecraftSound = list.get(idx);
      this.mcSoundButton.setMessage(this.mcSoundLabel());
      NotificationSound.play(this.working);
   }

   private Text enabledLabel() {
      return Text.literal(this.working.soundEnabled ? "Sound: ON" : "Sound: OFF");
   }

   private Text sourceLabel() {
      return Text.literal("file".equals(this.working.soundType) ? "Source: Custom .wav file" : "Source: Minecraft sound");
   }

   private Text mcSoundLabel() {
      return Text.literal("Sound: " + this.working.minecraftSound);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      GalaxyTheme.panel(context, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
      super.render(context, mouseX, mouseY, delta);
      GalaxyTheme.title(context, this.textRenderer, "Sound Settings", this.width / 2, this.panelTop + 12);
      if ("file".equals(this.working.soundType)) {
         context.drawCenteredTextWithShadow(this.textRenderer, "Absolute path to a .wav file", this.width / 2, this.panelTop + this.panelHeight - 44, -7303024);
      }
   }

   public void close() {
      this.client.setScreen(this.parent);
   }
}
