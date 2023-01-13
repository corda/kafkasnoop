package kafkasnoop.dto

import com.papsign.ktor.openapigen.annotations.Response

@Response("Kafka Topic Partition")
data class Partition(
    val index: Int,
    val beginOffset: Long,
    val endOffset: Long,
    val inSyncReplicas: Int,
    val offlineReplicas: Int,
)

data class ApiUrls(val get: String, val websocket: String)

@Response("Kafka Topic")
data class Topic(val name: String, val partitions: List<Partition>, val urls: ApiUrls)

data class TopicInfo(val name: String, val partitions: List<Partition>)
