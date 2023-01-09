package kafkasnoop.routes

import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.Message
import kafkasnoop.serialisation.MessageDeserialiser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.time.Instant
import kotlin.math.max

/**
 * Message processor
 *
 * NOTE: changed this to use 1 consumer per partition.
 * According to the docs, reading assigning multiple partitions to a consumer without a cosnumer group should
 * be fine, but it appeared unreliable. This probably means something was wrong in my code, but will come back to this.
 */
class MessageProcessor(
    private val kafkaClientFactory: KafkaClientFactory,
    private val topicName: String,
    private val messageDeserialiser: MessageDeserialiser,
) : AutoCloseable {
    val partitions: List<TopicPartition>
    @Volatile
    private var isClosed = false
    init {
        logger.debug("Getting messages for $topicName")
        kafkaClientFactory.createConsumer().use {
            partitions = it.partitionsFor(topicName).map {
                TopicPartition(it.topic(), it.partition())
            }
        }
    }

    fun startProcess(partition: TopicPartition, maxMsgCount: Int = Int.MAX_VALUE, minOffset: Long? = null): Flow<Message> =
        flow {
            kafkaClientFactory.createConsumer().use { kafkaConsumer ->
                kafkaConsumer.assign(listOf(partition))

                var offset = if (minOffset != null) {
                    minOffset
                } else {
                    val beginningOffsets = kafkaConsumer.beginningOffsets(partitions)
                    val endOffsets = kafkaConsumer.endOffsets(partitions)
                    logger.debug("Min offset for partition $partition is ${beginningOffsets[partition]}")
                    logger.debug("Max offset for partition $partition is ${endOffsets[partition]}")
                    val startOffset = max(endOffsets[partition]?.minus(maxMsgCount) ?: 0L, 0L)
                    startOffset
                }

                logger.info("Trying to load $maxMsgCount from $partition starting at $offset")
                kafkaConsumer.seek(partition, offset)

                var messagesLoaded = 0
                var emptyPolls = 0
                while (!isClosed && emptyPolls <= 5 && messagesLoaded < maxMsgCount) {
                    logger.debug("Polling $partition from ${kafkaConsumer.position(partition)}")
                    val msgs = kafkaConsumer
                        .poll(Duration.ofMillis(200))
                        .records(partition)
                        .map { record ->
                            logger.debug("Found message $partition: ${record.offset()}")
                            val key = messageDeserialiser.deserialise(record.key() ?: ByteArray(0))
                            val value = messageDeserialiser.deserialise(record.value() ?: ByteArray(0))
                            Message(record.offset(), partition.toString(), key, value, Instant.ofEpochMilli(record.timestamp()))
                        }.sortedBy { it.offset }
                        .take(maxMsgCount)

                    if (msgs.isEmpty()) {
                        emptyPolls += 1
                        logger.debug("Empty polls: $emptyPolls")
                        Thread.sleep(200)
                    } else {
                        logger.debug("Found ${msgs.count()} on $topicName: ${msgs.groupBy { it.partition }.map { it.key to it.value.maxOf { it.offset } }.toMap()}")
                        logger.debug("Found $msgs on $partition")
                        msgs.forEach { emit(it) }
                        emptyPolls = 0
                    }
                    messagesLoaded += msgs.count()
                    offset += messagesLoaded
                    kafkaConsumer.seek(partition, offset)
                    logger.debug("Loaded $messagesLoaded out of $maxMsgCount")
                }
                logger.debug("stopping to process")
            }
        }

    override fun close() {
        isClosed = true
    }
}
