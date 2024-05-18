package com.kadowork.app.entity;

import lombok.*;

import java.util.*;

@Data
public class Output {
    private String replyToken;
    private List<Messages> messages = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Messages {

        private String type;
        private String text;
    }
}
