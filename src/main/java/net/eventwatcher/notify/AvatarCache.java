package net.eventwatcher.notify;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.eventwatcher.EventWatcherClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Downloads Discord author avatars and registers them as Minecraft textures so the
 * Title Screen can draw the pfp next to a detected event. Avatars are requested at
 * 64x64. Loading is lazy and best-effort: any failure just means no image is shown.
 */
public final class AvatarCache {
   private static final int SIZE = 64;
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
   private static final Map<String, Identifier> loaded = new ConcurrentHashMap<>();
   private static final Set<String> inFlight = ConcurrentHashMap.newKeySet();

   private AvatarCache() {
   }

   /** Pixel size of the square avatar texture (matches the requested Discord size). */
   public static int size() {
      return SIZE;
   }

   /**
    * Returns the texture id for this author's avatar, or null if it isn't ready yet
    * (in which case a background load is kicked off). Draw nothing when null.
    */
   @Nullable
   public static Identifier get(String userId, String avatarHash) {
      if (userId == null || userId.isBlank() || avatarHash == null || avatarHash.isBlank()) {
         return null;
      }

      String key = sanitize(userId + "_" + avatarHash);
      Identifier existing = loaded.get(key);
      if (existing != null) {
         return existing;
      }

      if (inFlight.add(key)) {
         startLoad(userId, avatarHash, key);
      }

      return null;
   }

   private static void startLoad(String userId, String avatarHash, String key) {
      String url = "https://cdn.discordapp.com/avatars/" + userId + "/" + avatarHash + ".png?size=" + SIZE;
      CompletableFuture.runAsync(() -> {
         try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10L)).GET().build();
            HttpResponse<byte[]> resp = HTTP.send(req, BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200) {
               byte[] bytes = resp.body();
               MinecraftClient client = MinecraftClient.getInstance();
               client.execute(() -> register(client, key, bytes));
            } else {
               inFlight.remove(key);
               EventWatcherClient.LOGGER.warn("Avatar fetch returned HTTP {} for {}", resp.statusCode(), userId);
            }
         } catch (Exception e) {
            inFlight.remove(key);
            EventWatcherClient.LOGGER.warn("Avatar fetch failed for {}: {}", userId, e.toString());
         }
      });
   }

   private static void register(MinecraftClient client, String key, byte[] bytes) {
      try {
         NativeImage image = NativeImage.read(bytes);
         Identifier id = Identifier.of("eventwatcher", "avatar/" + key);
         client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "eventwatcher-avatar-" + key, image));
         loaded.put(key, id);
      } catch (Exception e) {
         EventWatcherClient.LOGGER.warn("Avatar decode/register failed: {}", e.toString());
      } finally {
         inFlight.remove(key);
      }
   }

   private static String sanitize(String raw) {
      String lower = raw.toLowerCase(Locale.ROOT);
      StringBuilder sb = new StringBuilder(lower.length());

      for (int i = 0; i < lower.length(); i++) {
         char c = lower.charAt(i);
         sb.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' ? c : '_');
      }

      return sb.toString();
   }
}
