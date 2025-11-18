package com.b2c2.sessions;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Strategy implements Agent, FragmentHandler {

    public static final long PUBLISHER_TIMEOUT_MS = 700;
    public static final String CHANNEL = CommonContext.IPC_CHANNEL;
    public static final int STREAM_ID = 1;

    private final DirectBuffer buffer;
    private final int id;
    private final Publication publication;
    private final Subscription subscription;
    private long publishDeadLine = 0;

    public Strategy(int id, Aeron connect) {
        this.id = id;
        String data = "[test message from strategy " + id + "]";
        this.buffer = new UnsafeBuffer(data.getBytes());

        var channelBuilder = new ChannelUriStringBuilder(CHANNEL);
        if (id != 0) {
            channelBuilder.sessionId(id);
        }

        publication = connect.addExclusivePublication(
                channelBuilder.build(),
                STREAM_ID);
        subscription = connect.addSubscription(channelBuilder.build(), STREAM_ID);
    }

    private int tryPublish() {
        long now = System.currentTimeMillis();
        if (now > publishDeadLine) {
            long position = publication.offer(buffer, 0, buffer.capacity());
            if (position >= 0) {
                System.out.println("published message on position " + position);
            } else {
                System.out.println(Publication.errorString(position));
            }
            publishDeadLine = now + PUBLISHER_TIMEOUT_MS;
            return 1;
        }
        return 0;
    }


    private int consume() {
        return subscription.poll(new FragmentAssembler(this), 100);
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        System.out.println("[Strategy " + id + "] Got a message "
                + buffer.getStringWithoutLengthAscii(offset, length) + " on position " + header.position());
    }

    @Override
    public int doWork()  {
        return tryPublish() + consume();
    }

    @Override
    public String roleName() {
        return "strategy" + id;
    }

    public static void main(String[] args) {
        try (Aeron connect = Aeron.connect()) {
            Strategy strategy1 = new Strategy(1, connect);
            Strategy strategy2 = new Strategy(2, connect);
            IdleStrategy idleStrategy = new SleepingMillisIdleStrategy(1);
            while (!Thread.currentThread().isInterrupted()) {
                idleStrategy.idle(strategy1.doWork() + strategy2.doWork());
            }
        }
    }
}
