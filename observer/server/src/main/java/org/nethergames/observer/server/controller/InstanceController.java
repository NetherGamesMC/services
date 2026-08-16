package org.nethergames.observer.server.controller;

import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import org.nethergames.observer.server.Observer;
import org.nethergames.utils.deployment.GithubDataFile;

import java.util.HashMap;
import java.util.Map;

public class InstanceController {
    @OpenApi(
            path = "/health",
            methods = HttpMethod.GET,
            tags = "Internal",
            summary = "Get health information of the observer instance"
    )
    public static void health(Context context) {
        Map<String, Object> values = new HashMap<>();
        values.put("environment", Observer.getEnvironment());
        values.put("serverName", Observer.getServerName());

        GithubDataFile dataFile = Observer.getObserver().getDataFile();
        values.put("infraCommit", dataFile.getCommit());
        values.put("infraBranch", dataFile.getBranch());
        values.put("applicationCommit", dataFile.getApplicationCommit());
        values.put("applicationBranch", dataFile.getApplicationBranch());
        values.put("mongoInformation", Observer.getObserver().getMongoManager().getClient().getClusterDescription().getConnectionMode());
        context.json(values);
    }
}
