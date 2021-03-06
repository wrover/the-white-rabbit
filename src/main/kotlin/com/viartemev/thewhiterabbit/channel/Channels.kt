package com.viartemev.thewhiterabbit.channel

import com.rabbitmq.client.Channel
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

object Channels {
    internal val localConfirmChannels = ConcurrentHashMap<Thread, UncloseableConfirmChannel>()
    internal val localChannels = ConcurrentHashMap<Thread, UncloseableChannel>()

    init {
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            sequenceOf(localChannels.values.asSequence(), localConfirmChannels.values.asSequence())
                .flatten()
                .forEach { it.close0() }
            localChannels.clear()
            localConfirmChannels.clear()
        })
    }


    private interface IAmUncloseableChannel : Channel {
        fun close0()
    }

    internal class UncloseableChannel(private val channel: Channel) : IAmUncloseableChannel, Channel by channel {
        override fun close() {}

        override fun close(closeCode: Int, closeMessage: String?) {}

        override fun close0() {
            if (channel.isOpen) channel.close()
        }
    }

    internal class UncloseableConfirmChannel(private val channel: ConfirmChannel) : ConfirmChannel(channel),
        IAmUncloseableChannel {
        override fun close() {}

        override fun close(closeCode: Int, closeMessage: String?) {}

        override fun close0() {
            if (channel.isOpen) channel.close()
        }
    }
}

