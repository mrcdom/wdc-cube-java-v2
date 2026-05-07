package br.com.wdc.framework.commons.log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log factory that delegates to {@code java.util.logging} (JUL).
 * <p>
 * Best for GraalVM native-image targets (Android, iOS) where JUL is
 * built into the JDK and requires no reflection or service-loading.
 * On Android, JUL output is routed to logcat. On iOS, it goes to the
 * system console.
 * <p>
 * Usage:
 * <pre>
 *   Log.setFactory(new JulLogFactory());
 * </pre>
 */
public final class JulLogFactory implements Log.Factory {

    @Override
    public Log create(String name) {
        Logger logger = Logger.getLogger(name);
        var log = new Log(name);
        log.setErrorOut((msg, t) -> logger.log(Level.SEVERE, msg, t));
        log.setWarnOut((msg, t) -> logger.log(Level.WARNING, msg, t));
        log.setInfoOut((msg, t) -> logger.log(Level.INFO, msg, t));
        log.setDebugOut((msg, t) -> logger.log(Level.FINE, msg, t));
        log.setTraceOut((msg, t) -> logger.log(Level.FINEST, msg, t));
        return log;
    }
}
