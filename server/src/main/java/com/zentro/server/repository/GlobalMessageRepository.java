package com.zentro.server.repository;

import com.zentro.server.model.GlobalMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalMessageRepository extends JpaRepository<GlobalMessage, Long> {

    @Query("SELECT m FROM GlobalMessage m ORDER BY m.timestamp ASC")
    List<GlobalMessage> findAllGlobalMessages();

    @Query("SELECT m FROM GlobalMessage m WHERE m.timestamp > :timestamp ORDER BY m.timestamp ASC")
    List<GlobalMessage> findNewMessagesSince(@Param("timestamp") java.time.LocalDateTime timestamp);

    @Query("SELECT m FROM GlobalMessage m ORDER BY m.timestamp DESC LIMIT :limit")
    List<GlobalMessage> findRecentMessages(@Param("limit") int limit);
}
