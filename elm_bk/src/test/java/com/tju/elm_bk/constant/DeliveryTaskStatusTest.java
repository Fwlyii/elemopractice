package com.tju.elm_bk.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryTaskStatusTest {

    @Test
    void shouldAllowHappyPathTransitions() {
        assertTrue(DeliveryTaskStatus.WAITING_RIDER.canTransitionTo(DeliveryTaskStatus.ACCEPTED));
        assertTrue(DeliveryTaskStatus.ACCEPTED.canTransitionTo(DeliveryTaskStatus.ARRIVED_STORE));
        assertTrue(DeliveryTaskStatus.ARRIVED_STORE.canTransitionTo(DeliveryTaskStatus.DELIVERING));
        assertTrue(DeliveryTaskStatus.DELIVERING.canTransitionTo(DeliveryTaskStatus.DELIVERED));
        assertTrue(DeliveryTaskStatus.DELIVERED.canTransitionTo(DeliveryTaskStatus.COMPLETED));
    }

    @Test
    void shouldRejectSkippedOrTerminalTransitions() {
        assertFalse(DeliveryTaskStatus.ACCEPTED.canTransitionTo(DeliveryTaskStatus.DELIVERED));
        assertFalse(DeliveryTaskStatus.COMPLETED.canTransitionTo(DeliveryTaskStatus.DELIVERING));
        assertFalse(DeliveryTaskStatus.CANCELLED.canTransitionTo(DeliveryTaskStatus.ACCEPTED));
    }

    @Test
    void shouldAllowExceptionRecovery() {
        assertTrue(DeliveryTaskStatus.DELIVERING.canTransitionTo(DeliveryTaskStatus.EXCEPTION));
        assertTrue(DeliveryTaskStatus.EXCEPTION.canTransitionTo(DeliveryTaskStatus.DELIVERING));
        assertTrue(DeliveryTaskStatus.EXCEPTION.canTransitionTo(DeliveryTaskStatus.WAITING_RIDER));
    }
}
