package org.nethergames.social.server.impl;

import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import lombok.extern.log4j.Log4j2;
import org.nethergames.social.server.impl.service.LocalityServiceImpl;
import org.nethergames.social.server.impl.service.SocialServiceImpl;
import org.nethergames.social.server.task.MetricsSubscriptionTask;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2(topic = "SocialService")
public class SocialService {
    public SocialService(int port) {
        this(ServerBuilder.forPort(port), port);
    }

    public SocialService(ServerBuilder<?> serverBuilder, int port) {
        var server = ((NettyServerBuilder) serverBuilder)
                .executor(Executors.newWorkStealingPool())
                .addService(new SocialServiceImpl())
                .addService(new LocalityServiceImpl())
                .permitKeepAliveWithoutCalls(true)
                .permitKeepAliveTime(10, TimeUnit.SECONDS)
                .maxConnectionIdle(5, TimeUnit.SECONDS)
                .maxConnectionAgeGrace(10, TimeUnit.SECONDS)
                .maxInboundMessageSize(32 * 1024 * 1024)
                .maxInboundMetadataSize(1024 * 1024)
                .build();

        ScheduledExecutorService singleService = Executors.newSingleThreadScheduledExecutor();
        singleService.scheduleAtFixedRate(new MetricsSubscriptionTask(), 5, 1, TimeUnit.SECONDS);

        try {
            server.start();

            log.info("Service created on port {}", port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
