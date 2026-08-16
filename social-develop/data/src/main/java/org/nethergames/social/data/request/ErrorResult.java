package org.nethergames.social.data.request;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ErrorResult<T> {
    private String id;
    private String message;
    private String errorId;
    private T info;
}

