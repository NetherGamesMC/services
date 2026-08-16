package org.nethergames.gsms.server.impl.stream;

import io.grpc.stub.StreamObserver;

public class NoopStreamObserver<V> implements StreamObserver<V> {
    @Override
    public void onNext(V value) {
        // NOOP
    }

    @Override
    public void onError(Throwable t) {
        // NOOP
    }

    @Override
    public void onCompleted() {
        // NOOP
    }
}
