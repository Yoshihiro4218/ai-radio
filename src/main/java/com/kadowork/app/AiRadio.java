package com.kadowork.app;

import com.amazonaws.services.lambda.runtime.*;
import com.kadowork.app.entity.*;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.*;
import org.apache.http.impl.client.*;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.*;
import org.springframework.ai.openai.audio.speech.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.web.client.*;

import java.time.*;
import java.util.*;

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

    private static final String SYSTEM_PROMPT = """
            あなたはプロの編集者です。与えられたニュース記事を、要点を押さえたうえで、ラジオのアナウンサーがヘッドラインとして読む原稿として要約をします。
            ニュースのヘッドラインを聞く人たちは専門知識を持っていません。内容が理解しやすいようにしてください。
            なおラジオのニュースを聞く人たちは日本人なので、日本語で要約をしてください。
            """;
    private static final String OTEHON_NEWS = """
            Title: "Britain to World: Don’t Let What Happened to Us Happen to You",
            Description: "In bid for re-election, Conservatives’ economic record is hobbled by bad luck and bad choices."
            Content: "British Prime Minister Rishi Sunak called a general election for July 4. - Martyn Wheatley/Zuma Press President Biden, fighting for re-election with abysmal approval ratings, can count himself lucky… [+6081 chars]"
            """;
    private static final String OTEHON_SUMMARY = """
            イギリスのリシ・スナク首相は、7月4日に総選挙を実施すると発表しました。
            保守党は、経済実績が悪運や悪い選択によって損なわれている状況に直面しています。
            一方、アメリカではジョー・バイデン大統領が非常に低い支持率の中、再選に向けて戦っていますが、まだ幸運であると見られています。
            """;

    @Override
    public Object handleRequest(Map<String, Object> map, Context context) {
        // test
        System.out.println("I am Lambda!");
        map.forEach((key, value) -> System.out.println(key + ":" + value));

        // TODO とりあえず line のヤツを持ってきたので、 ai-radio 用に作り替えていく格好
        // テスト
        List<Chat> chatHistory = new ArrayList<>();
        Chat chat = Chat.builder()
                        .id(UUID.randomUUID().toString())
                        .userId("N/A")
                        .role(user)
                        // TODO
                        .content("""
                                 Title: Meet Salim Ramji, Who Is Going to Oversee the Retirement Assets of Tens of Millions of Americans
                                 Description: The first outsider to run Vanguard needs to win over the Bogleheads. Colleagues say he can.
                                 Content: The first outsider to take the helm of investing giant Vanguard Group is the son of Tanzanian immigrants who didnt attend college but wanted to ensure their three children did. Salim Ramji grew up in… [+5571 chars]
                                 """)
                        .typedAt(LocalDateTime.now(ZoneId.of("Asia/Tokyo")).toString())
                        .build();
        chatHistory.add(chat);
        String summary = chatOpenAI(chatHistory);

        // TODO: speech 部分...
//        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
//                                                                         .withModel("tts-1")
//                                                                         .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
//                                                                         .withResponseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
//                                                                         .withSpeed(1.0f)
//                                                                         .build();
//
//        var openAiAudioApi = new OpenAiAudioApi(System.getenv("OPENAI_API_KEY"));
//        var openAiAudioSpeechModel = new OpenAiAudioSpeechClient(openAiAudioApi);
//        var speechPrompt = new SpeechPrompt("Hello, this is a text-to-speech example.", speechOptions);
//        SpeechResponse response = openAiAudioSpeechModel.call(speechPrompt);
//        byte[] responseAsBytes = response.getResult().getOutput();


        System.out.println("処理終了〜");
        return null;
    }

    private String chatOpenAI(List<Chat> messages) {
        final var service = new OpenAiService(OPENAI_TOKEN, Duration.ofSeconds(OPENAPI_DURATION_SECONDS));
        System.out.println("\nCreating completion...");
        List<ChatMessage> chatMessages = new LinkedList<>();
        chatMessages.add(new ChatMessage("system", SYSTEM_PROMPT));
        chatMessages.add(new ChatMessage("user",OTEHON_NEWS ));
        chatMessages.add(new ChatMessage("assistant", OTEHON_SUMMARY));
        messages.stream()
                .sorted(Comparator.comparing(Chat::getTypedAt))
                .forEach(x -> chatMessages.add(new ChatMessage(x.getRole().toString(), x.getContent())));
        System.out.println(chatMessages);
        final var chatCompletionRequest = ChatCompletionRequest.builder()
                                                               .model(OPENAI_MODEL_NAME)
                                                               .maxTokens(3000)
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