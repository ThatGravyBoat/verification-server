package com.resourcefulbees.server;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Config {

    private static final Gson GSON = new GsonBuilder().create();
    private static final File CONFIG_FILE = new File("./server.config.json");

    private static String favicon;
    private static String serverDesc;
    private static String url;
    private static final Map<String, String> headers = new HashMap<>();

    public static void read() {
        if (CONFIG_FILE.exists()) {
            //noinspection UnstableApiUsage
            try (BufferedReader reader = Files.newReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                url = getOrNull(json, "url");
                favicon = getOrNull(json, "favicon");
                serverDesc = getOrNull(json, "server_description");
                if (json.has("headers")) {
                    json.get("headers").getAsJsonObject().entrySet().forEach(entry -> headers.put(entry.getKey(), entry.getValue().getAsString()));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Could not load Config!");
            }
        }else {
            try {
                if (CONFIG_FILE.createNewFile()) save();
            }catch (Exception e){
                throw new IllegalStateException("Could not save Config!");
            }
        }
    }

    private static String getOrNull(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : null;
    }

    public static void save() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8))) {
            writer.write(GSON.toJson(new JsonObject()));
        }
    }

    public static Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public static Optional<String> getFavicon() {
        return Optional.ofNullable(favicon);
    }

    public static Optional<String> getServerDescription() {
        return Optional.ofNullable(serverDesc);
    }

    public static Map<String, String> getHeaders() {
        return headers;
    }
}
