/**
 * Domain metadata — the source of truth for this demonstration.
 *
 * Everything the interface offers comes from here: which entities exist, which fields each one projects, which fields
 * can be filtered on and — the point this demonstration wants to teach — which operators each field type accepts.
 *
 * The per-type restriction is not screen decoration: it mirrors the domain classes. `Criterion` covers what is compared
 * only by identity, `ComparableCriterion` adds ordering, and `TextCriterion` adds pattern matching. Asking for
 * `between` on a field without ordering does not even compile on the Java side; here the field simply does not offer
 * the option.
 */

/** Operators by criterion family, in the order that makes sense to whoever builds the filter. */
export const OPERATORS = {
  EQ: { label: 'equal to', arity: 1, sql: '=' },
  NE: { label: 'not equal to', arity: 1, sql: '<>' },
  GT: { label: 'greater than', arity: 1, sql: '>' },
  GE: { label: 'greater than or equal to', arity: 1, sql: '>=' },
  LT: { label: 'less than', arity: 1, sql: '<' },
  LE: { label: 'less than or equal to', arity: 1, sql: '<=' },
  BETWEEN: { label: 'between', arity: 2, sql: 'BETWEEN' },
  IN: { label: 'in (list)', arity: -1, sql: 'IN' },
  LIKE: { label: 'like (case-sensitive)', arity: 1, sql: 'LIKE' },
  ILIKE: { label: 'like (case-insensitive)', arity: 1, sql: 'ILIKE' },
  IS_NULL: { label: 'is null', arity: 0, sql: 'IS NULL' },
  IS_NOT_NULL: { label: 'is not null', arity: 0, sql: 'IS NOT NULL' },
};

/** Identity only — enum, boolean, foreign key. */
const IDENTITY_OPS = ['EQ', 'NE', 'IN', 'IS_NULL', 'IS_NOT_NULL'];

/** Identity + ordering — number, date, timestamp. */
const COMPARABLE_OPS = ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'BETWEEN', 'IN', 'IS_NULL', 'IS_NOT_NULL'];

/** All of the above + pattern matching. Text is ordered too: `name().ge("M")` is a legitimate query. */
const TEXT_OPS = [...COMPARABLE_OPS, 'LIKE', 'ILIKE'];

/** The criterion family of each field type, and how the value is read from the form. */
export const FIELD_KINDS = {
  identity: { operators: IDENTITY_OPS, parse: (raw) => raw, input: 'text' },
  number: { operators: COMPARABLE_OPS, parse: (raw) => Number(raw), input: 'number' },
  text: { operators: TEXT_OPS, parse: (raw) => raw, input: 'text' },
  date: { operators: COMPARABLE_OPS, parse: (raw) => raw, input: 'datetime-local' },
};

/**
 * Text shortcuts — not operators of their own, but `ILIKE` with the wildcards already in place.
 *
 * They exist because wildcards are part of the value, as in SQL: `like('COFFEE')` is not `like('%COFFEE%')`.
 * Anchoring at the start is exactly what lets the database use an index, so a shortcut that makes it explicit is
 * worth having.
 */
export const TEXT_SHORTCUTS = {
  startingWith: { label: 'starts with', wrap: (v) => `${v}%`, explain: (v) => `starts with “${v}”` },
  containing: { label: 'contains', wrap: (v) => `%${v}%`, explain: (v) => `contains “${v}”` },
  endingWith: { label: 'ends with', wrap: (v) => `%${v}`, explain: (v) => `ends with “${v}”` },
};

/**
 * The four entities: the REST path, the projectable fields, the filterable fields, and the provisioned orderings.
 *
 * `orderings` is not a list of sortable fields: each entry is a whole ordering, with a name that states its effect
 * and an `sql` that shows which columns it sorts by. A new ordering comes in by decision, together with the index that
 * supports it — which is why the list is short.
 */
