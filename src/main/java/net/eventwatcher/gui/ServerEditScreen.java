package net.eventwatcher.gui;

import java.util.ArrayList;
import java.util.List;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.config.EventWatcherConfig.ServerEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ServerEditScreen extends Screen {
   private static final int[] COOLDOWNS = {0, 5, 15, 30, 60, 120};
   @Nullable
   private final Screen parent;
   private final EventWatcherConfig working;
   private final ServerEntry entry;
   private TextFieldWidget nameField;
   private TextFieldWidget addressField;
   private TextFieldWidget channelField;
   private TextFieldWidget addKeywordField;
   private KeywordListWidget keywordList;
   private ButtonWidget modeButton;
   private ButtonWidget wholeWordButton;
   private ButtonWidget cooldownButton;
   private ButtonWidget retryButton;
   private int panelLeft;
   private int panelTop;
   private int panelWidth;
   private int panelHeight;
   private int col1;
   private int col2;
   private int contentTop;

   public ServerEditScreen(@Nullable Screen parent, EventWatcherConfig working, ServerEntry entry) {
      super(Text.literal("Edit Server"));
      this.parent = parent;
      this.working = working;
      this.entry = entry;
   }

   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 470);
      this.panelHeight = Math.min(this.height - 20, 262);
      this.panelLeft = (this.width - this.panelWidth) / 2;
      this.panelTop = (this.height - this.panelHeight) / 2;
      int pad = 10;
      int colWidth = (this.panelWidth - pad * 3) / 2;
      this.col1 = this.panelLeft + pad;
      this.col2 = this.col1 + colWidth + pad;
      this.contentTop = this.panelTop + 30;
      this.nameField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 10, colWidth, 18, Text.literal("Name"));
      this.nameField.setMaxLength(40);
      this.nameField.setText(this.entry.name);
      this.nameField.setChangedListener(s -> this.entry.name = s);
      this.addDrawableChild(this.nameField);
      this.addressField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 48, colWidth, 18, Text.literal("Server Address"));
      this.addressField.setMaxLength(80);
      this.addressField.setText(this.entry.address);
      this.addressField.setChangedListener(s -> this.entry.address = s.trim());
      this.addDrawableChild(this.addressField);
      this.channelField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 86, colWidth, 18, Text.literal("Channel ID(s)"));
      this.channelField.setMaxLength(200);
      this.channelField.setText(String.join(", ", this.entry.channels()));
      this.channelField.setChangedListener(s -> this.entry.channelIds = parseChannels(s));
      this.addDrawableChild(this.channelField);
      int halfW = (colWidth - 4) / 2;
      int rightHalf = this.col1 + halfW + 4;
      this.modeButton = ButtonWidget.builder(this.modeLabel(), b -> {
         this.entry.autoConnect = !this.entry.autoConnect;
         this.modeButton.setMessage(this.modeLabel());
      }).dimensions(this.col1, this.contentTop + 112, halfW, 18).tooltip(Tooltip.of(Text.literal(
         "Notify Me: shows a join button + sound.\nAuto-Connect: joins the server automatically."
      ))).build();
      this.addDrawableChild(this.modeButton);
      this.wholeWordButton = ButtonWidget.builder(this.wholeWordLabel(), b -> {
         this.entry.wholeWord = !this.entry.wholeWord;
         this.wholeWordButton.setMessage(this.wholeWordLabel());
      }).dimensions(rightHalf, this.contentTop + 112, halfW, 18).tooltip(Tooltip.of(Text.literal(
         "ON: match keywords as whole words only,\nso \"event\" won't fire on \"prevent\"."
      ))).build();
      this.addDrawableChild(this.wholeWordButton);
      this.cooldownButton = ButtonWidget.builder(this.cooldownLabel(), b -> {
         this.entry.cooldownSeconds = nextCooldown(this.entry.cooldownSeconds);
         this.cooldownButton.setMessage(this.cooldownLabel());
      }).dimensions(this.col1, this.contentTop + 134, halfW, 18).tooltip(Tooltip.of(Text.literal(
         "After firing, ignore repeat matches for this\nserver for this many seconds. Stops spam."
      ))).build();
      this.addDrawableChild(this.cooldownButton);
      this.retryButton = ButtonWidget.builder(this.retryLabel(), b -> {
         this.entry.retryJoin = !this.entry.retryJoin;
         this.retryButton.setMessage(this.retryLabel());
      }).dimensions(rightHalf, this.contentTop + 134, halfW, 18).tooltip(Tooltip.of(Text.literal(
         "Auto-Connect only. If the server is full or\nqueued, keep retrying until you get in."
      ))).build();
      this.addDrawableChild(this.retryButton);
      int listTop = this.contentTop + 10;
      int listBottom = this.panelTop + this.panelHeight - 56;
      this.keywordList = new KeywordListWidget(this.textRenderer, this.col2, listTop, colWidth, listBottom - listTop, this.entry.keywords, this::removeKeyword);
      this.addDrawableChild(this.keywordList);
      this.addKeywordField = new TextFieldWidget(this.textRenderer, this.col2, listBottom + 4, colWidth - 44, 18, Text.literal("New keyword"));
      this.addKeywordField.setMaxLength(48);
      this.addDrawableChild(this.addKeywordField);
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Add"), b -> this.addKeyword()).dimensions(this.col2 + colWidth - 40, listBottom + 4, 40, 18).build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Done"), b -> this.close()).dimensions(this.width / 2 - 75, this.panelTop + this.panelHeight - 26, 150, 20).build()
      );
   }

   private static List<String> parseChannels(String raw) {
      List<String> out = new ArrayList<>();
      if (raw != null) {
         for (String part : raw.split("[\\s,]+")) {
            String t = part.trim();
            if (!t.isEmpty() && !out.contains(t)) {
               out.add(t);
            }
         }
      }

      return out;
   }

   private Text modeLabel() {
      return Text.literal(this.entry.autoConnect ? "Auto-Connect" : "Notify Me");
   }

   private Text wholeWordLabel() {
      return Text.literal(this.entry.wholeWord ? "Word: ON" : "Word: OFF");
   }

   private Text cooldownLabel() {
      return Text.literal(this.entry.cooldownSeconds <= 0 ? "CD: off" : "CD: " + this.entry.cooldownSeconds + "s");
   }

   private Text retryLabel() {
      return Text.literal(this.entry.retryJoin ? "Retry: ON" : "Retry: OFF");
   }

   private static int nextCooldown(int current) {
      for (int c : COOLDOWNS) {
         if (c > current) {
            return c;
         }
      }

      return 0;
   }

   private void addKeyword() {
      String kw = this.addKeywordField.getText().trim();
      if (!kw.isEmpty() && !this.entry.keywords.contains(kw)) {
         this.entry.keywords.add(kw);
         this.addKeywordField.setText("");
      }
   }

   private void removeKeyword(String keyword) {
      this.entry.keywords.remove(keyword);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      GalaxyTheme.panel(context, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
      GalaxyTheme.separator(context, this.col2 - 6, this.contentTop - 6, this.panelTop + this.panelHeight - 32);
      super.render(context, mouseX, mouseY, delta);
      GalaxyTheme.title(context, this.textRenderer, "Edit Server", this.width / 2, this.panelTop + 10);
      int labelColor = -5197648;
      context.drawTextWithShadow(this.textRenderer, "Name (optional)", this.col1, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Server Address", this.col1, this.contentTop + 38, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Channel ID(s) — comma separated", this.col1, this.contentTop + 76, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Keywords for this server", this.col2, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Tip: prefix with ! to ignore (e.g. !ended)", this.col2, this.panelTop + this.panelHeight - 32, -7303024);
   }

   public void close() {
      this.client.setScreen(this.parent);
   }
}
