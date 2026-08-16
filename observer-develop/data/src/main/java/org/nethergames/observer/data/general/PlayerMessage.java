package org.nethergames.observer.data.general;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Data
@AllArgsConstructor
abstract public class PlayerMessage {
    private TYPE type;

    public enum TYPE {
        STATIC,
        TRANSLATED
    }

    public static TranslatedPlayerMessage Translated(String targetXuid, String translationKey, Map<String, String> variables) {
        return new TranslatedPlayerMessage(translationKey, variables);
    }

    public abstract String getMessage() throws JsonProcessingException;

    public static StaticPlayerMessage Static(String message) {
        return new StaticPlayerMessage(message);
    }

    @Getter
    @ToString
    public static class TranslatedPlayerMessage extends PlayerMessage {
        private final String translationKey;
        private final Map<String, String> variables;

        private TranslatedPlayerMessage(String translationKey, Map<String, String> variables) {
            super(TYPE.TRANSLATED);

            this.translationKey = translationKey;
            this.variables = variables;
        }

        public String getMessage() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            return "{\"type\":0,\"message\":{\"type\":1,\"messageType\":1,\"translationKey\":\"" + this.translationKey + "\",\"variables\":\"" + mapper.writeValueAsString(variables) + "\"}}";
        }
    }


    @Getter
    @ToString
    public static class StaticPlayerMessage extends PlayerMessage {
        private final String message;

        private StaticPlayerMessage(String message) {
            super(TYPE.STATIC);

            this.message = message;
        }

        public String getMessage() {
            return "{\"type\":0,\"message\":{\"type\":0,\"text\":\"" + this.message + "\"}}";
        }

        public String getRawMessage() {
            return this.message;
        }
    }
}


