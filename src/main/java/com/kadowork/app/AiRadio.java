package com.kadowork.app;

import com.amazonaws.services.lambda.runtime.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.*;
import com.google.gson.*;
import com.kadowork.app.entity.*;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.*;
import org.apache.http.impl.client.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.web.client.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.regions.*;
import software.amazon.awssdk.services.dynamodb.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import static com.kadowork.app.entity.Chat.Role.assistant;
import static com.kadowork.app.entity.Chat.Role.user;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class AiRadio implements RequestHandler<Map<String, Object>, Object> {
    private final RestTemplate restTemplate = restTemplate();

    private static final String LINE_ACCESS_TOKEN = System.getenv("LINE_ACCESS_TOKEN");
    private static final String OPENAI_TOKEN = System.getenv("OPENAI_TOKEN");
    private static final String OPENAI_MODEL_NAME = System.getenv("OPENAI_MODEL_NAME");
    private static final String AWS_ACCESS_KEY = System.getenv("AWS_MY_ACCESS_KEY");
    private static final String AWS_ACCESS_SECRET = System.getenv("AWS_MY_ACCESS_SECRET");
    private static final int SCAN_RECORD_NUM = Integer.parseInt(System.getenv("SCAN_RECORD_NUM"));
    private static final long OPENAPI_DURATION_SECONDS = Integer.parseInt(System.getenv("OPENAPI_DURATION_SECONDS"));
    private static final int REST_TEMPLATE_COMMON_TIMEOUT = Integer.parseInt(System.getenv("REST_TEMPLATE_COMMON_TIMEOUT"));

    private static final String LINE_REPLY_POST_URL = "https://api.line.me/v2/bot/message/reply";
    private static final String DYNAMODB_URL = "https://dynamodb.ap-northeast-1.amazonaws.com";

    @Override
    public Object handleRequest(Map<String, Object> map, Context context) {
        // test
        System.out.println("I am Lambda!");
        map.forEach((key, value) -> System.out.println(key + ":" + value));

        // TODO とりあえず line のヤツを持ってきたので、 ai-radio 用に作り替えていく格好

//        List<Chat> chatHistory = new ArrayList<>();
//        Chat chat = Chat.builder()
//                        .id(UUID.randomUUID().toString())
//                        .userId(body.getEvents()[0].getSource().getUserId())
//                        .role(user)
//                        .content(messageWithoutOption)
//                        .typedAt(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).toString())
//                        .build();
//        chatRepository.save(chat);
//        chatHistory.add(chat);

        System.out.println("処理終了〜");
        return null;
    }

    private String chatOpenAI(List<Chat> messages) {
        final var service = new OpenAiService(OPENAI_TOKEN, Duration.ofSeconds(OPENAPI_DURATION_SECONDS));
        System.out.println("\nCreating completion...");
        List<ChatMessage> chatMessages = new LinkedList<>();
        chatMessages.add(new ChatMessage("system", "あなたはとても優秀な秘書です。ユーザーからの相談や疑問に対して、親身になって回答してください。その際は説明が長くなりすぎず、より簡潔に内容をまとめるようにしてください。"));
        messages.stream()
                .sorted(Comparator.comparing(Chat::getTypedAt))
                .forEach(x -> chatMessages.add(new ChatMessage(x.getRole().toString(), x.getContent())));
        System.out.println(chatMessages);
        final var chatCompletionRequest = ChatCompletionRequest.builder()
                                                               .model(OPENAI_MODEL_NAME)
                                                               .maxTokens(1024)
                                                               .messages(chatMessages)
                                                               .build();
        var result = service.createChatCompletion(chatCompletionRequest);
        for (final ChatCompletionChoice choice : result.getChoices()) {
            System.out.println(choice);
        }
        return result.getChoices().get(0).getMessage().getContent();
    }

    private ResponseEntity<String> postLineNotify(String bodyJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + LINE_ACCESS_TOKEN);
        headers.setContentType(APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(bodyJson, headers);
        return restTemplate.exchange(LINE_REPLY_POST_URL, POST, httpEntity, String.class);
    }

    private RestTemplate restTemplate() {
        var clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build());
        // 接続が確立するまでのタイムアウト
        clientHttpRequestFactory.setConnectTimeout(REST_TEMPLATE_COMMON_TIMEOUT);
        // コネクションマネージャーからの接続要求のタイムアウト
        clientHttpRequestFactory.setConnectionRequestTimeout(REST_TEMPLATE_COMMON_TIMEOUT);
        // ソケットのタイムアウト（パケット間のタイムアウト）
        clientHttpRequestFactory.setReadTimeout(REST_TEMPLATE_COMMON_TIMEOUT);
        return new RestTemplate(clientHttpRequestFactory);
    }
}