export const ENTITIES = {
  product: {
    label: 'Product',
    path: 'product',
    criteriaName: 'ProductCriteria',
    orderings: [
      { name: 'OLDEST_FIRST', label: 'oldest first', sql: 'ID asc' },
      { name: 'NEWEST_FIRST', label: 'newest first', sql: 'ID desc' },
      { name: 'NAME_A_TO_Z', label: 'name (A → Z)', sql: 'NAME asc, ID asc' },
      { name: 'CHEAPEST_FIRST', label: 'cheapest first', sql: 'PRICE asc, ID asc' },
      { name: 'MOST_EXPENSIVE_FIRST', label: 'most expensive first', sql: 'PRICE desc, ID asc' },
    ],
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'name', kind: 'text', label: 'name' },
      { name: 'price', kind: 'number', label: 'price' },
      { name: 'description', kind: 'text', label: 'description' },
      { name: 'image', kind: 'text', label: 'image (base64)', heavy: true },
    ],
    criteria: [
      { name: 'productId', kind: 'number', label: 'productId', column: 'EN_PRODUCT.ID' },
      { name: 'name', kind: 'text', label: 'name', column: 'EN_PRODUCT.NAME' },
      {
        name: 'price',
        kind: 'number',
        label: 'price',
        column: 'EN_PRODUCT.PRICE',
        note: 'The column is NUMERIC and the domain speaks Double: the value is converted before the comparison, and the same holds for between and in.',
      },
      { name: 'description', kind: 'text', label: 'description', column: 'EN_PRODUCT.DESCRIPTION' },
    ],
  },

  user: {
    label: 'User',
    path: 'user',
    criteriaName: 'UserCriteria',
    orderings: [
      { name: 'OLDEST_FIRST', label: 'oldest first', sql: 'ID asc' },
      { name: 'NEWEST_FIRST', label: 'newest first', sql: 'ID desc' },
      { name: 'NAME_A_TO_Z', label: 'name (A → Z)', sql: 'NAME asc, ID asc' },
      { name: 'LOGIN_A_TO_Z', label: 'login (A → Z)', sql: 'USERNAME asc, ID asc' },
    ],
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'userName', kind: 'text', label: 'login' },
      { name: 'name', kind: 'text', label: 'name' },
      { name: 'roles', kind: 'text', label: 'roles' },
    ],
    criteria: [
      { name: 'userId', kind: 'number', label: 'userId', column: 'EN_USER.ID' },
      { name: 'userName', kind: 'text', label: 'userName', column: 'EN_USER.USERNAME' },
      { name: 'name', kind: 'text', label: 'name', column: 'EN_USER.NAME' },
      {
        name: 'roles',
        kind: 'text',
        label: 'roles',
        column: 'EN_USER.ROLES',
        note: 'Comma-separated roles in the column; “contains” is the filter that answers “has this role”.',
      },
    ],
  },

  purchase: {
    label: 'Purchase',
    path: 'purchase',
    criteriaName: 'PurchaseCriteria',
    orderings: [
      { name: 'OLDEST_FIRST', label: 'oldest first', sql: 'ID asc' },
      { name: 'NEWEST_FIRST', label: 'newest first', sql: 'ID desc' },
      { name: 'MOST_RECENT_PURCHASE_FIRST', label: 'most recent purchase first', sql: 'BUYDATE desc, ID desc' },
      { name: 'EARLIEST_PURCHASE_FIRST', label: 'earliest purchase first', sql: 'BUYDATE asc, ID asc' },
    ],
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'buyDate', kind: 'date', label: 'purchase date' },
      { name: 'user', kind: 'ref', label: 'user', ref: 'user' },
    ],
    collection: {
      name: 'items',
      label: 'items',
      of: 'purchaseItem',
      note: '1:N relation. In the projection it is an envelope with shape, criteria, and slice; in the response, the array of rows.',
    },
    criteria: [
      { name: 'purchaseId', kind: 'number', label: 'purchaseId', column: 'EN_PURCHASE.ID' },
      { name: 'userId', kind: 'number', label: 'userId', column: 'EN_PURCHASE.USERID' },
      {
        name: 'buyDate',
        kind: 'date',
        label: 'buyDate',
        column: 'EN_PURCHASE.BUYDATE',
        note: 'The column stores no time zone: the value is converted to LocalDateTime before the comparison.',
      },
      {
        name: 'productId',
        kind: 'number',
        label: 'productId',
        column: 'EN_PURCHASEITEM.PRODUCTID',
        derived: 'EXISTS over the items',
        note: 'Not a purchase column: the product lives in the items. The condition comes out as EXISTS, but the field is used like any other.',
      },
    ],
  },

  purchaseItem: {
    label: 'Purchase item',
    path: 'purchase-item',
    criteriaName: 'PurchaseItemCriteria',
    orderings: [
      { name: 'OLDEST_FIRST', label: 'oldest first', sql: 'ID asc' },
      { name: 'NEWEST_FIRST', label: 'newest first', sql: 'ID desc' },
      { name: 'MOST_EXPENSIVE_FIRST', label: 'most expensive first', sql: 'PRICE desc, ID asc' },
      { name: 'LARGEST_QUANTITY_FIRST', label: 'largest quantity first', sql: 'AMOUNT desc, ID asc' },
    ],
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'amount', kind: 'number', label: 'quantity' },
      { name: 'price', kind: 'number', label: 'price' },
      { name: 'product', kind: 'ref', label: 'product', ref: 'product' },
    ],
    criteria: [
      { name: 'purchaseItemId', kind: 'number', label: 'purchaseItemId', column: 'EN_PURCHASEITEM.ID' },
      { name: 'purchaseId', kind: 'number', label: 'purchaseId', column: 'EN_PURCHASEITEM.PURCHASEID' },
      { name: 'productId', kind: 'number', label: 'productId', column: 'EN_PURCHASEITEM.PRODUCTID' },
      { name: 'amount', kind: 'number', label: 'amount', column: 'EN_PURCHASEITEM.AMOUNT' },
      {
        name: 'price',
        kind: 'number',
        label: 'price',
        column: 'EN_PURCHASEITEM.PRICE',
        note: 'As with the product, the column is NUMERIC and the value is converted before the comparison.',
      },
      {
        name: 'userId',
        kind: 'number',
        label: 'userId',
        column: 'EN_PURCHASE.USERID',
        derived: 'EXISTS over the purchase',
        note: 'The user owns the purchase, not the item. The condition applies to EN_PURCHASE, correlated by the foreign key.',
      },
    ],
  },
};

/** Query operations — the ones that carry criteria, and so matter to this demonstration. */
export const QUERY_OPERATIONS = {
  fetch: { label: 'fetch', method: 'POST', suffix: '/fetch', paging: 'offset', describe: 'Lists the rows that satisfy the criteria.' },
  'fetch-page': { label: 'fetch-page', method: 'POST', suffix: '/fetch-page', paging: 'page', describe: 'Like fetch, also returning the total row count.' },
  count: { label: 'count', method: 'POST', suffix: '/count', paging: null, describe: 'Counts the rows that satisfy the criteria, without fetching them.' },
};

export const entity = (key) => ENTITIES[key];

export const fieldKind = (kind) => FIELD_KINDS[kind] ?? FIELD_KINDS.identity;

/** The operators a field offers, according to its type's family. */
export const operatorsOf = (field) => fieldKind(field.kind).operators;
