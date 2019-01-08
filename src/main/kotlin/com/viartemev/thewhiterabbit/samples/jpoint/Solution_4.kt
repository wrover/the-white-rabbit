package com.viartemev.thewhiterabbit.samples.jpoint

import awaitString
import com.github.kittinunf.fuel.Fuel
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.viartemev.thewhiterabbit.channel.ConfirmChannel
import com.viartemev.thewhiterabbit.publisher.OutboundMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.LongAdder
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val factory = ConnectionFactory()
    factory.useNio()
    factory.host = "localhost"
    factory.newConnection().use { connection ->
        connection.createChannel().use { channel ->
            ConfirmChannel(channel).let {
                val ch = it
                val queue = "test"
                channel.queueDeclare(queue, false, false, false, null)
                val counter = LongAdder()
                val measureTimeMillis = measureTimeMillis {
                    runBlocking {
                        repeat(999) {
                            launch {
                                val message = Fuel.get("http://localhost:8081/message").awaitString() + " $it"
                                val outboundMessage = OutboundMessage("", queue, MessageProperties.PERSISTENT_BASIC, message.toByteArray(charset("UTF-8")))
                                val ack = ch.publisher().publish(outboundMessage)
                                counter.increment()
                                println(" [x] Sent '$message' ack: $ack")
                            }
                        }
                    }
                }

                println(measureTimeMillis)
                println(counter.sumThenReset())
            }
        }
    }
    println("Done")
}