package org.nethergames.social.server.controller;

import io.javalin.http.Context;
import org.nethergames.social.data.request.locality.LocalityEntry;
import org.nethergames.social.data.request.locality.PlayerData;
import org.nethergames.social.data.request.locality.PlayerStatusResponse;
import org.nethergames.social.server.Social;
import org.nethergames.social.server.manager.LocalityManager;

import java.util.HashSet;
import java.util.Set;

public class LocalityController {
    public static void pushStatus(Context context) {
        LocalityEntry entry = context.bodyAsClass(LocalityEntry.class);

        Social.getInstance().getLocalityManager().addPlayerStatus(entry);
    }

    public static void getStatus(Context context) {
        Set<String> playerIdentifiers = context.bodyAsClass(HashSet.class);

        LocalityManager localityManager = Social.getInstance().getLocalityManager();
        PlayerStatusResponse response = new PlayerStatusResponse();

        for (String xuid : playerIdentifiers) {
            PlayerStatusResponse.PlayerStatusEntry statusEntry = new PlayerStatusResponse.PlayerStatusEntry();
            LocalityEntry entry = localityManager.getPlayerByXuid(xuid);
            if (entry != null) {
                PlayerData playerData = new PlayerData();
                playerData.setPlayerXuid(xuid);
                playerData.setPlayerName(entry.getPlayerName());
                playerData.setServerId(entry.getLocation());
                playerData.setSourceId(entry.getSourceUid());
                playerData.setAddress(entry.getAddress());
                statusEntry.setPlayerData(playerData);
            }

            statusEntry.setOnline(entry != null);

            response.getData().put(xuid, statusEntry);
        }

        context.json(response);
    }

    public static void getAll(Context context) {
        context.json(Social.getInstance().getLocalityManager().getAll());
    }


}
