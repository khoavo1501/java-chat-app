package com.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select m from ChatMessage m "
            + "where (m.senderUsername = :userA and m.recipientUsername = :userB) "
            + "or (m.senderUsername = :userB and m.recipientUsername = :userA) "
            + "order by m.sentAt asc")
    List<ChatMessage> findConversation(@Param("userA") String userA, @Param("userB") String userB);
}
