/**
 * Recipes: ready-made filters that each make one point.
 *
 * Each one exists to make visible a rule that the documentation states but that only convinces once you see the JSON
 * and the result. Clicking a recipe builds the filter; what you learn is the difference between it and its neighbor.
 */

export const RECIPES = [
  {
    id: 'range',
    entity: 'product',
    title: 'A range in two rows',
    teaches: 'Two requests on the same field apply together: that is the default AND, and it is what makes a range built from two input fields mean what you expect.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('GE').values = ['1'];
      c.add('LE').values = ['3'];
    },
  },
  {
    id: 'between',
    entity: 'product',
    title: 'The same range, with BETWEEN',
    teaches: 'A single request with two values. The result is the same as the previous recipe — the difference is in the generated SQL, not in the returned set.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('BETWEEN').values = ['1', '3'];
    },
  },
  {
    id: 'two-ne',
    entity: 'product',
    title: 'Exclude two values',
    teaches: 'Two “not equal to” exclude both. If OR were the default, every row would satisfy one of the two and the query would return almost everything — with nothing pointing to the mistake.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('NE').values = ['0'];
      c.add('NE').values = ['1'];
    },
  },
  {
    id: 'or',
    entity: 'product',
    title: 'Alternatives with OR',
    teaches: 'With AND this would return zero: no id is two values at once. The disjunction applies within the field; between fields the join is always AND.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.disjunctive = true;
      c.add('EQ').values = ['0'];
      c.add('EQ').values = ['1'];
    },
  },
  {
    id: 'in',
    entity: 'product',
    title: 'Several values with IN',
    teaches: 'A variable-arity request. It says the same as the OR recipe, in one row — and it is what the database usually optimizes best.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('IN').values = ['0,1,3'];
    },
  },
  {
    id: 'starting-with',
    entity: 'user',
    title: 'Text anchored at the start',
    teaches: 'The “starts with” shortcut generates ILIKE with the wildcard only at the end. Anchoring at the start is what lets the database use an index — which is why it differs from “contains”.',
    apply: (criteria) => {
      const c = criteria.criterion('userName');
      c.clear();
      c.add('ILIKE', 'startingWith').values = ['adm'];
    },
  },
  {
    id: 'like-vs-ilike',
    entity: 'user',
    title: 'LIKE is case-sensitive',
    teaches: 'This one returns zero rows; switch to ILIKE and it returns admin. It is the pair that justifies having both operators — and a reminder that wildcards are part of the value.',
    apply: (criteria) => {
      const c = criteria.criterion('userName');
      c.clear();
      c.add('LIKE').values = ['ADMIN'];
    },
  },
  {
    id: 'derived',
    entity: 'purchase',
    title: 'Filter purchases by product',
    teaches: 'productId is not a purchase column — it lives in the items. The condition comes out as EXISTS, but the field is used like any other: in, between, and the disjunction apply here too.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('EQ').values = ['1'];
    },
  },
];

/** Recipes for the current entity. */
export const recipesFor = (entityKey) => RECIPES.filter((r) => r.entity === entityKey);

/**
 * The projected collection recipe — it changes the projection, not the criteria, and so is handled separately.
 */
export const COLLECTION_RECIPE = {
  id: 'collection',
  title: 'Ordered and sliced items',
  teaches: 'The projected collection carries shape, criteria, and slice. Without them, asking for “the most recent item” would bring all of them, in no order.',
  apply: (projection) => {
    const { collection } = projection;
    collection.enabled = true;
    collection.limit = '1';
    collection.offset = null;
    collection.criteria.clear();
    collection.criteria.orderBy = 'NEWEST_FIRST';
  },
};
