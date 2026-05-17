package me.ryun.mcsockproxy.fabric;

public record CraftSocketProxySettings(String host, int port, String path, int localPort, String password) {
    public static final String DEFAULT_HOST = "minecraft.example.com";
    public static final int DEFAULT_PORT = 80;
    public static final String DEFAULT_PATH = "/";
    public static final int DEFAULT_LOCAL_PORT = 25565;
}
