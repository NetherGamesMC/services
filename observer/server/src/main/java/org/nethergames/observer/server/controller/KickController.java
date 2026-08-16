package org.nethergames.observer.server.controller;

import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.nethergames.observer.data.kick.Kick;
import org.nethergames.observer.server.Observer;

public class KickController {
    // Abstract: In-game, Kick players from the server.

    // PUT: /kick: Kick a player from the server.
    // Body content is KickData.

    @OpenApi(
            summary = "Kick a player from the server.",
            path = "/kick",
            methods = HttpMethod.PUT,
            tags = "Kicks",
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = {
                            @OpenApiContent(from = Kick.class, mimeType = "application/json")
                    }
            ),
            responses = {
                    @OpenApiResponse(status = "200", description = "The player was successfully kicked."),
            }
    )
    public static void kickPlayer(Context context) {
        var reportData = context.bodyAsClass(Kick.class);
        Observer.getObserver().getKafkaManager().broadcastKick(reportData);
    }
}
