package com.lxe.lx.service;

import com.lxe.lx.domain.dto.LingXiEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderedEventBufferTest {

    @Test
    void bridgesReplayAndLiveEventsWithoutGapsOrDuplicates() {
        List<Long> sent = new ArrayList<>();
        OrderedEventBuffer buffer = new OrderedEventBuffer(2, event -> sent.add(event.getSequence()));

        buffer.addLive(event(5));
        buffer.addReplay(event(3));
        buffer.addLive(event(4));
        buffer.addReplay(event(4));
        buffer.finishReplay();

        assertEquals(Arrays.asList(3L, 4L, 5L), sent);
    }

    @Test
    void waitsForMissingSequenceBeforeSendingLaterLiveEvent() {
        List<Long> sent = new ArrayList<>();
        OrderedEventBuffer buffer = new OrderedEventBuffer(0, event -> sent.add(event.getSequence()));
        buffer.finishReplay();

        buffer.addLive(event(2));
        assertEquals(0, sent.size());
        buffer.addLive(event(1));

        assertEquals(Arrays.asList(1L, 2L), sent);
    }

    private LingXiEvent event(long sequence) {
        LingXiEvent event = new LingXiEvent();
        event.setSequence(sequence);
        return event;
    }
}
