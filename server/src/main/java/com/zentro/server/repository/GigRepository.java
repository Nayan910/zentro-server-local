package com.zentro.server.repository;

import com.zentro.server.model.Gig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GigRepository extends JpaRepository<Gig, Long> {

    List<Gig> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Gig> findByStatusOrderByCreatedAtDesc(String status);

    List<Gig> findByStatusAndTradeCategoryOrderByCreatedAtDesc(String status, String tradeCategory);

    List<Gig> findByAcceptedByIdOrderByCreatedAtDesc(Long userId);
}
