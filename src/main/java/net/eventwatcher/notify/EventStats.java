package net.eventwatcher.notify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.eventwatcher.EventWatcherClient;
import net.fabricmc.loader.api.FabricLoader;

/** Small persistent tally of events caught, kept separate from the editable config. */
public final class EventStats {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static EventStats instance;

   public long eventsCaught = 0L;
   public long lastEventEpochMs = 0L;

   private static Path path() {
      return FabricLoader.getInstance().getConfigDir().resolve("eventwatcher-stats.json");
   }

   public static synchronized EventStats get() {
      if (instance == null) {
         instance = load();
      }

      return instance;
   }

   private static EventStats load() {
      Path p = path();
      if (Files.exists(p)) {
         try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            EventStats loaded = GSON.fromJson(r, EventStats.class);
            if (loaded != null) {
               return loaded;
            }
         } catch (Exception e) {
            EventWatcherClient.LOGGER.warn("Failed to read stats: {}", e.toString());
         }
      }

      return new EventStats();
   }

   public static synchronized void recordEvent() {
      EventStats s = get();
      s.eventsCaught++;
      s.lastEventEpochMs = System.currentTimeMillis();
      s.save();
   }

   private void save() {
      Path p = path();

      try {
         Files.createDirectories(p.getParent());

         try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
         }
      } catch (Exception e) {
         EventWatcherClient.LOGGER.warn("Failed to save stats: {}", e.toString());
      }
   }
}
