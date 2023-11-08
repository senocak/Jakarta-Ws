package com.github.senocak.jakartaws

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.websocket.Decoder
import jakarta.websocket.Encoder
import jakarta.websocket.EndpointConfig
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint
import java.util.concurrent.ConcurrentHashMap
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.socket.server.standard.ServerEndpointExporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private val om = ObjectMapper()

@SpringBootApplication
@RestController
class JakartawsApplication {
    @Bean
    fun serverEndpointExporter(): ServerEndpointExporter = ServerEndpointExporter()

    @GetMapping
    fun ping(): String = "ping"
}

fun main(args: Array<String>) {
    runApplication<JakartawsApplication>(*args)
}

@Component
@ServerEndpoint(value = "/chat/{username}", decoders = [MessageDecoder::class], encoders = [MessageEncoder::class])
class ServerApp {

    @OnOpen
    fun onOpen(session: Session, @PathParam("username") username: String) {
        onlineSessions[username] = session
        val message: Message = Message()
            .also {
                it.type = "CONNECT"
                it.username = username
            }
        log.info("$username connected.")
        val userSet: Set<String> = onlineSessions.keys
        message.connectedUsers = userSet.toTypedArray<String>()
        sendMessageToAll(msg = message)
    }

    @OnMessage
    fun onMessage(message: Message) {
        message.type = "SPEAK"
        sendMessageToAll(msg = message)
    }

    @OnClose
    fun onClose(@PathParam("username") username: String) {
        onlineSessions.remove(key = username)
        val message: Message = Message()
            .also {
                it.type = "DISCONNECT"
                it.username = username
            }
        log.info("$username disconnected.")
        sendMessageToAll(msg = message)
    }

    @OnError
    fun onError(session: Session?, error: Throwable) {
        println(error.message)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this.javaClass)
        var onlineSessions: MutableMap<String, Session> = ConcurrentHashMap<String, Session>()
        private fun sendMessageToAll(msg: Message) {
            for (session: Session in onlineSessions.values) {
                try {
                    session.basicRemote.let {
                        it.sendObject(msg)
                        log.info("Message send to $it")
                    }
                } catch (e: Exception) {
                    log.error(e.message)
                }
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
class Message {
    var type: String = ""
    var username: String = ""
    val msg: String? = null
    var connectedUsers: Array<String>? = null
}
class MessageEncoder : Encoder.Text<Message?> {
    override fun encode(msg: Message?): String = om.writeValueAsString(msg)
    override fun init(endpointConfig: EndpointConfig) {}
    override fun destroy() {}
}
class MessageDecoder : Decoder.Text<Message?> {
    override fun decode(s: String?): Message = om.readValue(s, Message::class.java)
    override fun willDecode(s: String?): Boolean = s != null
    override fun init(endpointConfig: EndpointConfig) {}
    override fun destroy() {}
}