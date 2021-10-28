package com.resourcefulbees.server;

import com.google.gson.JsonSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.ping.ResponseData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

public class Server {

    public static final MinecraftServer VERIFICATION_SERVER = MinecraftServer.init();
    public static final String UUID_REPLACE_STRING = "${uuid}";

    public static void main(String[] args) {
        Config.read();

        if (Config.getUrl().isEmpty()) throw new IllegalArgumentException("Auth URL is missing!");

        MinecraftServer.getExtensionManager().setLoadOnStartup(false);
        MojangAuth.init();

        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();

        eventHandler.addListener(AsyncPlayerPreLoginEvent.class, Server::onLogin);
        eventHandler.addListener(ServerListPingEvent.class, Server::onPing);

        VERIFICATION_SERVER.start("0.0.0.0", Integer.parseInt(args[0]));
    }

    private static void onPing(ServerListPingEvent event) {
        ResponseData responseData = event.getResponseData();
        responseData.setPlayersHidden(true);
        Config.getServerDescription().ifPresent(desc -> responseData.setDescription(Component.text(desc)));
        Config.getFavicon().ifPresent(favicon -> responseData.setFavicon("data:image/png;base64,"+favicon));
    }

    private static void onLogin(AsyncPlayerPreLoginEvent event) {
        String uuid = event.getPlayerUuid().toString();

        Optional<String> url = Config.getUrl();

        if (url.isPresent()) {
            try {
                String response = getAuthResponse(uuid, url.get());
                event.getPlayer().kick(GsonComponentSerializer.gson().deserialize(response));
                return;
            } catch (JsonSyntaxException e) {
                MinecraftServer.LOGGER.error("Auth response was malformed!");
            } catch (Exception e) {
                /*NOTHING*/
            }
        }

        event.getPlayer().kick(Component.text("Error occurred trying to get auth code.").color(NamedTextColor.RED));
    }

    public static String getAuthResponse(String uuid, String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url.replace(UUID_REPLACE_STRING, uuid)).openConnection();
        Config.getHeaders().forEach((key, value) -> connection.setRequestProperty(key, value.replace(UUID_REPLACE_STRING, uuid)));

        final int responseCode = connection.getResponseCode();
        final InputStream inputStream = 200 <= responseCode && responseCode <= 299 ? connection.getInputStream() : connection.getErrorStream();

        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder response = new StringBuilder();
        String currentLine;

        while ((currentLine = in.readLine()) != null) response.append(currentLine);

        in.close();

        return response.toString();
    }

}
