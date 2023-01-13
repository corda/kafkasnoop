package kafkasnoop.routes

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import kafkasnoop.KafkaClientFactory
import kafkasnoop.serialisation.MessageDeserialiser

fun NormalOpenAPIRoute.snoopApiRoutes(
    kafkaClientFactory: KafkaClientFactory,
    messageDeserialiser: MessageDeserialiser
) {
    route("/api/topics").topics(kafkaClientFactory)
    route("/api/topics/{topic}/info").topicInfo(kafkaClientFactory)
    route("/api/topics/{topic}").messages(kafkaClientFactory, messageDeserialiser)
}
