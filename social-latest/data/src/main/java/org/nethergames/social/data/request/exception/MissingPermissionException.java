package org.nethergames.social.data.request.exception;

import com.google.rpc.Code;

public class MissingPermissionException extends RequestException {
    public MissingPermissionException(String player, String action) {
        super("not_permitted", "The player " + player + " does not have the permission to execute " + action, Code.PERMISSION_DENIED, 403);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
