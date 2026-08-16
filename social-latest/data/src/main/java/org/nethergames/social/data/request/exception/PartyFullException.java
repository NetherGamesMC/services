package org.nethergames.social.data.request.exception;


import com.google.rpc.Code;

public class PartyFullException extends RequestException {
    public PartyFullException(String name) {
        super("party_full", "A party with the name " + name + " is currently full", Code.RESOURCE_EXHAUSTED, 507);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
