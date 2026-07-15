package net.eventwatcher.sound;

import java.io.File;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent.Type;
import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.EventWatcherConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class NotificationSound {
   public static final List<String> MINECRAFT_SOUNDS = List.of(
      "block.note_block.pling",
      "block.note_block.bell",
      "block.bell.use",
      "entity.experience_orb.pickup",
      "entity.player.levelup",
      "ui.button.click",
      "block.amethyst_block.chime",
      "entity.arrow.hit_player",
      "entity.ender_eye.death"
   );

   private NotificationSound() {
   }

   public static void play(EventWatcherConfig config) {
      if (config != null && config.soundEnabled) {
         try {
            if ("file".equals(config.soundType) && config.soundFilePath != null && !config.soundFilePath.isBlank()) {
               playWav(config.soundFilePath.trim());
            } else {
               playMinecraft(config.minecraftSound);
            }
         } catch (Exception var2) {
            EventWatcherClient.LOGGER.warn("Failed to play notification sound: {}", var2.toString());
         }
      }
   }

   private static void playMinecraft(String soundId) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && soundId != null && !soundId.isBlank()) {
         Identifier id = soundId.contains(":") ? Identifier.tryParse(soundId) : Identifier.of("minecraft", soundId);
         if (id == null) {
            EventWatcherClient.LOGGER.warn("Invalid Minecraft sound id: {}", soundId);
         } else {
            SoundEvent event = SoundEvent.of(id);
            client.execute(() -> client.getSoundManager().play(PositionedSoundInstance.ui(event, 1.0F)));
         }
      }
   }

   private static void playWav(String path) {
      Thread t = new Thread(() -> {
         try {
            File file = new File(path);
            if (!file.isFile()) {
               EventWatcherClient.LOGGER.warn("Notification sound file not found: {}", path);
               return;
            }

            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            clip.addLineListener(ev -> {
               if (ev.getType() == Type.STOP) {
                  clip.close();

                  try {
                     stream.close();
                  } catch (Exception var4x) {
                  }
               }
            });
            clip.start();
         } catch (Exception var4) {
            EventWatcherClient.LOGGER.warn("Failed to play .wav '{}': {} — must be a standard PCM .wav file.", path, var4.toString());
         }
      }, "EventWatcher-Sound");
      t.setDaemon(true);
      t.start();
   }
}
