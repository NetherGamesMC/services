package org.nethergames.gsms.app.streams;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.nethergames.gsms.domain.context.ClientContext;
import org.nethergames.gsms.infra.service.ClientEventCommandService;
import org.nethergames.gsms.rpc.ProxyModel;

import java.util.UUID;

public class ProxyChangeStreamObserver implements StreamObserver<ProxyModel> {

	private final UUID podUid;

	private final ClientEventCommandService commandService;

	public ProxyChangeStreamObserver(ClientEventCommandService commandService, UUID podUid) {
		this.podUid = podUid;
		this.commandService = commandService;
	}

	@Override
	public void onNext(ProxyModel serverEvent) {
		Context.current().withValue(ClientContext.CTX_SERVER_UUID, podUid)
				.run(() -> commandService.onEventReceived(serverEvent));
	}

	@Override
	public void onError(Throwable throwable) {
		commandService.removeProxyEventStreams(podUid);
	}

	@Override
	public void onCompleted() {
		commandService.removeProxyEventStreams(podUid);
	}
}
