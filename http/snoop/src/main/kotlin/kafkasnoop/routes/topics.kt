package kafkasnoop.routes

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import kafkasnoop.KafkaClientFactory
import kafkasnoop.dto.ApiUrls
import kafkasnoop.dto.Partition
import kafkasnoop.dto.Topic
import kafkasnoop.dto.TopicInfo
import org.apache.kafka.common.TopicPartition
import java.net.URLEncoder

fun NormalOpenAPIRoute.topics(kafkaClientFactory: KafkaClientFactory) {
    get<Unit, List<Topic>>(
        info("Topics", "Get Topics and Partition Details"),
        example = listOf(
            Topic(
                "foo-topic",
                listOf(
                    Partition(
                        0,
                        0,
                        123,
                        2,
                        1
                    )
                ),
                ApiUrls("/api/topics/foo-topic", "/ws/topics/foo-topic")
            )
        )
    ) {
        kafkaClientFactory
            .createConsumer().use {
                respond(
                    it.listTopics()
                        .map { t ->
                            val partitions = t.value.map { p -> TopicPartition(t.key, p.partition()) }
                            val beggingOffsets = it.beginningOffsets(partitions)
                                .map { o -> o.key.partition() to o.value }.toMap()
                            val endOffsets = it.endOffsets(partitions)
                                .map { o -> o.key.partition() to o.value }.toMap()

                            val url = "topics/${URLEncoder.encode(t.key, Charsets.UTF_8)}"

                            Topic(
                                t.key,
                                t.value.map { p ->
                                    Partition(
                                        p.partition(),
                                        beggingOffsets.getOrDefault(p.partition(), 0L),
                                        endOffsets.getOrDefault(p.partition(), 0L),
                                        p.inSyncReplicas().count(),
                                        p.offlineReplicas().count()
                                    )
                                },
                                ApiUrls(
                                    "/api/$url",
                                    "/ws/$url",
                                )
                            )
                        }
                )
            }
    }
}

data class GetTopicInfoParams(
    @PathParam("Name of the topic")
    val topic: String,
)
fun NormalOpenAPIRoute.topicInfo(kafkaClientFactory: KafkaClientFactory) {
    get<GetTopicInfoParams, TopicInfo>(
        info("Topic info", "Get partitions info for a topic"),
        example =
        TopicInfo(
            "foo-topic",
            listOf(
                Partition(
                    0,
                    0,
                    123,
                    2,
                    1
                )
            )
        )
    ) { params ->
        kafkaClientFactory
            .createConsumer().use {
                respond(
                    it.partitionsFor(params.topic).run {
                        val topicPartitions = this.map { p -> TopicPartition(params.topic, p.partition()) }
                        val beginningOffsets = it.beginningOffsets(topicPartitions)
                            .map { o -> o.key.partition() to o.value }.toMap()
                        val endOffsets = it.endOffsets(topicPartitions)
                            .map { o -> o.key.partition() to o.value }.toMap()

                        val partitions = this.map { p ->
                            Partition(
                                p.partition(),
                                beginningOffsets.getOrDefault(p.partition(), 0L),
                                endOffsets.getOrDefault(p.partition(), 0L),
                                p.inSyncReplicas().count(),
                                p.offlineReplicas().count()
                            )
                        }
                        TopicInfo(params.topic, partitions)
                    }
                )
            }
    }
}
