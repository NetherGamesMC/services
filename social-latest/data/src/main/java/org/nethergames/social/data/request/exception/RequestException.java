package org.nethergames.social.data.request.exception;

import com.google.rpc.Code;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class RequestException extends RuntimeException {
    private String identifier;
    private String message;
    private Code errorCode;
    private int httpStatus;

    public abstract Object getInfo();
}
