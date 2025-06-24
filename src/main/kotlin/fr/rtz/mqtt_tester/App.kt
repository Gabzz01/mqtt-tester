@file:OptIn(ExperimentalSerializationApi::class)

package fr.rtz.mqtt_tester

import io.github.oshai.kotlinlogging.KotlinLogging
import io.vertx.core.Vertx
import io.vertx.mqtt.MqttClient
import io.vertx.mqtt.MqttClientOptions
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonElement

private val logger = KotlinLogging.logger { }

private const val HOST = ""
private const val PORT = -1
private const val USR = ""
private const val PSSWD = ""
private const val TOPIC = ""

fun main() {
    val vertx = Vertx.vertx()
    val options = MqttClientOptions()
        .setUsername(USR)
        .setPassword(PSSWD)
        .setSsl(true)
        //.setTrustAll(true)
        .setHostnameVerificationAlgorithm("HTTPS")

    val client = MqttClient.create(vertx, options)
    logger.info { "Connecting to MQTT broker..." }
    client.authenticationExchangeHandler({ auth ->
        //The handler will be called time to time by default
        logger.info { "We have just received AUTH packet: " + auth.reasonCode() }
    })
    client.pingResponseHandler { s ->
        //The handler will be called time to time by default
        logger.info { "We have just received PINGRESP packet" }
    }
    client.connect(PORT, HOST).await()
    logger.info { "Successfully connected to MQTT broker." }
    client
        .publishHandler { s ->
            logger.info { "Received msg in topic '${s.topicName()}' : ${s.payload()}" }
            val json = try {
                Json.decodeFromString(MsgPayload.serializer(), s.payload().toString())
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse message payload" }
            }
            logger.info { "Succesfully parsed $json" }
        }
        .subscribe(TOPIC, 1)
        .onSuccess { subAck ->
            logger.info { "Subscribed to topic '$TOPIC' with QoS 1. SubAck: $subAck" }
        }
        .onFailure { throwable ->
            logger.error(throwable) { "Failed to subscribe to topic $TOPIC" }
        }
    /*
    client.publishCompletionHandler {
        println("Message published successfully. $it")
    }
    client.publish(
        "gw-cg/SN-OptimaGate/up",
        Buffer.buffer("Hello from MQTT Tester".toByteArray()),
        MqttQoS.EXACTLY_ONCE,
        false,
        false
    )
     */
}

@Serializable
@JsonIgnoreUnknownKeys
data class MsgPayload(
    val timestamp: Long,
    val deviceId: String,
    val payload: JsonElement,
    val port: Int? = null,
)