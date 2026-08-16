package org.nethergames.gsms.server.impl;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.server.GSMS;

import java.io.IOException;

@Log4j2(topic = "gRPC Service")
public class GameService {
    @Getter
    private final Server server;
    @Getter
    private final GameServiceImpl gameService;
    @Getter
    private final MatchmakerServiceImpl matchmakerService;

    public GameService(GSMS gsms, int port) {
        server = ServerBuilder.forPort(port)
                .directExecutor()
                .addService(gameService = new GameServiceImpl(gsms))
                .addService(matchmakerService = new MatchmakerServiceImpl(gsms))
                .build();

        try {
            getServer().start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("GRPC Service started on port {}", port);
    }

    public void shutdown() throws InterruptedException {
        getServer().shutdownNow().awaitTermination();
    }
}
