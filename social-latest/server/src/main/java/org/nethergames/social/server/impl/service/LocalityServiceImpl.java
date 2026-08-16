package org.nethergames.social.server.impl.service;

import io.grpc.stub.StreamObserver;
import io.sentry.Sentry;
import org.nethergames.social.data.request.locality.LocalityEntry;
import org.nethergames.social.rpc.Shutdown;
import org.nethergames.social.rpc.*;
import org.nethergames.social.server.Social;
import org.nethergames.social.server.events.PlayerSwitchServerEvent;
import org.nethergames.social.server.task.MetricsSubscriptionTask;

import java.util.ArrayList;
import java.util.List;

public class LocalityServiceImpl extends PlayerLocalityGrpc.PlayerLocalityImplBase {

    @Override
    public void signup(Signup request, StreamObserver<SignupResponse> responseObserver) {
        String sourceId = Social.getInstance().getSourceManager().signupSource(request.getSourceId());

        responseObserver.onNext(
                SignupResponse.newBuilder()
                        .setSourceUid(sourceId)
                        .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void shutdown(Shutdown request, StreamObserver<ActionResponse> responseObserver) {
        if (!Social.getInstance().getSourceManager().sourceExists(request.getSourceUid())) {
            responseObserver.onNext(ActionResponse.newBuilder()
                    .setActionComplete(false)
                    .setValidSourceUid(false)
                    .build()
            );
            responseObserver.onCompleted();
            return;
        }

        Social.getInstance().getSourceManager().removeSource(request.getSourceUid());

        responseObserver.onNext(ActionResponse.newBuilder()
                .setActionComplete(true)
                .setValidSourceUid(true)
                .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void addPlayer(AddPlayer request, StreamObserver<ActionResponse> responseObserver) {
        try {
            ActionResponse response;
            Social social = Social.getInstance();

            if (!social.getSourceManager().sourceExists(request.getSourceUid())) {
                response = ActionResponse.newBuilder().setActionComplete(false).setValidSourceUid(false).build();
            } else {
                for (PlayerData player : request.getDataList()) {
                    LocalityEntry entry = getLocalityEntry(request, player);

                    social.getLocalityManager().addPlayerStatus(entry);
                }

                response = ActionResponse.newBuilder().setActionComplete(true).setValidSourceUid(true).build();
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Throwable t) {
            Sentry.captureException(t);
            Social.getInstance().getLogger().error("Error while adding players", t);
        }
    }

    @Override
    public void removePlayer(RemovePlayer request, StreamObserver<ActionResponse> responseObserver) {
        try {
            ActionResponse response;
            if (!Social.getInstance().getSourceManager().sourceExists(request.getSourceUid())) {
                response = ActionResponse.newBuilder().setActionComplete(false).setValidSourceUid(false).build();
            } else {
                for (String identifier : request.getPlayerXuidList()) {
                    boolean success = Social.getInstance().getLocalityManager().removePlayer(identifier);
                    Social.getInstance().getLogger().info("Removing player {} {}", identifier, success ? "was successful." : "was failed.");
                }

                response = ActionResponse.newBuilder().setActionComplete(true).setValidSourceUid(true).build();
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Throwable t) {
            Social.getInstance().getLogger().error("Error while removing players", t);
            Sentry.captureException(t);
        }
    }

    @Override
    public void getPlayer(GetPlayerStatus request, StreamObserver<PlayerStatusResponse> responseObserver) {
        try {
            PlayerStatusResponse.Builder builder = PlayerStatusResponse.newBuilder();

            for (String identifier : request.getPlayerInfoList()) {
                PlayerStatusEntry.Builder response = PlayerStatusEntry.newBuilder().setSearchEntry(identifier);
                LocalityEntry entry = Social.getInstance().getLocalityManager().getPlayerByName(identifier);

                if (entry == null) {
                    entry = Social.getInstance().getLocalityManager().getPlayerByXuid(identifier);
                }

                buildPlayerLocality(builder, response, entry);
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Throwable t) {
            Sentry.captureException(t);
            Social.getInstance().getLogger().error("Error while fulfilling player get request", t);
        }
    }

    @Override
    public void getPlayerByXuid(GetPlayerStatus request, StreamObserver<PlayerStatusResponse> responseObserver) {
        try {
            PlayerStatusResponse.Builder builder = PlayerStatusResponse.newBuilder();

            for (String xuid : request.getPlayerInfoList()) {
                PlayerStatusEntry.Builder response = PlayerStatusEntry.newBuilder().setSearchEntry(xuid);
                LocalityEntry entry = Social.getInstance().getLocalityManager().getPlayerByXuid(xuid);

                buildPlayerLocality(builder, response, entry);
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Throwable t) {
            Sentry.captureException(t);
            Social.getInstance().getLogger().error("Error while fulfilling player get request", t);
        }
    }

    @Override
    public void getPlayerByName(GetPlayerStatus request, StreamObserver<PlayerStatusResponse> responseObserver) {
        try {
            PlayerStatusResponse.Builder builder = PlayerStatusResponse.newBuilder();

            for (String playerName : request.getPlayerInfoList()) {
                PlayerStatusEntry.Builder response = PlayerStatusEntry.newBuilder().setSearchEntry(playerName);
                LocalityEntry entry = Social.getInstance().getLocalityManager().getPlayerByName(playerName);

                buildPlayerLocality(builder, response, entry);
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Throwable t) {
            Sentry.captureException(t);
            Social.getInstance().getLogger().error("Error while fulfilling player get request", t);
        }
    }

    private void buildPlayerLocality(PlayerStatusResponse.Builder builder, PlayerStatusEntry.Builder response, LocalityEntry entry) {
        response.setOnline(entry != null);

        if (entry != null) {
            var data = PlayerData.newBuilder();
            data.setPlayerName(entry.getPlayerName());
            data.setPlayerXuid(entry.getPlayerIdentifier());
            data.setServerId(entry.getLocation());
            data.setSourceId(entry.getProxyId());
            data.setAddress(entry.getAddress());
            data.setConnectionId(entry.getConnectionId());

            response.setData(data.build());
        }

        builder.addResponses(response);
    }

    @Override
    public void getAll(Empty request, StreamObserver<GetAllResponse> responseObserver) {
        List<PlayerData> data = new ArrayList<>();

        for (LocalityEntry entry : Social.getInstance().getLocalityManager().getAll().values()) {
            data.add(PlayerData.newBuilder()
                    .setServerId(entry.getLocation())
                    .setSourceId(entry.getSourceUid())
                    .setPlayerName(entry.getPlayerName())
                    .setPlayerXuid(entry.getPlayerIdentifier())
                    .setAddress(entry.getAddress())
                    .build()
            );
        }

        GetAllResponse response = GetAllResponse.newBuilder().addAllData(data).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getState(Empty request, StreamObserver<GetStateResponse> responseObserver) {
        MetricsSubscriptionTask.addSubscriber(responseObserver);
    }

    @Override
    public void updatePlayer(UpdatePlayerStatus request, StreamObserver<ActionResponse> responseObserver) {
        ActionResponse response;

        if (!Social.getInstance().getSourceManager().sourceExists(request.getSourceUid())) {
            response = ActionResponse.newBuilder().setActionComplete(false).setValidSourceUid(false).build();
        } else {
            boolean success = false;
            if (request.getStatus().equals(PlayerStatus.Online)) { // Player is still online, update position
                LocalityEntry entry = Social.getInstance().getLocalityManager().getPlayerByXuid(request.getPlayerXuid());

                if (entry != null) {
                    Social.getInstance().getEventBus().post(new PlayerSwitchServerEvent(entry.getPlayerIdentifier(), entry.getLocation(), request.getServerId()));
                    entry.setLastLocation(entry.getLocation());
                    entry.setLocation(request.getServerId());
                    success = true;
                }

            } else { // Player is offline, remove
                try {
                    Social.getInstance().getLocalityManager().removePlayer(request.getPlayerXuid());
                } catch (Throwable t) {
                    Sentry.captureException(t);
                    Social.getInstance().getLogger().error("Error", t);
                }

                success = true;
            }

            response = ActionResponse.newBuilder().setValidSourceUid(true).setActionComplete(success).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayerEvents(Empty request, StreamObserver<PlayerEvent> responseObserver) {
        Social.getInstance().getLocalityManager().addEventListener(responseObserver);
    }

    private static LocalityEntry getLocalityEntry(AddPlayer request, PlayerData player) {
        LocalityEntry entry = new LocalityEntry();
        entry.setLocation(player.getServerId());
        entry.setSourceUid(request.getSourceUid());
        entry.setProxyId(player.getSourceId());
        entry.setPlayerName(player.getPlayerName());
        entry.setPlayerIdentifier(player.getPlayerXuid());
        entry.setAddress(player.getAddress());

        // Let proxies set the player connection ID for existing connections.
        String connectionId = player.getConnectionId();
        if (!connectionId.isEmpty()) {
            entry.setConnectionId(connectionId);
        }

        return entry;
    }
}
