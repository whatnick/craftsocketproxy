package me.ryun.mcsockproxy.fabric;

import me.ryun.mcsockproxy.client.ProxyClient;
import me.ryun.mcsockproxy.common.CraftConnectionConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

public final class EmbeddedProxySession {
    private static final Object LOCK = new Object();
    private static ProxyClient proxyClient;
    private static volatile String status = "Stopped";

    private EmbeddedProxySession() {
    }

    public static String status() {
        return status;
    }

    public static int start(CraftSocketProxySettings settings) {
        validate(settings);

        synchronized(LOCK) {
            if(proxyClient != null) {
                proxyClient.shutdown();
            }

            CraftConnectionConfiguration configuration = new CraftConnectionConfiguration(
                settings.localPort(),
                settings.host().trim(),
                settings.port()
            );
            String password = settings.password().isBlank() ? null : settings.password();
            proxyClient = ProxyClient.Companion.serveAsync(configuration, settings.path().trim(), password);
            status = "Running on localhost:" + settings.localPort();
        }

        return settings.localPort();
    }

    public static int startAndWait(CraftSocketProxySettings settings, Duration timeout) throws IOException, InterruptedException {
        int localPort = start(settings);
        long deadline = System.nanoTime() + timeout.toNanos();
        IOException lastFailure = null;

        while(System.nanoTime() < deadline) {
            try(Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", localPort), 250);
                return localPort;
            } catch(IOException failure) {
                lastFailure = failure;
                Thread.sleep(100L);
            }
        }

        if(lastFailure != null) {
            throw lastFailure;
        }
        throw new IOException("Timed out waiting for localhost:" + localPort);
    }

    public static void stop() {
        synchronized(LOCK) {
            if(proxyClient != null) {
                proxyClient.shutdown();
            }
            proxyClient = null;
            status = "Stopped";
        }
    }

    private static void validate(CraftSocketProxySettings settings) {
        if(settings.host().isBlank()) {
            throw new IllegalArgumentException("Host is required.");
        }
        if(settings.port() < 1 || settings.port() > 65535) {
            throw new IllegalArgumentException("WebSocket port must be between 1 and 65535.");
        }
        if(settings.localPort() < 1 || settings.localPort() > 65535) {
            throw new IllegalArgumentException("Local port must be between 1 and 65535.");
        }
        if(!settings.path().startsWith("/")) {
            throw new IllegalArgumentException("Path must start with /." );
        }
    }
}
