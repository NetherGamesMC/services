package org.nethergames.social.data.request.exception;

import com.google.rpc.Code;

public class PartyNotPublicException extends RequestException {
    public PartyNotPublicException(String name) {
        super("party_not_public", "The party with the name " + name + " is not public", Code.NOT_FOUND, 404);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
