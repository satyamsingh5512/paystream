package com.paystream.account.service;

import com.paystream.account.repository.OutboxEventRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically purges already-published outbox rows so the table does not grow unbounded.
 * Published rows are retained for a configurable window (default 7 days) to allow auditing
 * and replay before deletion.
 */
@Component
public class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxEventRepository outboxRepository;
    private final long retentionHours;

    public OutboxCleanupJob(OutboxEventRepository outboxRepository,
                            @org.springframework.beans.factory.annotation.Value(
                                    "${paystream.account.outbox.retention-hours:168}") long retentionHours) {
        this.outboxRepository = outboxRepository;
        this.retentionHours = retentionHours;
    }

    @Scheduled(cron = "${paystream.account.outbox.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    @Transactional
    public void purgePublished() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(retentionHours));
        int deleted = outboxRepository.deletePublishedOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Purged {} published outbox row(s) older than {}h", deleted, retentionHours);
        }
    }
}
