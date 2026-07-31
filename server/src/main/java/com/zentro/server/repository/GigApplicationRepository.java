package com.zentro.server.repository;

import com.zentro.server.model.GigApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GigApplicationRepository extends JpaRepository<GigApplication, Long> {

    List<GigApplication> findByGigIdOrderByCreatedAtDesc(Long gigId);

    List<GigApplication> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GigApplication> findByGigIdAndUserId(Long gigId, Long userId);

    boolean existsByGigIdAndUserId(Long gigId, Long userId);
}
