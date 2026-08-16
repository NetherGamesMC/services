package org.nethergames.observer.server.manager;

import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;
import org.nethergames.observer.data.general.PlayerMessage;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.util.Configuration;
import org.nethergames.social.client.SocialClient;
import org.nethergames.social.rpc.GetPlayerStatus;
import org.nethergames.social.rpc.PlayerStatusEntry;
import org.nethergames.social.rpc.PlayerStatusResponse;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2(topic = "Social Manager")
public class SocialManager {
    private final SocialClient socialClient;

    public SocialManager() {
        socialClient = new SocialClient(Configuration.SOCIAL_HOST, 7500);
    }

    public boolean isPlayerOnlineByXuid(String xuid) {
        PlayerStatusResponse response = socialClient.getStub().getPlayerByXuid(GetPlayerStatus.newBuilder()
                .addPlayerInfo(xuid).build());

        return response.getResponses(0).getOnline();
    }

    public void sendPlayerMessage(String targetXuid, PlayerMessage message) {
        Preconditions.checkNotNull(message, "The message sent to a player cannot be null");

        if (isPlayerOnline(targetXuid)) {
            // player is online, send via kafka
            Observer.getObserver().getKafkaManager().broadcastChatMessage(targetXuid, message);
            log.info("Broadcasting chat message via kafka");
        } else {
            log.info("Sending offline message");
            if (!(message instanceof PlayerMessage.StaticPlayerMessage staticMessage)) {
                log.warn("Cannot send translated message {} because translated messages can't be sent to offline players", message);
                return;
            }
            sendOfflineMessage(targetXuid, staticMessage);
        }
    }

    private void sendOfflineMessage(String targetName, PlayerMessage.StaticPlayerMessage message) {
        try {
            PreparedStatement statement = Observer.getObserver().getDatabaseManager().getConnection().prepareStatement("INSERT INTO offline_messages(player, message, type) VALUES (?, ?, 0)");

            statement.setString(1, targetName);
            statement.setString(2, message.getRawMessage());

            statement.executeUpdate();
        } catch (SQLException exception) {
            log.error("Error while trying to send offline message to {}", targetName, exception);
        }
    }

    public boolean isPlayerOnline(String xuid) {
        PlayerStatusResponse response = this.socialClient.getStub().getPlayerByXuid(
                GetPlayerStatus.newBuilder()
                        .addPlayerInfo(xuid)
                        .build()
        );

        return response.getResponses(0).getOnline();
    }

    public Map<String, Boolean> arePlayersOnline(List<String> xuids) {
        GetPlayerStatus.Builder builder = GetPlayerStatus.newBuilder();

        builder.addAllPlayerInfo(xuids);
        PlayerStatusResponse response = socialClient.getStub().getPlayerByXuid(builder.build());

        Map<String, Boolean> result = new HashMap<>(response.getResponsesCount());
        response.getResponsesList().forEach(status -> result.put(status.getSearchEntry(), status.getOnline()));

        return result;
    }

    public PlayerStatusEntry getPlayerStatus(String xuid) {
        PlayerStatusResponse response = this.socialClient.getStub().getPlayerByXuid(
                GetPlayerStatus.newBuilder()
                        .addPlayerInfo(xuid)
                        .build()
        );

        return response.getResponses(0);
    }

    public boolean isPlayerOnlineByName(String name) {
        PlayerStatusResponse response = this.socialClient.getStub().getPlayerByName(
                GetPlayerStatus.newBuilder()
                        .addPlayerInfo(name)
                        .build()
        );

        return response.getResponses(0).getOnline();
    }

    public PlayerStatusEntry getPlayerStatusByName(String name) {
        PlayerStatusResponse response = this.socialClient.getStub().getPlayerByXuid(
                GetPlayerStatus.newBuilder()
                        .addPlayerInfo(name)
                        .build()
        );

        return response.getResponses(0);
    }

    public static class Translator {
        public enum MESSAGE_TYPE {
            DEFAULT("§l» §r", "§f", "§f"),
            INFO("§l» §r", "§b", "§f"),
            WARNING("§l» §r", "§e", "§c"),
            SUCCESS("§l» §r", "§a", "§f"),
            ERROR("§l» §r", "§c", "§f");
            private String prefix;
            private String argumentColor;
            private String baseColor;

            MESSAGE_TYPE(String prefix, String baseColor, String argumentColor) {
                this.prefix = prefix;
                this.baseColor = baseColor;
                this.argumentColor = argumentColor;
            }
        }
    }
}
