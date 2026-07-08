//package com.project.auto_complete_service.config;
//
//import lombok.RequiredArgsConstructor;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//@RequiredArgsConstructor
//public class KafkaConfig {
//
//    // Automatically reads everything from application.yml
//    private final KafkaProperties kafkaProperties;
//
//    // PRODUCER CONFIG
//    // =========================
//
//    @Bean
//    public ProducerFactory<String, String> producerFactory() {
//
//        Map<String, Object> props = new HashMap<>();
//
//        // values from yml
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
//                kafkaProperties.getBootstrapServers());
//
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
//                StringSerializer.class);
//
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
//                StringSerializer.class);
//
//        props.put(ProducerConfig.ACKS_CONFIG,
//                kafkaProperties.getProducer().getAcks());
//
//        return new DefaultKafkaProducerFactory<>(props);
//    }
//
//    @Bean
//    public KafkaTemplate<String, String> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//
//    // CONSUMER CONFIG
//
//
//    @Bean
//    public ConsumerFactory<String, String> consumerFactory() {
//
//        Map<String, Object> props = new HashMap<>();
//
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
//                kafkaProperties.getBootstrapServers());
//
//        props.put(ConsumerConfig.GROUP_ID_CONFIG,
//                kafkaProperties.getConsumer().getGroupId());
//
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
//                kafkaProperties.getConsumer().getAutoOffsetReset());
//
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
//                kafkaProperties.getConsumer().getEnableAutoCommit());
//
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
//                StringDeserializer.class);
//
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
//                StringDeserializer.class);
//
//        return new DefaultKafkaConsumerFactory<>(props);
//    }
//
//    // =========================
//    // LISTENER FACTORY
//    // =========================
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, String>
//    kafkaListenerContainerFactory() {
//
//        ConcurrentKafkaListenerContainerFactory<String, String> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//
//        factory.setConsumerFactory(consumerFactory());
//
//        // manual acknowledgment
//        factory.getContainerProperties().setAckMode(
//                kafkaProperties.getListener().getAckMode()
//        );
//
//        return factory;
//    }
//}