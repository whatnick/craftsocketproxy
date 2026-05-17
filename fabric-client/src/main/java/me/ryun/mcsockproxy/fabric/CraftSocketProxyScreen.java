package me.ryun.mcsockproxy.fabric;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public final class CraftSocketProxyScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget hostField;
    private TextFieldWidget portField;
    private TextFieldWidget pathField;
    private TextFieldWidget localPortField;
    private TextFieldWidget passwordField;
    private ButtonWidget startButton;
    private ButtonWidget joinButton;
    private ButtonWidget stopButton;
    private Text statusText = Text.literal(EmbeddedProxySession.status());

    public CraftSocketProxyScreen(Screen parent) {
        super(Text.translatable("screen.craftsocketproxy.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CraftSocketProxySettings settings = CraftSocketProxyConfig.load();
        int fieldWidth = 220;
        int fieldHeight = 20;
        int left = width / 2 - fieldWidth / 2;
        int top = 48;

        hostField = textField(left, top, fieldWidth, fieldHeight, Text.translatable("screen.craftsocketproxy.host"), settings.host());
        top += 34;
        portField = textField(left, top, fieldWidth, fieldHeight, Text.translatable("screen.craftsocketproxy.port"), Integer.toString(settings.port()));
        top += 34;
        pathField = textField(left, top, fieldWidth, fieldHeight, Text.translatable("screen.craftsocketproxy.path"), settings.path());
        top += 34;
        localPortField = textField(left, top, fieldWidth, fieldHeight, Text.translatable("screen.craftsocketproxy.local_port"), Integer.toString(settings.localPort()));
        top += 34;
        passwordField = textField(left, top, fieldWidth, fieldHeight, Text.translatable("screen.craftsocketproxy.password"), "");
        passwordField.setMaxLength(256);
        passwordField.setRenderTextProvider((text, firstCharacterIndex) -> OrderedText.styledForwardsVisitedString("*".repeat(text.length()), Style.EMPTY));
        top += 32;

        startButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.craftsocketproxy.start"), button -> startProxy())
            .dimensions(left, top, 106, 20)
            .build());
        stopButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.craftsocketproxy.stop"), button -> {
            EmbeddedProxySession.stop();
            statusText = Text.translatable("message.craftsocketproxy.stopped");
            CraftSocketProxyClientMod.sendStatus(statusText);
        }).dimensions(left + 114, top, 106, 20).build());
        top += 28;
        joinButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.craftsocketproxy.connect"), button -> startProxyAndJoin())
            .dimensions(left, top, fieldWidth, 20)
            .build());
        top += 28;
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
            .dimensions(left, top, fieldWidth, 20)
            .build());
    }

    private TextFieldWidget textField(int left, int top, int width, int height, Text label, String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, left, top, width, height, label);
        field.setText(value);
        field.setMaxLength(256);
        addDrawableChild(field);
        return field;
    }

    private void startProxy() {
        CraftSocketProxySettings settings = readSettings();
        if(settings == null) {
            return;
        }

        try {
            CraftSocketProxyConfig.save(settings);
            int localPort = EmbeddedProxySession.start(settings);
            statusText = Text.translatable("message.craftsocketproxy.started", localPort);
            CraftSocketProxyClientMod.sendStatus(statusText);
        } catch(IOException | RuntimeException cause) {
            statusText = Text.translatable("message.craftsocketproxy.failed", cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
            CraftSocketProxyClientMod.sendStatus(statusText);
        }
    }

    private void startProxyAndJoin() {
        CraftSocketProxySettings settings = readSettings();
        if(settings == null || client == null) {
            return;
        }

        setBusy(true);
        statusText = Text.translatable("screen.craftsocketproxy.waiting");
        try {
            CraftSocketProxyConfig.save(settings);
        } catch(IOException cause) {
            statusText = Text.translatable("message.craftsocketproxy.failed", cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
            setBusy(false);
            return;
        }

        Thread connector = new Thread(() -> {
            try {
                int localPort = EmbeddedProxySession.startAndWait(settings, Duration.ofSeconds(12));
                client.execute(() -> joinLocalServer(localPort));
            } catch(IOException | InterruptedException | RuntimeException cause) {
                if(cause instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                client.execute(() -> {
                    statusText = Text.translatable("message.craftsocketproxy.failed", cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
                    CraftSocketProxyClientMod.sendStatus(statusText);
                    setBusy(false);
                });
            }
        }, "craftsocketproxy-login");
        connector.setDaemon(true);
        connector.start();
    }

    private void joinLocalServer(int localPort) {
        if(client == null) {
            return;
        }

        String address = "localhost:" + localPort;
        statusText = Text.translatable("message.craftsocketproxy.joining", localPort);
        CraftSocketProxyClientMod.sendStatus(statusText);
        ServerInfo serverInfo = new ServerInfo("CraftSocketProxy", address, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, client, ServerAddress.parse(address), serverInfo, false, new CookieStorage(Map.of()));
    }

    private void setBusy(boolean busy) {
        hostField.active = !busy;
        portField.active = !busy;
        pathField.active = !busy;
        localPortField.active = !busy;
        passwordField.active = !busy;
        startButton.active = !busy;
        joinButton.active = !busy;
        stopButton.active = !busy;
    }

    private CraftSocketProxySettings readSettings() {
        Integer port = parseInt(portField.getText());
        Integer localPort = parseInt(localPortField.getText());
        if(hostField.getText().isBlank() || port == null || localPort == null || pathField.getText().isBlank()) {
            statusText = Text.translatable("message.craftsocketproxy.invalid");
            CraftSocketProxyClientMod.sendStatus(statusText);
            return null;
        }

        return new CraftSocketProxySettings(
            hostField.getText().trim(),
            port,
            pathField.getText().trim(),
            localPort,
            passwordField.getText()
        );
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch(NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFF);
        drawLabel(context, hostField, Text.translatable("screen.craftsocketproxy.host"));
        drawLabel(context, portField, Text.translatable("screen.craftsocketproxy.port"));
        drawLabel(context, pathField, Text.translatable("screen.craftsocketproxy.path"));
        drawLabel(context, localPortField, Text.translatable("screen.craftsocketproxy.local_port"));
        drawLabel(context, passwordField, Text.translatable("screen.craftsocketproxy.password"));
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.craftsocketproxy.address", localPortField.getText()), width / 2, passwordField.getY() + 28, 0xC0C0C0);
        context.drawCenteredTextWithShadow(textRenderer, statusText, width / 2, height - 28, 0xA0FFA0);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLabel(DrawContext context, TextFieldWidget field, Text label) {
        context.drawTextWithShadow(textRenderer, label, field.getX(), field.getY() - 11, 0xA0A0A0);
    }

    @Override
    public void close() {
        if(client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
