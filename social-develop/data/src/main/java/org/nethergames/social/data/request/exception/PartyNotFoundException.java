package org.nethergames.social.data.request.exception;

import com.google.rpc.Code;

public class PartyNotFoundException extends RequestException {
    public PartyNotFoundException(String name) {
        super("party_not_found", "A party with the name " + name + " does not exists", Code.NOT_FOUND, 404);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
