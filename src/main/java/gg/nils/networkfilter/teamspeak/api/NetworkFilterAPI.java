package gg.nils.networkfilter.teamspeak.api;

import com.google.gson.Gson;
import gg.nils.networkfilter.teamspeak.api.dao.NetworkFilterRequest;
import gg.nils.networkfilter.teamspeak.api.dao.NetworkFilterResponse;
import lombok.Getter;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NetworkFilterAPI {

    @Getter
    private static NetworkFilterAPI instance;

    private final String apiKey;

    private final Gson gson;
    private final Executor executor;
    private final OkHttpClient client;

    public NetworkFilterAPI(String apiKey) {
        instance = this;

        this.apiKey = apiKey;

        this.gson = new Gson();
        this.executor = Executors.newFixedThreadPool(4);
        this.client = new OkHttpClient().newBuilder().build();
    }

    public CompletableFuture<Boolean> check(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            MediaType mediaType = MediaType.parse("application/json");

            RequestBody body = RequestBody.create(
                    mediaType,
                    this.gson.toJson(new NetworkFilterRequest(ip))
            );

            Request request = new Request.Builder()
                    .url("https://nf.ni.ls/api/check")
                    .method("POST", body)
                    .addHeader("X-API-KEY", this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = this.client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return false;
                }

                NetworkFilterResponse networkFilterResponse = this.gson.fromJson(response.body().string(), NetworkFilterResponse.class);

                if (!networkFilterResponse.isSuccess()) {
                    return false;
                }

                return networkFilterResponse.getData().isBlocked();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }, this.executor);
    }
}
