package com.paystream.account.repository;

import com.paystream.account.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for the Transactional Outbox. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** Fetches a bounded batch of unpublished events oldest-first for the relay. */
    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();

    /** Bulk-deletes already-published events older than the cutoff (retention cleanup). */
    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.published = true AND o.createdAt < :cutoff")
    int deletePublishedOlderThan(@Param("cutoff") Instant cutoff);
}
