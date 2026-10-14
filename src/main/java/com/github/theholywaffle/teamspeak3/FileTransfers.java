package com.github.theholywaffle.teamspeak3;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/** Resources shared by the file transfers of one query. */
record FileTransfers(FileTransferHelper helper, Semaphore admission, Executor callbacks) {}
