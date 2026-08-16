package org.nethergames.social.data.request.exception;


import com.google.rpc.Code;

public class NotInPartyException extends RequestException {
    public NotInPartyException(String name) {
        super("not_in_party", name + " is not member of a party.", Code.NOT_FOUND, 404);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
