package com.project.auto_complete_service.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    // Resolves list format bindings cleanly without hitting NullPointerExceptions
    private List<String> getResolvedBootstrapServers() {
        List<String> servers = kafkaProperties.getBootstrapServers();
        if (servers == null || servers.isEmpty()) {
            // Hard fallback directly to your unique Aiven Cluster URI
            return Collections.singletonList("autocomplete-kafka-ak9623019-b363.g.aivencloud.com:19348");
        }
        return servers;
    }

    private void applySslConfigs(Map<String, Object> props) {
        Map<String, String> kafkaCustomProps = kafkaProperties.getProperties();

        if (kafkaCustomProps != null && "SSL".equalsIgnoreCase(kafkaCustomProps.get("security.protocol"))) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

            putIfNotNull(props, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaCustomProps.get("ssl.truststore.password"));
            putIfNotNull(props, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kafkaCustomProps.get("ssl.keystore.password"));
            putIfNotNull(props, SslConfigs.SSL_KEY_PASSWORD_CONFIG, kafkaCustomProps.get("ssl.key.password"));

            // Convert classpath resources to absolute physical file paths for Kafka
            try {
                String truststoreLocation = kafkaCustomProps.get("ssl.truststore.location");
                if (truststoreLocation != null) {
                    props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
                            ResourceUtils.getFile(truststoreLocation).getAbsolutePath());
                }

                String keystoreLocation = kafkaCustomProps.get("ssl.keystore.location");
                if (keystoreLocation != null) {
                    props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
                            ResourceUtils.getFile(keystoreLocation).getAbsolutePath());
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Could not find the SSL certificate file inside resources!", e);
            }
        }
    }

    private void putIfNotNull(Map<String, Object> targetMap, String kafkaConfigKey, Object value) {
        if (value != null) {
            targetMap.put(kafkaConfigKey, value);
        }
    }

    // PRODUCER CONFIG
    // =========================

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getResolvedBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        if (kafkaProperties.getProducer() != null && kafkaProperties.getProducer().getAcks() != null) {
            props.put(ProducerConfig.ACKS_CONFIG, kafkaProperties.getProducer().getAcks());
        } else {
            props.put(ProducerConfig.ACKS_CONFIG, "1");
        }

        applySslConfigs(props);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // CONSUMER CONFIG
    // =========================

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getResolvedBootstrapServers());

        if (kafkaProperties.getConsumer() != null) {
            putIfNotNull(props, ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());
            putIfNotNull(props, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getConsumer().getAutoOffsetReset());
            putIfNotNull(props, ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaProperties.getConsumer().getEnableAutoCommit());
            if (kafkaProperties.getConsumer().getMaxPollRecords() != null) {
                props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.getConsumer().getMaxPollRecords());
            }
        }

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        applySslConfigs(props);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // LISTENER FACTORY
    // =========================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        if (kafkaProperties.getListener() != null && kafkaProperties.getListener().getAckMode() != null) {
            factory.getContainerProperties().setAckMode(kafkaProperties.getListener().getAckMode());
        }

        return factory;
    }
}