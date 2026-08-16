package org.nethergames.social.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import lombok.Getter;
import org.nethergames.social.rpc.*;

import java.net.InetSocketAddress;

public class SocialClient {
    private final ManagedChannel channel;

    @Getter
    private final PlayerLocalityGrpc.PlayerLocalityStub asyncStub;
    @Getter
    private final PlayerLocalityGrpc.PlayerLocalityBlockingStub stub;
    @Getter
    private final PlayerLocalityGrpc.PlayerLocalityFutureStub futureStub;

    public SocialClient(String host, int port) {
        this(NettyChannelBuilder.forAddress(new InetSocketAddress(host, port)).enableRetry().usePlaintext());
    }

    private SocialClient(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.asyncStub = PlayerLocalityGrpc.newStub(this.channel);
        this.stub = PlayerLocalityGrpc.newBlockingStub(this.channel);
        this.futureStub = PlayerLocalityGrpc.newFutureStub(this.channel);
    }

    public static void main(String[] args) {
        SocialClient client = new SocialClient("localhost", 7500);
        SignupResponse response = client.getStub().signup(Signup.newBuilder().setSourceId("test").build());

        System.out.println(response.getSourceUid());
    }

    public void shutdown() {
        this.channel.shutdownNow();
    }
}
