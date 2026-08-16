package org.nethergames.gsms.app.interceptor;

import io.grpc.*;
import org.apache.commons.lang3.StringUtils;
import org.nethergames.gsms.domain.constants.ServerCategory;
import org.nethergames.gsms.domain.context.ClientContext;
import org.springframework.stereotype.Component;

@Component
public class GameInterceptor implements ServerInterceptor {
	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
			ServerCall<ReqT, RespT> call,
			Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {

		String serverTypeStr = headers.get(Metadata.Key.of("server-type", Metadata.ASCII_STRING_MARSHALLER));
		String serverId = headers.get(Metadata.Key.of("server-id", Metadata.ASCII_STRING_MARSHALLER));
		if (StringUtils.isBlank(serverTypeStr) || StringUtils.isBlank(serverId)) {
			call.close(Status.FAILED_PRECONDITION.withDescription("Missing or invalid server type or server id"), headers);
			return new ServerCall.Listener<>() {};
		}

		ServerCategory serverType;
		try {
			serverType = ServerCategory.valueOf(serverTypeStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			call.close(Status.FAILED_PRECONDITION.withDescription("Missing or invalid server type or server id"), headers);
			return new ServerCall.Listener<>() {};
		}

		// Save into context for later use.
		Context ctx = Context.current().withValue(ClientContext.CTX_SERVER_TYPE, serverType)
				.withValue(ClientContext.CTX_SERVER_ID, serverId);
		return Contexts.interceptCall(ctx, call, headers, next);
	}
}
