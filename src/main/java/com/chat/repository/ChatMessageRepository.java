package com.chat.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.chat.entity.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    @Query(value = "{ '$or': ["
            + "{ 'senderUsername': ?0, 'recipientUsername': ?1, 'scope': 'PRIVATE' },"
            + "{ 'senderUsername': ?1, 'recipientUsername': ?0, 'scope': 'PRIVATE' }"
            + "] }", sort = "{ 'sentAt': 1 }")
    List<ChatMessage> findConversation(String userA, String userB);

    @Query(value = "{ 'scope': 'GROUP', 'groupCode': ?0 }", sort = "{ 'sentAt': 1 }")
    List<ChatMessage> findGroupMessages(String groupCode);
}
