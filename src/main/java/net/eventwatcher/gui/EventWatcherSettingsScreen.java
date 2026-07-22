package net.eventwatcher.gui;

import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.config.EventWatcherConfig.ServerEntry;
import net.eventwatcher.discord.DiscordSource;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class EventWatcherSettingsScreen extends Screen {
   @Nullable
   private final Screen parent;
   private final EventWatcherConfig working;
   private TextFieldWidget tokenField;
   private ButtonWidget tokenTypeButton;
   private int panelLeft;
   private int panelTop;
   private int panelWidth;
   private int panelHeight;
   private int col1;
   private int col2;
   private int contentTop;
   private int statusY;

   public EventWatcherSettingsScreen(@Nullable Screen parent) {
      super(Text.literal("EventWatcher Settings"));
      this.parent = parent;
      this.working = EventWatcherClient.getConfig().copy();
   }

   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 470);
      this.panelHeight = Math.min(this.height - 20, 252);
      this.panelLeft = (this.width - this.panelWidth) / 2;
      this.panelTop = (this.height - this.panelHeight) / 2;
      int pad = 10;
      int colWidth = (this.panelWidth - pad * 4) / 3;
      this.col1 = this.panelLeft + pad;
      this.col2 = this.col1 + colWidth + pad;
      this.contentTop = this.panelTop + 40;
      this.tokenField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 10, colWidth, 18, Text.literal("Discord Token"));
      this.tokenField.setMaxLength(120);
      this.tokenField.setText(this.working.discordToken);
      this.tokenField.setChangedListener(s -> this.working.discordToken = s);
      this.tokenField.addFormatter((displayed, offset) -> OrderedText.styledForwardsVisitedString("*".repeat(displayed.length()), Style.EMPTY));
      this.addDrawableChild(this.tokenField);
      this.tokenTypeButton = ButtonWidget.builder(this.tokenTypeLabel(), b -> {
         this.working.useUserToken = !this.working.useUserToken;
         this.tokenTypeButton.setMessage(this.tokenTypeLabel());
      }).dimensions(this.col1, this.contentTop + 48, colWidth, 18).build();
      this.addDrawableChild(this.tokenTypeButton);
      int rightX = this.col2;
      int rightW = this.panelLeft + this.panelWidth - pad - rightX;
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Manage Servers..."), b -> this.client.setScreen(new ServersScreen(this, this.working)))
            .dimensions(rightX, this.contentTop + 22, rightW, 20)
            .build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Sound..."), b -> this.client.setScreen(new SoundSettingsScreen(this, this.working)))
            .dimensions(rightX, this.contentTop + 50, rightW, 20)
            .build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Test Alert"), b -> this.testAlert()).dimensions(rightX, this.contentTop + 78, rightW, 20).build()
      );
      this.statusY = this.contentTop + 108;
      int btnY = this.panelTop + this.panelHeight - 26;
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Save"), b -> this.save()).dimensions(this.width / 2 - 104, btnY, 100, 20).build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Cancel"), b -> this.backToParent()).dimensions(this.width / 2 + 4, btnY, 100, 20).build()
      );
   }

   private Text tokenTypeLabel() {
      return Text.literal(this.working.useUserToken ? "Type: User token" : "Type: Bot token");
   }

   private void testAlert() {
      for (ServerEntry entry : this.working.servers) {
         if (entry.address != null && !entry.address.isBlank()) {
            net.eventwatcher.connect.ServerConnector.simulate(this.working, entry);
            return;
         }
      }
   }

   private void save() {
      this.working.servers.removeIf(ServerEntry::isBlank);
      EventWatcherClient.setConfig(this.working);
      this.working.save();
      EventWatcherClient.restartDiscord();
      this.backToParent();
   }

   private void backToParent() {
      this.client.setScreen(this.parent);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      GalaxyTheme.panel(context, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
      GalaxyTheme.separator(context, this.col2 - 6, this.contentTop - 6, this.panelTop + this.panelHeight - 32);
      super.render(context, mouseX, mouseY, delta);
      GalaxyTheme.title(context, this.textRenderer, "EventWatcher Settings", this.width / 2, this.panelTop + 12);
      int labelColor = -5197648;
      context.drawTextWithShadow(this.textRenderer, "Discord Token", this.col1, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Token Type", this.col1, this.contentTop + 38, labelColor);
      if (this.working.useUserToken) {
         context.drawTextWithShadow(this.textRenderer, "selfbot — use alt acct", this.col1, this.contentTop + 72, -2056144);
      } else {
         context.drawTextWithShadow(this.textRenderer, "channels are set per server", this.col1, this.contentTop + 72, -7303024);
      }

      context.drawTextWithShadow(this.textRenderer, "Watched Servers", this.col2, this.contentTop, labelColor);
      DiscordSource discord = EventWatcherClient.getDiscord();
      boolean connected = discord != null && discord.isConnected();
      int dotColor = connected ? -12264124 : -2276284;
      context.drawTextWithShadow(this.textRenderer, "Status:", this.col2, this.statusY, labelColor);
      int dotX = this.col2 + 46;
      context.fill(dotX, this.statusY - 1, dotX + 8, this.statusY + 7, dotColor);
      context.drawTextWithShadow(this.textRenderer, connected ? "Connected" : "Disconnected", dotX + 12, this.statusY, dotColor);
      long caught = net.eventwatcher.notify.EventStats.get().eventsCaught;
      context.drawTextWithShadow(this.textRenderer, "Events caught: " + caught, this.col2, this.statusY + 14, -7303024);
   }

   public void close() {
      this.backToParent();
   }
}
