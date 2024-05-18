package com.kadowork.app.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Body {
    private Events[] events;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Events {
        private String replyToken;
        private String type;
        private Long timestamp;
        private Source source;
        private Message message;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Source {
            private String type;
            private String userId;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Message {
            private String id;
            private String type;
            private String text;
        }
    }
}
