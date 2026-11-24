package br.com.wdc.framework.jooq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.Test;

import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.TextCriterion;

/** The translation of a field that applies to several columns, and of the alternative groups. */
public class CriterionTranslatorTest {

    private static final class Search implements Criteria {

        final TextCriterion<Search> term = new TextCriterion<>(this, "term");

        List<Search> groups = List.of();

        @Override
        public List<Criterion<?, ?>> criterions() {
            return List.of(term);
        }

        @Override
        public List<Search> anyOf() {
            return groups;
        }
    }

    private final DSLContext dsl = DSL.using(SQLDialect.H2);
    private final Field<String> name = DSL.field(DSL.name("name"), SQLDataType.VARCHAR);
    private final Field<String> login = DSL.field(DSL.name("login"), SQLDataType.VARCHAR);

    private String sql(Condition c) {
        return c == null ? null : dsl.renderInlined(c);
    }

    // :: translateAny

    @Test
    public void translateAny_appliesTheFieldToEachColumn_joinedByOr() {
        var search = new Search().term.containing("ab");

        assertEquals(
                sql(DSL.or(CriterionTranslator.translate(name, search.term),
                        CriterionTranslator.translate(login, search.term))),
                sql(CriterionTranslator.translateAny(List.of(name, login), search.term)));
    }

    @Test
    public void translateAny_keepsEveryRequestOfTheFieldInsideEachColumn() {
        var search = new Search().term.startingWith("a").term.containing("z");

        var each = DSL.or(
                DSL.and(name.likeIgnoreCase("a%"), name.likeIgnoreCase("%z%")),
                DSL.and(login.likeIgnoreCase("a%"), login.likeIgnoreCase("%z%")));
        assertEquals(sql(each), sql(CriterionTranslator.translateAny(List.of(name, login), search.term)));
    }

    @Test
    public void translateAny_withOneColumn_isTheSameAsTranslate() {
        var search = new Search().term.eq("x");

        assertEquals(sql(CriterionTranslator.translate(name, search.term)),
                sql(CriterionTranslator.translateAny(List.of(name), search.term)));
    }

    @Test
    public void translateAny_withoutRequestOrColumns_isNull() {
        assertNull(CriterionTranslator.translateAny(List.of(name, login), new Search().term));
        assertNull(CriterionTranslator.translateAny(List.<Field<String>> of(), new Search().term.eq("x").term));
    }

    // :: anyOf

    @Test
    public void anyOf_joinsTheGroupsWithOr() {
        var a = new Search().term.eq("a");
        var b = new Search().term.eq("b");

        assertEquals(sql(DSL.or(name.eq("a"), name.eq("b"))),
                sql(CriterionTranslator.anyOf(List.of(a, b), s -> CriterionTranslator.translate(name, s.term))));
    }

    @Test
    public void anyOf_aGroupWithNoCondition_matchesEveryRow() {
        var a = new Search().term.eq("a");
        var empty = new Search();

        assertNull("a group that does not filter makes the whole alternative hold",
                CriterionTranslator.anyOf(List.of(a, empty), s -> CriterionTranslator.and(
                        Collections.singletonList(CriterionTranslator.translate(name, s.term)))));
    }

    @Test
    public void anyOf_withoutGroups_isNull() {
        assertNull(CriterionTranslator.anyOf(List.<Search> of(), s -> DSL.trueCondition()));
    }
}
