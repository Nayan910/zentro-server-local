package com.zentro.server.repository;

import com.zentro.server.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByOrderByTimestampDesc();

    List<ActivityLog> findByUserIdOrderByTimestampDesc(Long userId);

    List<ActivityLog> findByActionOrderByTimestampDesc(String action);
}
