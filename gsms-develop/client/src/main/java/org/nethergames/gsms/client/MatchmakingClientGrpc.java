package org.nethergames.gsms.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.nethergames.gsms.rpc.MatchmakingRequest;
import org.nethergames.gsms.rpc.MatchmakingServiceGrpc;
import org.nethergames.gsms.rpc.Region;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class MatchmakingClientGrpc {
    private final ManagedChannel channel;

    @Getter
    private final MatchmakingServiceGrpc.MatchmakingServiceStub asyncStub;
    @Getter
    private final MatchmakingServiceGrpc.MatchmakingServiceBlockingStub stub;
    @Getter
    private final MatchmakingServiceGrpc.MatchmakingServiceFutureStub futureStub;

    public MatchmakingClientGrpc(String host, int port) {
        this(NettyChannelBuilder.forAddress(new InetSocketAddress(host, port)).enableRetry().usePlaintext());
    }

    private MatchmakingClientGrpc(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.asyncStub = MatchmakingServiceGrpc.newStub(this.channel);
        this.stub = MatchmakingServiceGrpc.newBlockingStub(this.channel);
        this.futureStub = MatchmakingServiceGrpc.newFutureStub(this.channel);
    }

    public static void main(String[] args) {
        MatchmakingClientGrpc client = new MatchmakingClientGrpc("localhost", 5001);

        var value = client.getStub().matchmake(MatchmakingRequest.newBuilder()
                .setServerType("lobby")
                .setCurrentRegion(Region.EU)
                .setCanJoinFull(true)
                .build());

        System.out.println("Received matchmaking: " + value.getServerUniqueId() + " Result: " + value.getResultCode());

        client.shutdown();
    }

    @SneakyThrows
    public void shutdown() {
        this.channel.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
}
