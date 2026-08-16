package org.nethergames.observer.server.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.nethergames.observer.server.util.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class APIManager {
    private OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public APIManager() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        String apiToken = Configuration.PUBLIC_API_TOKEN;

        if (apiToken == null) {
            log.error("Cannot start APIManager, no PUBLIC_API_TOKEN provided!");
            return;
        }

        builder.addInterceptor(chain -> {
            Request request = chain.request();
            Request newRequest = request.newBuilder()
                    .header("User-Agent", "nethergames/observer")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiToken)
                    .build();

            return chain.proceed(newRequest);
        });


        this.httpClient = builder.build();
    }

    public Map<String, String> getXboxNameMappings(Collection<String> xuids) {
        if (this.httpClient == null) {
            log.warn("Cannot fulfil getXboxNameMappings request, client is null");
            return Collections.emptyMap();
        }

        Response response;

        try {
            Request request = new Request.Builder()
                    .url(Configuration.PUBLIC_API_HOST + "/v1/players/xuids")
                    .post(
                            RequestBody.create(
                                    objectMapper.writeValueAsString(xuids),
                                    MediaType.parse("application/json")
                            )
                    )
                    .build();

            response = this.httpClient.newCall(request).execute();
        } catch (Throwable t) {
            log.error("Error while executing getXboxNameMappings request");
            return Collections.emptyMap();
        }


        try {
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    log.warn("Response body from API was null although status code was 200???");
                    return Collections.emptyMap();
                }

                return objectMapper.readValue(response.body().string(), XuidMap.class);
            }
        } catch (Throwable t) {
            log.error("Error while handling getXuidMappping response", t);
        } finally {
            response.close();
        }

        return Collections.emptyMap();
    }

    public static class XuidMap extends HashMap<String, String> {

    }

}

