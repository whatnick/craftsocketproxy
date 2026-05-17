# Client-side Mod Roadmap

This roadmap tracks a future Minecraft Java client mod that connects directly to CraftSocketProxy WebSocket servers without requiring players to run a separate local proxy process.

## Goals

- Let players add a WebSocket-backed server from the normal Multiplayer screen.
- Prompt for the server password in a Minecraft-native UI.
- Keep passwords out of command history, shell scripts, config files, and logs by default.
- Support common client loaders, starting with Fabric.
- Preserve compatibility with vanilla servers behind CraftSocketProxy.

## Proposed Architecture

1. Fabric client mod hooks the multiplayer connection flow.
2. A custom server entry stores the public WebSocket hostname, port, optional path, and display name.
3. When joining, the mod opens a WebSocket connection to CraftSocketProxy.
4. The mod sends `X-CraftSocketProxy-Password` during the WebSocket handshake.
5. Minecraft protocol bytes are bridged between the game client Netty channel and the WebSocket channel.
6. The normal Minecraft connection lifecycle handles disconnects, errors, and reconnect prompts.

## Milestones

### 1. Protocol Spike

- Create a minimal Fabric mod for the currently targeted Minecraft version. Initial scaffold lives in `fabric-client` and is enabled with `-PwithFabricClient=true`.
- Verify where to intercept outbound multiplayer connections.
- Prototype a Netty `ChannelDuplexHandler` that forwards Minecraft packets over WebSocket frames.
- Reuse CraftSocketProxy framing semantics: binary WebSocket frames carry raw Minecraft protocol bytes.

Current first slice:

- Adds a Fabric client module for Minecraft `1.21.1`.
- Adds an in-game CraftSocketProxy setup screen opened from an unbound keybind.
- Starts the existing authenticated CraftSocketProxy client from inside Minecraft.
- Saves non-secret host, port, path, and local port settings under Fabric config.
- Does not store the server password.

Build note: the module is opt-in so the existing standalone proxy build remains stable. Build it with:

```bash
./gradlew -PwithFabricClient=true :fabric-client:build
```

The Fabric build now works after removing the Foojay toolchain resolver from the settings plugin classpath. That resolver pulled Gson `2.9.1` ahead of Loom's Gson dependency and broke Minecraft manifest parsing.

### 2. Configuration UI

- Add a `WebSocket Server` button or edit screen option in Multiplayer.
- Capture hostname, port, path, and local display name.
- Prompt for password at connect time rather than storing it by default.
- Optionally support OS keychain storage later.

### 3. Authentication And Security

- Send the password in `X-CraftSocketProxy-Password` only during the WebSocket handshake.
- Redact password values from logs and crash reports.
- Add clear connection errors for `401 Unauthorized`, `403 Forbidden`, and tunnel failures.
- Document Cloudflare IP allowlists and password rotation.

### 4. Compatibility Matrix

- Test current Minecraft Java release.
- Test the homelab Paper server version.
- Decide whether to support older versions through separate mod builds or a compatibility layer.
- Test Windows, macOS, and Linux clients.

### 5. Packaging

- Publish Fabric builds from GitHub Actions.
- Generate Modrinth and GitHub Release artifacts.
- Include a signed checksum file for every build.
- Keep the standalone proxy scripts as the fallback path.

### 6. Player Experience

- Add a friendly connection wizard.
- Show a short status message when the WebSocket tunnel is connected.
- Make failures actionable: wrong password, IP not allowed, tunnel unreachable, backend server offline.
- Provide a short player guide for joining a password-protected CraftSocketProxy server.

## Open Questions

- Which Minecraft versions should the first Fabric build support?
- Should password storage be session-only, keychain-backed, or config-file based with explicit opt-in?
- Should the mod support Cloudflare Access service tokens in addition to the proxy password header?
- Should this remain Fabric-only initially, or should NeoForge be planned from the start?

## First Implementation Task

Create a `fabric-client` module in this repository with a minimal mod that adds a WebSocket-backed connection option and proves that raw Minecraft protocol bytes can be transported through CraftSocketProxy without a local proxy process.
