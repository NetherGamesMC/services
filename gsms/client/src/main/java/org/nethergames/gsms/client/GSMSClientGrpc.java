package org.nethergames.gsms.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.SneakyThrows;
import org.nethergames.gsms.rpc.*;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GSMSClientGrpc {
    private final ManagedChannel channel;

    @Getter
    private final GSMSServiceGrpc.GSMSServiceStub asyncStub;
    @Getter
    private final GSMSServiceGrpc.GSMSServiceBlockingStub stub;
    @Getter
    private final GSMSServiceGrpc.GSMSServiceFutureStub futureStub;

    public GSMSClientGrpc(String host, int port) {
        this(NettyChannelBuilder.forAddress(new InetSocketAddress(host, port)).enableRetry().usePlaintext());
    }

    private GSMSClientGrpc(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.asyncStub = GSMSServiceGrpc.newStub(this.channel);
        this.stub = GSMSServiceGrpc.newBlockingStub(this.channel);
        this.futureStub = GSMSServiceGrpc.newFutureStub(this.channel);
    }

    @SneakyThrows
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        GSMSClientGrpc client = new GSMSClientGrpc("localhost", 5001);

        StreamObserver<ProxyModel> data = client.getAsyncStub().registerListenerProxy(new StreamObserver<>() {
            @Override
            public void onNext(ServerEvent value) {
                System.out.println("Event Type: " + value.getEventType() + "\nServerModel: " + value.getServer().getServerUniqueId() + "\n");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Received an error");
                t.printStackTrace();

                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        while (latch.getCount() > 0) {
            ProxyModel.Builder builder = ProxyModel.newBuilder();
            builder.setRegion(Region.AP);
            builder.setProxyId("ap-4");
            builder.setPlayerCount(150);

            try{
                data.onNext(builder.build());
            } catch (Throwable error){
                Status status = Status.fromThrowable(error);

                System.out.println("Status: " + status.getCode() + ", debug: " + status.getDescription());
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        }
    }

    public void shutdown() {
        this.channel.shutdownNow();
    }
}
