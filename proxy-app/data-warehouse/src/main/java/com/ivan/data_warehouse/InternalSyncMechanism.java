package com.ivan.data_warehouse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InternalSyncMechanism {

    private static InternalSyncMechanism singleton = new InternalSyncMechanism();

    private AtomicInteger usersCountIsServedNow = new AtomicInteger(0);
    private AtomicBoolean isTransaction = new AtomicBoolean(false);

    private InternalSyncMechanism() {

    }

    public static InternalSyncMechanism getInstance() {
        return singleton;
    }

    public AtomicBoolean isTransaction() {
        return isTransaction;
    }

    public AtomicInteger getUsersCountIsServedNow() {
        return usersCountIsServedNow;
    }
}
