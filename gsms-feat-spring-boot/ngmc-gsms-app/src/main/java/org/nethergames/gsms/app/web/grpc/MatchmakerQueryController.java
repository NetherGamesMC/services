package org.nethergames.gsms.app.web.grpc;

import org.nethergames.gsms.rpc.MatchmakingServiceGrpc;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class MatchmakerQueryController extends MatchmakingServiceGrpc.MatchmakingServiceImplBase {
}
