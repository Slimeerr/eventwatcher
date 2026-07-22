package net.eventwatcher.gui;

import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.config.EventWatcherConfig.ServerEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ServersScreen extends Screen {
   @Nullable
   private final Screen parent;
   private final EventWatcherConfig working;
   private int panelLeft;
   private int panelTop;
   private int panelWidth;
   private int panelHeight;

   public ServersScreen(@Nullable Screen parent, EventWatcherConfig working) {
      super(Text.literal("Watched Servers"));
      this.parent = parent;
      this.working = working;
   }

   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 360);
      this.panelHeight = Math.min(this.height - 20, 236);
      this.panelLeft = (this.width - this.panelWidth) / 2;
      this.panelTop = (this.height - this.panelHeight) / 2;
      int pad = 12;
      int x = this.panelLeft + pad;
      int w = this.panelWidth - pad * 2;
      int listTop = this.panelTop + 42;
      int listBottom = this.panelTop + this.panelHeight - 58;
      ServerListWidget list = new ServerListWidget(
         this.textRenderer, x, listTop, w, listBottom - listTop, this.working.servers, this::editServer, this::removeServer
      );
      this.addDrawableChild(list);
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Server"), b -> this.addServer()).dimensions(x, listBottom + 6, w, 20).build());
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Done"), b -> this.close()).dimensions(this.width / 2 - 75, this.panelTop + this.panelHeight - 26, 150, 20).build()
      );
   }

   private void addServer() {
      ServerEntry entry = new ServerEntry();
      this.working.servers.add(entry);
      this.client.setScreen(new ServerEditScreen(this, this.working, entry));
   }

   private void editServer(int index) {
      if (index >= 0 && index < this.working.servers.size()) {
         this.client.setScreen(new ServerEditScreen(this, this.working, this.working.servers.get(index)));
      }
   }

   private void removeServer(int index) {
      if (index >= 0 && index < this.working.servers.size()) {
         this.working.servers.remove(index);
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      GalaxyTheme.panel(context, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
      super.render(context, mouseX, mouseY, delta);
      GalaxyTheme.title(context, this.textRenderer, "Watched Servers", this.width / 2, this.panelTop + 12);

      java.util.List<String> dupes = this.working.duplicateKeywords();
      if (!dupes.isEmpty()) {
         String note = this.textRenderer.trimToWidth("Shared (first wins): " + String.join(", ", dupes), this.panelWidth - 20);
         context.drawCenteredTextWithShadow(this.textRenderer, note, this.width / 2, this.panelTop + 28, -2056144);
      } else if (this.working.servers.isEmpty()) {
         context.drawCenteredTextWithShadow(this.textRenderer, "No servers yet — add one below", this.width / 2, this.panelTop + this.panelHeight / 2, -7303024);
      }
   }

   public void close() {
      this.client.setScreen(this.parent);
   }
}
