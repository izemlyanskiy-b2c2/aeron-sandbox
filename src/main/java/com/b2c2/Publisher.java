package com.b2c2;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.Publication;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Publisher {
    public static final String DATA;
    public static final long PUBLISHER_TIMEOUT = TimeUnit.MILLISECONDS.toNanos(200);
    public static final String CHANNEL = new ChannelUriStringBuilder(CommonContext.IPC_CHANNEL)
            .flowControl("max")
            .untetheredWindowLimitTimeout("0")
            .build();

    static {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append("Veeeeeeeeeeeeeeeeery big message!");
        }
        DATA = sb.toString();

    }
    public static final int STREAM_ID = 1001;
    private static final DirectBuffer buffer = new UnsafeBuffer(DATA.getBytes());

    public static void main(String[] args) {
        System.out.println(CHANNEL);
        try (Aeron connect = Aeron.connect()) {
            Publication publication = connect.addPublication(
                    CHANNEL,
                    STREAM_ID);
            while (!Thread.currentThread().isInterrupted()) {
                long position = publication.offer(buffer, 0, buffer.capacity());
                if (position >= 0) {
                    System.out.println("published message on position " + position);
                } else {
                    System.out.println(Publication.errorString(position));
                }
                LockSupport.parkNanos(PUBLISHER_TIMEOUT);
            }
        }
    }
}