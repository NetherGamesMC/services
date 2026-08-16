package org.nethergames.social.data.request.exception;
import com.google.rpc.Code;

import java.util.Collections;

public class AlreadyInPartyException extends RequestException {
    private final String currentParty;

    public AlreadyInPartyException(String currentParty, String attemptedParty) {
        super("already_in_party", "Player is still in party " + currentParty + " but tried to join " + attemptedParty, Code.ALREADY_EXISTS, 409);
        this.currentParty = currentParty;
    }

    @Override
    public Object getInfo() {
        return Collections.singletonMap("currentParty", this.currentParty);
    }
}
