package net.eventwatcher.gui;

import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.EventWatcherConfig;
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
   private TextFieldWidget channelField;
   private TextFieldWidget targetField;
   private TextFieldWidget addKeywordField;
   private KeywordListWidget keywordList;
   private ButtonWidget notifyButton;
   private ButtonWidget autoButton;
   private ButtonWidget tokenTypeButton;
   private int panelLeft;
   private int panelTop;
   private int panelWidth;
   private int panelHeight;
   private int col1;
   private int col2;
   private int col3;
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
      this.col3 = this.col2 + colWidth + pad;
      this.contentTop = this.panelTop + 40;
      this.tokenField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 10, colWidth, 18, Text.literal("Discord Token"));
      this.tokenField.setMaxLength(120);
      this.tokenField.setText(this.working.discordToken);
      this.tokenField.setChangedListener(s -> this.working.discordToken = s);
      this.tokenField.addFormatter((displayed, offset) -> OrderedText.styledForwardsVisitedString("*".repeat(displayed.length()), Style.EMPTY));
      this.addDrawableChild(this.tokenField);
      this.channelField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 48, colWidth, 18, Text.literal("Channel ID"));
      this.channelField.setMaxLength(40);
      this.channelField.setText(this.working.channelId);
      this.channelField.setChangedListener(s -> this.working.channelId = s.trim());
      this.addDrawableChild(this.channelField);
      this.tokenTypeButton = ButtonWidget.builder(this.tokenTypeLabel(), b -> {
         this.working.useUserToken = !this.working.useUserToken;
         this.tokenTypeButton.setMessage(this.tokenTypeLabel());
      }).dimensions(this.col1, this.contentTop + 84, colWidth, 18).build();
      this.addDrawableChild(this.tokenTypeButton);
      int listTop = this.contentTop + 10;
      int listBottom = this.panelTop + this.panelHeight - 56;
      this.keywordList = new KeywordListWidget(this.textRenderer, this.col2, listTop, colWidth, listBottom - listTop, this.working.keywords, this::removeKeyword);
      this.addDrawableChild(this.keywordList);
      this.addKeywordField = new TextFieldWidget(this.textRenderer, this.col2, listBottom + 4, colWidth - 44, 18, Text.literal("New keyword"));
      this.addKeywordField.setMaxLength(32);
      this.addDrawableChild(this.addKeywordField);
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Add"), b -> this.addKeyword())
            .dimensions(this.col2 + colWidth - 40, listBottom + 4, 40, 18)
            .build()
      );
      this.notifyButton = ButtonWidget.builder(Text.literal("Notify Me"), b -> {
         this.working.autoConnect = false;
         this.updateModeButtons();
      }).dimensions(this.col3, this.contentTop + 10, colWidth, 18).build();
      this.addDrawableChild(this.notifyButton);
      this.autoButton = ButtonWidget.builder(Text.literal("Auto-Connect"), b -> {
         this.working.autoConnect = true;
         this.updateModeButtons();
      }).dimensions(this.col3, this.contentTop + 32, colWidth, 18).build();
      this.addDrawableChild(this.autoButton);
      this.updateModeButtons();
      this.targetField = new TextFieldWidget(this.textRenderer, this.col3, this.contentTop + 78, colWidth, 18, Text.literal("Target Server"));
      this.targetField.setMaxLength(80);
      this.targetField.setText(this.working.targetServer);
      this.targetField.setChangedListener(s -> this.working.targetServer = s.trim());
      this.addDrawableChild(this.targetField);
      this.statusY = this.contentTop + 108;
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Sound..."), b -> this.client.setScreen(new SoundSettingsScreen(this, this.working)))
            .dimensions(this.col3, this.statusY + 16, colWidth, 18)
            .build()
      );
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

   private void updateModeButtons() {
      this.notifyButton.setMessage(Text.literal((this.working.autoConnect ? "( ) " : "(*) ") + "Notify Me"));
      this.autoButton.setMessage(Text.literal((this.working.autoConnect ? "(*) " : "( ) ") + "Auto-Connect"));
   }

   private void addKeyword() {
      String kw = this.addKeywordField.getText().trim();
      if (!kw.isEmpty() && !this.working.keywords.contains(kw)) {
         this.working.keywords.add(kw);
         this.addKeywordField.setText("");
      }
   }

   private void removeKeyword(String keyword) {
      this.working.keywords.remove(keyword);
   }

   private void save() {
      EventWatcherClient.setConfig(this.working);
      this.working.save();
      EventWatcherClient.restartDiscord();
      this.backToParent();
   }

   private void backToParent() {
      this.client.setScreen(this.parent);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, -1072689136);
      context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 1, -12961222);
      context.fill(this.panelLeft, this.panelTop + this.panelHeight - 1, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, -12961222);
      int sepTop = this.contentTop - 6;
      int sepBottom = this.panelTop + this.panelHeight - 32;
      context.fill(this.col2 - 6, sepTop, this.col2 - 5, sepBottom, 822083583);
      context.fill(this.col3 - 6, sepTop, this.col3 - 5, sepBottom, 822083583);
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, "EventWatcher Settings", this.width / 2, this.panelTop + 12, -1);
      int labelColor = -5197648;
      context.drawTextWithShadow(this.textRenderer, "Discord Token", this.col1, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Channel ID", this.col1, this.contentTop + 38, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Token Type", this.col1, this.contentTop + 74, labelColor);
      if (this.working.useUserToken) {
         context.drawTextWithShadow(this.textRenderer, "selfbot — use alt acct", this.col1, this.contentTop + 106, -2056144);
      }

      context.drawTextWithShadow(this.textRenderer, "Monitored Keywords", this.col2, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "On Keyword Detected:", this.col3, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Target Server", this.col3, this.contentTop + 68, labelColor);
      DiscordSource discord = EventWatcherClient.getDiscord();
      boolean connected = discord != null && discord.isConnected();
      int dotColor = connected ? -12264124 : -2276284;
      context.drawTextWithShadow(this.textRenderer, "Status:", this.col3, this.statusY, labelColor);
      int dotX = this.col3 + 46;
      context.fill(dotX, this.statusY - 1, dotX + 8, this.statusY + 7, dotColor);
      context.drawTextWithShadow(this.textRenderer, connected ? "Connected" : "Disconnected", dotX + 12, this.statusY, dotColor);
   }

   public void close() {
      this.backToParent();
   }
}
