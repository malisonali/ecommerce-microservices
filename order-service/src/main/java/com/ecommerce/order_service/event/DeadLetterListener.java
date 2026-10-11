package com.ecommerce.order_service.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DeadLetterListener {

    @KafkaListener(topics = "inventory-events.DLT",
            groupId = "order-service-dlt",
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        log.error("""
                DEAD LETTER received
                  original: {} partition {} offset {}
                  key:      {}
                  error:    {}: {}
                  value:    {}""",
                text(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                number(record, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                number(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                record.key(),
                text(record, KafkaHeaders.DLT_EXCEPTION_FQCN),
                text(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                record.value());
    }

    // Text headers (topic, exception class, message) are stored as UTF-8 bytes
    private static String text(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? "-" : new String(header.value(), StandardCharsets.UTF_8);
    }

    // Partition is stored as a 4-byte int, offset as an 8-byte long
    private static String number(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null) {
            return "-";
        }
        byte[] bytes = header.value();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (bytes.length == Integer.BYTES) {
            return String.valueOf(buffer.getInt());
        }
        if (bytes.length == Long.BYTES) {
            return String.valueOf(buffer.getLong());
        }
        return "?";
    }
}