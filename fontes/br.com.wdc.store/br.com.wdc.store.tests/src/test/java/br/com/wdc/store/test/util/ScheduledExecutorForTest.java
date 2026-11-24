package br.com.wdc.store.test.util;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;

@SuppressWarnings("java:S112")
public interface ScheduledExecutorForTest extends ScheduledExecutor {

    void flush() throws Exception;

    void shutdown();

}
