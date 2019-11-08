package com.reljicd.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class Producer implements CommandLineRunner  {

	
	private static final Logger logger=LoggerFactory.getLogger(Producer.class);
	public void sendkafkaMessage(String x) {
		System.out.println(x);
	}
	public void sendKafkaMessage(String payload,	        
	         String topic)
	{
		String bootstrapServers="127.0.0.1:9092";
		Properties prop=new Properties();
		prop.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		prop.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		prop.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
		
		
		KafkaProducer<String, String> producer=new KafkaProducer<String, String>(prop);
	    logger.info("Sending Kafka message: " + payload);
	    producer.send(new ProducerRecord<>(topic, payload));
	    
	    //flush data
		producer.flush();
		//flush and close
		producer.close();
	}
	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
