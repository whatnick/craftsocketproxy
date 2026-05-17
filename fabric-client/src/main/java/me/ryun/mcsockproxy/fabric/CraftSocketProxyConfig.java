package me.ryun.mcsockproxy.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class CraftSocketProxyConfig {
    private static final String CONFIG_FILE = "craftsocketproxy-client.properties";

    private CraftSocketProxyConfig() {
    }

    public static CraftSocketProxySettings load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        Properties properties = new Properties();
        if(Files.exists(path)) {
            try(InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch(IOException ignored) {
            }
        }

        return new CraftSocketProxySettings(
            properties.getProperty("host", CraftSocketProxySettings.DEFAULT_HOST),
            parseInt(properties.getProperty("port"), CraftSocketProxySettings.DEFAULT_PORT),
            properties.getProperty("path", CraftSocketProxySettings.DEFAULT_PATH),
            parseInt(properties.getProperty("localPort"), CraftSocketProxySettings.DEFAULT_LOCAL_PORT),
            ""
        );
    }

    public static void save(CraftSocketProxySettings settings) throws IOException {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        Files.createDirectories(path.getParent());

        Properties properties = new Properties();
        properties.setProperty("host", settings.host());
        properties.setProperty("port", Integer.toString(settings.port()));
        properties.setProperty("path", settings.path());
        properties.setProperty("localPort", Integer.toString(settings.localPort()));

        try(OutputStream output = Files.newOutputStream(path)) {
            properties.store(output, "CraftSocketProxy client settings. Passwords are intentionally not stored.");
        }
    }

    private static int parseInt(String value, int fallback) {
        if(value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(value);
        } catch(NumberFormatException ignored) {
            return fallback;
        }
    }
}
