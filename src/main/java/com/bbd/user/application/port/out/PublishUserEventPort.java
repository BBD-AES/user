package com.bbd.user.application.port.out;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/*
 Publishes a serialized user event to the external event broker.
 */
public interface PublishUserEventPort {

    void publish(String eventKey, String payload)
            throws InterruptedException, ExecutionException, TimeoutException;
}
