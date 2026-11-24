package br.com.wdc.framework.domain.resource;

/**
 * One action on a resource, as an object: the request is gathered with fluent {@code withXxx(...)} setters and
 * {@link #run()} answers it. A new instance per use: {@code new ProductQuery().withProductId(id).run()}.
 * <p>
 * What it does names it, with a verb as the suffix, in the spirit of the HTTP methods: {@code XxxQuery}
 * ({@link ResourceQuery}, the GET), {@code XxxCreate} ({@link ResourceCreate}, POST), {@code XxxSave}
 * ({@link ResourceSave}, PUT), {@code XxxUpdate} ({@link ResourceUpdate}, PATCH) and {@code XxxDelete}
 * ({@link ResourceDelete}, DELETE); the write verbs share {@link ResourceWrite}, which opens the transaction. What fits
 * none of them gets a verb of its own that says what it does and extends this class directly (a {@code XxxCheck} that
 * verifies credentials, for one). The call reaches the repositories it uses itself, and answers with a type that
 * already exists or with one of its own.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ResourceCall<R> {

    /** Answers the request gathered so far. */
    public abstract R run();

}
