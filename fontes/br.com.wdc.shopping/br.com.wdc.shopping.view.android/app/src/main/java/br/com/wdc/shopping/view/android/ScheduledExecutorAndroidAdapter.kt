package br.com.wdc.shopping.view.android

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor
import br.com.wdc.framework.commons.function.Registration
import br.com.wdc.framework.commons.function.ThrowingRunnable
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Adapter that schedules tasks on a ScheduledExecutorService.
 * Commands execute in the thread pool (allowing network I/O).
 * UI updates are triggered via Compose's mutableState observation (thread-safe).
 */
class ScheduledExecutorAndroidAdapter(
    private val service: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
) : ScheduledExecutor {

    override fun execute(command: ThrowingRunnable): Registration {
        val future = service.submit(command)
        return Registration { future.cancel(true) }
    }

    override fun schedule(command: ThrowingRunnable, delay: Duration): Registration {
        val future = service.schedule(command, delay.toMillis(), TimeUnit.MILLISECONDS)
        return Registration { future.cancel(true) }
    }

    override fun scheduleAtFixedRate(command: ThrowingRunnable, initialDelay: Duration, period: Duration): Registration {
        val future = service.scheduleAtFixedRate(
            command,
            initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS
        )
        return Registration { future.cancel(true) }
    }

    override fun scheduleWithFixedDelay(command: ThrowingRunnable, initialDelay: Duration, delay: Duration): Registration {
        val future = service.scheduleWithFixedDelay(
            command,
            initialDelay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS
        )
        return Registration { future.cancel(true) }
    }

    fun shutdown() {
        service.shutdownNow()
    }
}
