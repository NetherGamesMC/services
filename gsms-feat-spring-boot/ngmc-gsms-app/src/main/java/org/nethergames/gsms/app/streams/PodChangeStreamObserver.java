package org.nethergames.gsms.app.streams;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.nethergames.gsms.domain.context.ClientContext;
import org.nethergames.gsms.infra.service.ClientEventCommandService;
import org.nethergames.gsms.rpc.GameEvent;
import org.nethergames.gsms.rpc.UpdateModel;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PodChangeStreamObserver implements StreamObserver<UpdateModel> {

	private final UUID podUUID;

	private final ClientEventCommandService commandService;

	public PodChangeStreamObserver(ClientEventCommandService service, UUID podUUID) {
		this.commandService = service;
		this.podUUID = podUUID;
	}

	@Override
	public void onNext(UpdateModel updateModel) {
		Context.current().withValue(ClientContext.CTX_SERVER_UUID, podUUID)
				.run(() -> commandService.onEventReceived(updateModel));
	}

	@Override
	public void onError(Throwable throwable) {
		commandService.removeGameEventStreams(podUUID);
	}

	@Override
	public void onCompleted() {
		commandService.removeGameEventStreams(podUUID);
	}
}
