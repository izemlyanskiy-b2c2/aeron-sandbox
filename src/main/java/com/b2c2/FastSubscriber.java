package com.b2c2;

import io.aeron.Aeron;
import io.aeron.ChannelUri;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class FastSubscriber {

    public static final String CHANNEL = new ChannelUriStringBuilder(CommonContext.IPC_CHANNEL)
            .alias("fast")
            .build();
    public static final FragmentAssembler FRAGMENT_HANDLER = new FragmentAssembler(
            (directBuffer, offset, length, header) -> {
                System.out.println("Got a message on position " + header.position());
            });

    public static void main(String[] args) {
        try (Aeron connect = Aeron.connect()) {
            ChannelUri channelUri = ChannelUri.parse(CHANNEL);
            String resultChannel =
                    new ChannelUriStringBuilder(CHANNEL).alias(channelUri.get(CommonContext.ALIAS_PARAM_NAME)
                                    + "("
                                    + connect.clientId()
                                    + ")")
                            .build();
            System.out.println(resultChannel);
            Subscription subscription = connect.addSubscription(resultChannel, Publisher.STREAM_ID);

            while (!Thread.currentThread().isInterrupted()) {
                subscription.poll(FRAGMENT_HANDLER, Integer.MAX_VALUE);
                LockSupport.parkNanos(Publisher.PUBLISHER_TIMEOUT);
            }
        }
    }
}
