package com.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chat.entity.GroupRoom;

public interface GroupRoomRepository extends MongoRepository<GroupRoom, String> {

    Optional<GroupRoom> findByGroupCode(String groupCode);

    List<GroupRoom> findByMemberUsernamesContainingOrderByCreatedAtDesc(String username);

    List<GroupRoom> findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);
}