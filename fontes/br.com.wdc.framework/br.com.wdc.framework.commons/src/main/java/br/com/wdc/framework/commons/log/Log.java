package br.com.wdc.framework.commons.log;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Lightweight logging facade compatible with all runtimes (desktop, Android, iOS, TeaVM).
 * <p>
 * No reflection, no service-loading, no java.util.concurrent blocking structures.
 * <p>
 * Usage:
 * <pre>
 *   private static final Log LOG = Log.getLogger(MyClass.class);
 *   LOG.warn("something happened: {}", detail);
 * </pre>
 */
public final class Log {

    public enum Level {
        ERROR, WARN, INFO, DEBUG, TRACE
    }

    @FunctionalInterface
    public interface Factory {
        Log create(String name);
    }

    // :: Configuration

    private static final AtomicReference<Factory> FACTORY = new AtomicReference<>(Log::defaultCreate);
    private static volatile Level globalLevel = Level.DEBUG;

    public static void setFactory(Factory factory) {
        FACTORY.set(factory != null ? factory : Log::defaultCreate);
    }

    public static void setGlobalLevel(Level level) {
        globalLevel = level;
    }

    // :: Factory methods

    public static Log getLogger(Class<?> clazz) {
        return FACTORY.get().create(clazz.getName());
    }

    public static Log getLogger(String name) {
        return FACTORY.get().create(name);
    }

    // :: Instance

    private final String name;
    private BiConsumer<String, Throwable> errorOut;
    private BiConsumer<String, Throwable> warnOut;
    private BiConsumer<String, Throwable> infoOut;
    private BiConsumer<String, Throwable> debugOut;
    private BiConsumer<String, Throwable> traceOut;

    public Log(String name) {
        this.name = name;
    }

    // :: Public API

    public void error(String msg) {
        log(Level.ERROR, msg, (Object[]) null, null);
    }

    public void error(String msg, Object... args) {
        Throwable t = extractThrowable(args);
        log(Level.ERROR, msg, args, t);
    }

    public void error(String msg, Throwable t) {
        log(Level.ERROR, msg, null, t);
    }

    public void warn(String msg) {
        log(Level.WARN, msg, (Object[]) null, null);
    }

    public void warn(String msg, Object... args) {
        Throwable t = extractThrowable(args);
        log(Level.WARN, msg, args, t);
    }

    public void warn(String msg, Throwable t) {
        log(Level.WARN, msg, null, t);
    }

    public void info(String msg) {
        log(Level.INFO, msg, (Object[]) null, null);
    }

    public void info(String msg, Object... args) {
        Throwable t = extractThrowable(args);
        log(Level.INFO, msg, args, t);
    }

    public void info(String msg, Throwable t) {
        log(Level.INFO, msg, null, t);
    }

    public void debug(String msg) {
        log(Level.DEBUG, msg, (Object[]) null, null);
    }

    public void debug(String msg, Object... args) {
        Throwable t = extractThrowable(args);
        log(Level.DEBUG, msg, args, t);
    }

    public void debug(String msg, Throwable t) {
        log(Level.DEBUG, msg, null, t);
    }

    public void trace(String msg) {
        log(Level.TRACE, msg, (Object[]) null, null);
    }

    public void trace(String msg, Object... args) {
        Throwable t = extractThrowable(args);
        log(Level.TRACE, msg, args, t);
    }

    public void trace(String msg, Throwable t) {
        log(Level.TRACE, msg, null, t);
    }

    public boolean isDebugEnabled() {
        return globalLevel.ordinal() >= Level.DEBUG.ordinal();
    }

    public boolean isTraceEnabled() {
        return globalLevel.ordinal() >= Level.TRACE.ordinal();
    }

    public String getName() {
        return name;
    }

    // :: Customization (used by Factory implementations)

    public void setErrorOut(BiConsumer<String, Throwable> out) {
        this.errorOut = out;
    }

    public void setWarnOut(BiConsumer<String, Throwable> out) {
        this.warnOut = out;
    }

    public void setInfoOut(BiConsumer<String, Throwable> out) {
        this.infoOut = out;
    }

    public void setDebugOut(BiConsumer<String, Throwable> out) {
        this.debugOut = out;
    }

    public void setTraceOut(BiConsumer<String, Throwable> out) {
        this.traceOut = out;
    }

    // :: Internal

    private void log(Level level, String msg, Object[] args, Throwable t) {
        if (level.ordinal() > globalLevel.ordinal()) {
            return;
        }

        String formatted = format(msg, args);

        BiConsumer<String, Throwable> out = switch (level) {
            case ERROR -> errorOut;
            case WARN -> warnOut;
            case INFO -> infoOut;
            case DEBUG -> debugOut;
            case TRACE -> traceOut;
        };

        if (out != null) {
            out.accept(formatted, t);
        } else {
            defaultOutput(level, formatted, t);
        }
    }

    private void defaultOutput(Level level, String formatted, Throwable t) {
        String line = level.name() + " [" + shortName() + "] " + formatted;
        if (level.ordinal() <= Level.WARN.ordinal()) {
            System.err.println(line);
            if (t != null) {
                t.printStackTrace(System.err);
            }
        } else {
            System.out.println(line);
            if (t != null) {
                t.printStackTrace(System.out);
            }
        }
    }

    private String shortName() {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }

    private static String format(String msg, Object[] args) {
        if (msg == null) {
            return "";
        }
        if (args == null || args.length == 0) {
            return msg;
        }

        var sb = new StringBuilder(msg.length() + 32);
        int argIdx = 0;
        int i = 0;
        while (i < msg.length()) {
            if (i < msg.length() - 1 && msg.charAt(i) == '{' && msg.charAt(i + 1) == '}') {
                if (argIdx < args.length) {
                    sb.append(args[argIdx++]);
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(msg.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static Throwable extractThrowable(Object[] args) {
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable t) {
            return t;
        }
        return null;
    }

    private static Log defaultCreate(String name) {
        return new Log(name);
    }
}
