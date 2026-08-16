package org.nethergames.gsms.domain.context;

import io.grpc.Context;
import org.nethergames.gsms.domain.constants.ServerCategory;

import java.util.UUID;

public class ClientContext {
	public static final Context.Key<ServerCategory> CTX_SERVER_TYPE = Context.key("server-type");

	public static final Context.Key<String> CTX_SERVER_ID = Context.key("server-id");

	public static final Context.Key<UUID> CTX_SERVER_UUID = Context.key("pod-uuid");

}
