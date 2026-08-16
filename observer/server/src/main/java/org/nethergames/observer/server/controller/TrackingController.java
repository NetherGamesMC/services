package org.nethergames.observer.server.controller;

import io.javalin.http.Context;
import io.javalin.openapi.OpenApi;

public class TrackingController {

    @OpenApi(
            path = "/match/tracking"
    )
    public static void addTracking(Context context) {

    }
}
