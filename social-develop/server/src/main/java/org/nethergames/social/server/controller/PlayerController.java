package org.nethergames.social.server.controller;

import io.javalin.http.Context;
import org.nethergames.social.data.request.locality.LocalityEntry;
import org.nethergames.social.server.Social;

import java.util.ArrayList;

public class PlayerController {
    public static void getPlayerByName(Context context) {
        String playerName = context.pathParam("name");

        LocalityEntry entry = Social.getInstance().getLocalityManager().getPlayerByName(playerName);

        if (entry == null) {
            entry = Social.getInstance().getLocalityManager().getPlayerByXuid(playerName);
        }

        context.status(entry != null ? 200 : 404);

        if (entry != null) {
            context.json(entry);
        }
    }

    public static void getPlayerBulk(Context context) {
        String[] names = context.bodyAsClass(String[].class);

        ArrayList<LocalityEntry> list = new ArrayList<>();

        for (String name : names) {
            LocalityEntry entry = Social.getInstance().getLocalityManager().getPlayerByName(name);

            if (entry == null) {
                entry = Social.getInstance().getLocalityManager().getPlayerByXuid(name);
            }

            if (entry != null) {
                list.add(entry);
            }

        }

        context.json(list);

        Social.getInstance().getLogger().info("{}", String.join(", ", names));
    }
}