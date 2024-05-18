package com.kadowork.app.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class Chat {
    @Getter(AccessLevel.NONE)
    private String id;
    @Getter(AccessLevel.NONE)
    private String userId;
    private Role role;
    private String content;
    private String typedAt;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "userId-index")
    public String getUserId() {
        return userId;
    }

    public enum Role {
        system,
        user,
        assistant
    }
}
