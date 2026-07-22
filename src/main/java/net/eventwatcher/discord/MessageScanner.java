package net.eventwatcher.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.eventwatcher.config.EventWatcherConfig.ServerEntry;
import org.jetbrains.annotations.Nullable;

public final class MessageScanner {
   private MessageScanner() {
   }

   public static String extractText(JsonObject message) {
      StringBuilder sb = new StringBuilder();
      if (message.has("content") && !message.get("content").isJsonNull()) {
         sb.append(message.get("content").getAsString());
      }

      if (message.has("embeds") && message.get("embeds").isJsonArray()) {
         for (JsonElement el : message.getAsJsonArray("embeds")) {
            if (el.isJsonObject()) {
               JsonObject embed = el.getAsJsonObject();
               appendIfPresent(sb, embed, "title");
               appendIfPresent(sb, embed, "description");
            }
         }
      }

      return sb.toString();
   }

   private static void appendIfPresent(StringBuilder sb, JsonObject obj, String key) {
      if (obj.has(key) && !obj.get(key).isJsonNull()) {
         sb.append(' ').append(obj.get(key).getAsString());
      }
   }

   /**
    * Finds the first configured server whose keyword list matches the message.
    * Servers are checked in order, and within a server its keywords are checked
    * in order, so earlier entries win ties.
    *
    * <p>A keyword prefixed with {@code !} is a negative keyword: if the message
    * contains it, that server is vetoed (e.g. {@code !ended} skips "event ended").
    * When a server has {@code wholeWord} on, keywords match only as whole words.
    */
   @Nullable
   public static ServerMatch matchServer(String content, String channelId, List<ServerEntry> servers) {
      if (content == null || content.isEmpty() || servers == null) {
         return null;
      }

      String lower = content.toLowerCase(Locale.ROOT);

      for (ServerEntry server : new ArrayList<>(servers)) {
         if (server == null || server.keywords == null || server.address == null || server.address.isBlank()) {
            continue;
         }

         // Only consider servers that watch the channel this message came from.
         if (channelId != null && !channelId.isBlank() && !server.channels().contains(channelId)) {
            continue;
         }

         List<String> raws = new ArrayList<>(server.keywords);
         boolean vetoed = false;

         for (String raw : raws) {
            if (raw != null && raw.startsWith("!")) {
               String neg = raw.substring(1).trim().toLowerCase(Locale.ROOT);
               if (!neg.isEmpty() && containsMatch(lower, neg, server.wholeWord)) {
                  vetoed = true;
                  break;
               }
            }
         }

         if (vetoed) {
            continue;
         }

         for (String raw : raws) {
            if (raw == null || raw.isBlank() || raw.startsWith("!")) {
               continue;
            }

            String kw = raw.trim().toLowerCase(Locale.ROOT);
            if (containsMatch(lower, kw, server.wholeWord)) {
               return new ServerMatch(server, raw.trim());
            }
         }
      }

      return null;
   }

   private static boolean containsMatch(String lowerText, String lowerKw, boolean wholeWord) {
      if (lowerKw.isEmpty()) {
         return false;
      }

      if (!wholeWord) {
         return lowerText.contains(lowerKw);
      }

      int from = 0;

      while (true) {
         int idx = lowerText.indexOf(lowerKw, from);
         if (idx < 0) {
            return false;
         }

         boolean leftOk = idx == 0 || !isWordChar(lowerText.charAt(idx - 1));
         int end = idx + lowerKw.length();
         boolean rightOk = end >= lowerText.length() || !isWordChar(lowerText.charAt(end));
         if (leftOk && rightOk) {
            return true;
         }

         from = idx + 1;
      }
   }

   private static boolean isWordChar(char c) {
      return Character.isLetterOrDigit(c) || c == '_';
   }

   /** Pulls the display name, id, and avatar hash from a Discord message's author. */
   public static Author extractAuthor(JsonObject message) {
      String name = "";
      String id = "";
      String avatar = "";
      if (message.has("author") && message.get("author").isJsonObject()) {
         JsonObject a = message.getAsJsonObject("author");
         if (a.has("global_name") && !a.get("global_name").isJsonNull()) {
            name = a.get("global_name").getAsString();
         }

         if ((name == null || name.isBlank()) && a.has("username") && !a.get("username").isJsonNull()) {
            name = a.get("username").getAsString();
         }

         if (a.has("id") && !a.get("id").isJsonNull()) {
            id = a.get("id").getAsString();
         }

         if (a.has("avatar") && !a.get("avatar").isJsonNull()) {
            avatar = a.get("avatar").getAsString();
         }
      }

      return new Author(name == null ? "" : name, id, avatar);
   }

   public record ServerMatch(ServerEntry server, String keyword) {
   }

   public record Author(String name, String id, String avatarHash) {
   }
}
