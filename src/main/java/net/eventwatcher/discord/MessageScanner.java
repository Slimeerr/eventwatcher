package net.eventwatcher.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

   @Nullable
   public static String matchKeyword(String content, List<String> keywords) {
      if (content != null && !content.isEmpty() && keywords != null) {
         String lower = content.toLowerCase(Locale.ROOT);

         for (String kw : new ArrayList<>(keywords)) {
            if (kw != null && !kw.isBlank() && lower.contains(kw.toLowerCase(Locale.ROOT))) {
               return kw;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
