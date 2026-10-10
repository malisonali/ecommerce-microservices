package com.ecommerce.order_service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<OutboxEvent> batch = outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();

        for (OutboxEvent event : batch) {
            try {
                RecordMetadata meta = kafkaTemplate
                        .send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS)
                        .getRecordMetadata();

                event.markPublished();
                outboxRepository.save(event);
                log.info("Relayed outbox #{} ({}) for key {} to partition {} at offset {}",
                        event.getId(), event.getEventType(), event.getMessageKey(),
                        meta.partition(), meta.offset());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                Throwable cause = e;
                while (cause.getCause() != null && cause.getCause() != cause) {
                    cause = cause.getCause();
                }
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUpOldEvents() {
        int deleted = outboxRepository.deletePublishedBefore(Instant.now().minus(Duration.ofDays(7)));
        if (deleted > 0) {
            log.info("Deleted {} published outbox events older than 7 days", deleted);
        }
    }
}