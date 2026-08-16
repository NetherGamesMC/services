package org.nethergames.social.server.task;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.nethergames.social.rpc.GetStateResponse;
import org.nethergames.social.server.Social;

import java.util.ArrayList;
import java.util.List;

@Log4j2(topic = "MetricsSubscriber")
public class MetricsSubscriptionTask implements Runnable {
    private static final List<StreamObserver<GetStateResponse>> subscribedStates = new ArrayList<>();

    @Override
    public void run() {
        var totalPlayers = Social.getInstance().getLocalityManager().getSize();

        synchronized (subscribedStates) {
            try {
                publishEvents(totalPlayers);
            } catch (Throwable t) {
                log.error("Unhandled error when ticking events task.", t);
            }
        }
    }

    public void publishEvents(int totalPlayers) {
        var iter = subscribedStates.iterator();

        while (iter.hasNext()) {
            var stream = iter.next();

            try {
                var response = GetStateResponse.newBuilder();
                response.setPlayerCount(totalPlayers);

                stream.onNext(response.build());
            } catch (Throwable t) {
                if (Status.fromThrowable(t).getCode() != Status.Code.CANCELLED) {
                    log.error("Received an error", t);
                }

                iter.remove();
                stream.onCompleted();
            }
        }
    }

    public static void addSubscriber(StreamObserver<GetStateResponse> states) {
        synchronized (subscribedStates) {
            subscribedStates.add(states);
        }
    }
}
