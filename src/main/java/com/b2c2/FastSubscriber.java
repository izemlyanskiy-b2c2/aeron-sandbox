package com.b2c2;

import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;

import java.util.concurrent.locks.LockSupport;

public class FastSubscriber {

    public static void main(String[] args) {
        try (Aeron connect = Aeron.connect()) {
            Subscription slowSubscription = connect.addSubscription(CommonContext.IPC_CHANNEL, Publisher.STREAM_ID);

            while (!Thread.currentThread().isInterrupted()) {
                slowSubscription.poll(new FragmentAssembler(
                        (directBuffer, offset, length, header) -> {
                            System.out.println("Got a message on position " + header.position());
                        }), Integer.MAX_VALUE);
                LockSupport.parkNanos(Publisher.PUBLISHER_TIMEOUT);
            }
        }
    }
}
