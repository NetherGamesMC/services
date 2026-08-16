package org.nethergames.social.data.request.exception;

import com.google.rpc.Code;
import io.grpc.Status;

public class CannotLeaveLeaderException extends RequestException {

    public CannotLeaveLeaderException() {
        super("is_leader", "You cannot leave the party while you are a party leader. Give leadership to someone else first.", Code.INVALID_ARGUMENT, 409);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
