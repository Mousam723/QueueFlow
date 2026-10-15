package com.queueflow.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueStatusTest {

    @Test
    void activeCanTransitionToPaused() {
        assertTrue(QueueStatus.ACTIVE.canTransitionTo(QueueStatus.PAUSED));
    }

    @Test
    void activeCanTransitionToClosed() {
        assertTrue(QueueStatus.ACTIVE.canTransitionTo(QueueStatus.CLOSED));
    }

    @Test
    void activeCannotTransitionToActive() {
        assertFalse(QueueStatus.ACTIVE.canTransitionTo(QueueStatus.ACTIVE));
    }

    @Test
    void pausedCanTransitionToActive() {
        assertTrue(QueueStatus.PAUSED.canTransitionTo(QueueStatus.ACTIVE));
    }

    @Test
    void pausedCanTransitionToClosed() {
        assertTrue(QueueStatus.PAUSED.canTransitionTo(QueueStatus.CLOSED));
    }

    @Test
    void pausedCannotTransitionToPaused() {
        assertFalse(QueueStatus.PAUSED.canTransitionTo(QueueStatus.PAUSED));
    }

    @Test
    void closedCannotTransitionToActive() {
        assertFalse(QueueStatus.CLOSED.canTransitionTo(QueueStatus.ACTIVE));
    }

    @Test
    void closedCannotTransitionToPaused() {
        assertFalse(QueueStatus.CLOSED.canTransitionTo(QueueStatus.PAUSED));
    }

    @Test
    void closedCannotTransitionToClosed() {
        assertFalse(QueueStatus.CLOSED.canTransitionTo(QueueStatus.CLOSED));
    }
}
