package br.com.wdc.framework.domain.criteria;

/**
 * Operador de comparação de um {@link Criterion}, com a mesma semântica do SQL.
 *
 * <p>
 * Vive no {@code domain} e é deliberadamente <b>neutro</b>: não conhece jOOQ nem SQL, apenas nomeia a comparação. Quem
 * traduz para {@code Condition} é o {@code framework.jooq}; o cliente REST serializa o mesmo enum. Fosse este enum
 * ciente de jOOQ, o critério deixaria de ser montável do lado do cliente.
 * </p>
 */
public enum Operator {

    /** {@code = ?} */
    EQ(1),
    /** {@code <> ?} */
    NE(1),
    /** {@code > ?} */
    GT(1),
    /** {@code >= ?} */
    GE(1),
    /** {@code < ?} */
    LT(1),
    /** {@code <= ?} */
    LE(1),
    /** {@code LIKE ?} — sensível a maiúsculas; os curingas fazem parte do valor. */
    LIKE(1),
    /** {@code ILIKE ?} — insensível a maiúsculas. */
    ILIKE(1),
    /** {@code BETWEEN ? AND ?} */
    BETWEEN(2),
    /** {@code IN (?, ?, …)} — aridade variável. */
    IN(-1),
    /** {@code IS NULL} */
    IS_NULL(0),
    /** {@code IS NOT NULL} */
    IS_NOT_NULL(0);

    private final int arity;

    Operator(int arity) {
        this.arity = arity;
    }

    /** Quantidade de valores que o operador exige; {@code -1} quando é variável ({@link #IN}). */
    public int arity() {
        return arity;
    }

    /** {@code true} quando a quantidade informada satisfaz o operador. */
    public boolean accepts(int count) {
        return arity < 0 ? count > 0 : count == arity;
    }
}
