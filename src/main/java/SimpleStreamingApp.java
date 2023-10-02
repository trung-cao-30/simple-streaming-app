import com.sun.net.httpserver.HttpServer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

public class SimpleStreamingApp {

    private static final String TOPIC = "users";
    private static final String USER_SCHEMA = "user.avsc";


    public static Properties loadConfig(final String configFile) throws IOException {
        if (!Files.exists(Paths.get(configFile))) {
            throw new IOException(configFile + " not found.");
        }
        final Properties cfg = new Properties();
        try (InputStream inputStream = new FileInputStream(configFile)) {
            cfg.load(inputStream);
        }
        return cfg;
    }

    public static void main(String[] args) throws IOException {

        final Properties properties = loadConfig("client.properties");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        HttpServer healthServer = HttpServer.create(new InetSocketAddress(8000), 0);
        healthServer.createContext("/health", httpExchange -> {
            String response = "Server is healthy!";
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });
        // Start the server
        healthServer.start();

        KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(properties);
        try {
            Schema schema = new Schema.Parser().parse(loadSchemaFromFile(USER_SCHEMA));
            while (true) {
                Thread.sleep(30000);
                GenericRecord avroRecord = new GenericData.Record(schema);
                avroRecord.put("name", "John");
                avroRecord.put("age", 25);
                avroRecord.put("email", "john@example.com");
                ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(TOPIC, avroRecord);
                producer.send(record, (recordMetadata, exception) -> {
                    if (exception == null) {
                        System.out.println("Record written to offset " +
                                recordMetadata.offset() + " timestamp " +
                                recordMetadata.timestamp());
                    } else {
                        System.err.println("An error occurred");
                        exception.printStackTrace(System.err);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }

    private static String loadSchemaFromFile(String filepath) {
        InputStream inputStream = SimpleStreamingApp.class.getResourceAsStream(filepath);
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
