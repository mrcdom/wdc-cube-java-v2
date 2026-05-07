package br.com.wdc.framework.commons.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log factory that delegates to SLF4J.
 * <p>
 * Best for server-side and desktop JVM applications where Logback
 * (or another SLF4J provider) is available on the classpath.
 * <p>
 * Usage:
 * <pre>
 *   Log.setFactory(new Slf4jLogFactory());
 * </pre>
 */
public final class Slf4jLogFactory implements Log.Factory {

    @Override
    public Log create(String name) {
        Logger logger = LoggerFactory.getLogger(name);
        var log = new Log(name);
        log.setErrorOut((msg, t) -> {
            if (t != null) {
                logger.error(msg, t);
            } else {
                logger.error(msg);
            }
        });
        log.setWarnOut((msg, t) -> {
            if (t != null) {
                logger.warn(msg, t);
            } else {
                logger.warn(msg);
            }
        });
        log.setInfoOut((msg, t) -> {
            if (t != null) {
                logger.info(msg, t);
            } else {
                logger.info(msg);
            }
        });
        log.setDebugOut((msg, t) -> {
            if (t != null) {
                logger.debug(msg, t);
            } else {
                logger.debug(msg);
            }
        });
        log.setTraceOut((msg, t) -> {
            if (t != null) {
                logger.trace(msg, t);
            } else {
                logger.trace(msg);
            }
        });
        return log;
    }
}
