package com.b2c2.sessions;

import com.b2c2.Publisher;

import io.aeron.Aeron;
import io.aeron.ChannelUri;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import org.agrona.concurrent.BackoffIdleStrategy;

import java.util.concurrent.locks.LockSupport;

public class CommonSubscriber {

    public static final FragmentAssembler FRAGMENT_HANDLER = new FragmentAssembler(
            (directBuffer, offset, length, header) -> {
                System.out.println("[Subscriber] Got a message"+ directBuffer.getStringWithoutLengthAscii(offset, length)+" on position " + header.position());
            });

    public static void main(String[] args) {
        try (Aeron connect = Aeron.connect()) {
            System.out.println(Strategy.CHANNEL);
            Subscription subscription = connect.addSubscription(Strategy.CHANNEL, Strategy.STREAM_ID);

            BackoffIdleStrategy idleStrategy = new BackoffIdleStrategy();
            while (!Thread.currentThread().isInterrupted()) {
                idleStrategy.idle(subscription.poll(FRAGMENT_HANDLER, Integer.MAX_VALUE));
            }
        }
    }
}
