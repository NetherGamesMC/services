package org.nethergames.social.data.request.exception;


import com.google.rpc.Code;

public class NotInvitedException extends RequestException {
    public NotInvitedException(String name) {
        super("not_invited", "There is no pending invite to the party " + name, Code.NOT_FOUND, 404);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
