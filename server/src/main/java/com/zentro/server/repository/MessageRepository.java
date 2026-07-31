package com.zentro.server.repository;

import com.zentro.server.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query(value = "SELECT * FROM messages m WHERE " +
           "(m.sender_id = :userId1 AND m.receiver_id = :userId2) OR " +
           "(m.sender_id = :userId2 AND m.receiver_id = :userId1) " +
           "ORDER BY m.timestamp ASC", nativeQuery = true)
    List<Message> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query(value = "SELECT * FROM messages m WHERE m.receiver_id = :userId AND m.is_read = false", nativeQuery = true)
    List<Message> findUnreadByReceiverId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "UPDATE messages m SET m.is_read = true WHERE m.sender_id = :senderId AND m.receiver_id = :receiverId AND m.is_read = false", nativeQuery = true)
    void markAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    long countByReceiverIdAndIsReadFalse(Long receiverId);
}
