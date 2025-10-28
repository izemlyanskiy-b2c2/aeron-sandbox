package com.b2c2;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;

import java.util.concurrent.locks.LockSupport;

public class SlowSubscriber {

    public static final String CHANNEL = new ChannelUriStringBuilder(CommonContext.IPC_CHANNEL)
            .tether(false)
            .alias("slow")
            .build();

    public static void main(String[] args) {
        try (Aeron connect = Aeron.connect()) {
            Subscription slowSubscription = connect.addSubscription(CHANNEL, Publisher.STREAM_ID);

            while (!Thread.currentThread().isInterrupted()) {
                int poll = slowSubscription.poll(
                        new FragmentAssembler(
                                (directBuffer, offset, length, header) -> {
                                    System.out.println(directBuffer.getStringAscii(offset, length));
                                    System.out.println("Got a message on position " + header.position());
                                }), 1);
                if (slowSubscription.isConnected()) {
                    System.out.println("polled " + poll + " fragments" + slowSubscription.imageAtIndex(0).toString());
                } else {
                    System.out.println("NOT connected");
                }
                LockSupport.parkNanos(Publisher.PUBLISHER_TIMEOUT * 10);
            }
        }
    }
}
