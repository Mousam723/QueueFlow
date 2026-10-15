package com.queueflow.queue;

public enum QueueStatus {

    ACTIVE,
    PAUSED,
    CLOSED;

    public boolean canTransitionTo(QueueStatus target) {
        return switch (this) {
            case ACTIVE -> target == PAUSED || target == CLOSED;
            case PAUSED -> target == ACTIVE || target == CLOSED;
            case CLOSED -> false;
        };
    }
}