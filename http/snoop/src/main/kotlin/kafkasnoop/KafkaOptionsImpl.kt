package kafkasnoop

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import java.util.Properties

class KafkaOptionsImpl : KafkaOptions, OptionGroup("Message Envelope Options") {
    override val brokerAddress: String by option("-b", "--broker", help = "Kafka broker address")
        .default("localhost:9092")
    override val kafkaCliOptions: Map<String, String> by
    option("-k", "--kafka-prop", help = "Optional Kafka client properties")
        .associate()

    override val clientProperties: Properties by lazy {
        (
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokerAddress,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "100",
                ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed"
            ) + kafkaCliOptions
            ).toProperties()
    }
}
