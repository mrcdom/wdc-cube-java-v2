/**
 * The criteria: what is requested, how it becomes JSON, and what it means.
 *
 * This is the piece the demonstration exists to teach. A criteria field does not carry a value — it carries a list of
 * *comparison requests*, each with its operator and values, plus how they combine. That is why the JSON has the shape
 * it has:
 *
 *     "productId": { "or": true, "p": [ { "o": "GE", "v": [10] }, { "o": "IS_NULL" } ] }
 *
 * A bare value would not be enough: `BETWEEN` takes two values, `IN` takes many, `IS_NULL` none, and the disjunction
 * belongs to the field, not to the request. Reducing this to `"productId": 10` would discard everything but equality —
 * silently.
 *
 * Besides the JSON, each criteria object can explain itself in plain English and show the approximate `WHERE`. It is
 * the same structure seen from three angles, and comparing them is what teaches.
 */

import { OPERATORS, TEXT_SHORTCUTS, entity } from './schema.js';

/** A comparison request: an operator and the values that go with it. */
export class Predicate {
  static #seq = 0;

  id = `p${Predicate.#seq++}`;

  /** @param {string} operator @param {unknown[]} values @param {string|null} shortcut text shortcut it came from */
  constructor(operator = 'EQ', values = [], shortcut = null) {
    this.operator = operator;
    this.values = values;
    this.shortcut = shortcut;
  }

  get arity() {
    return OPERATORS[this.operator]?.arity ?? 1;
  }

  /**
   * The values entered, already split out.
   *
   * `IN` is typed into a single input, comma-separated, because that is how one thinks of a list — but on the wire it
   * is an array, and this is where that translation happens.
   */
  rawValues() {
    const filled = this.values.filter((v) => v !== '' && v !== null && v !== undefined);
    if (this.arity < 0) {
      return String(filled[0] ?? '').split(',').map((v) => v.trim()).filter((v) => v !== '');
    }
    return filled;
  }

  /** A request goes into the JSON only when it has the values its operator requires. */
  get complete() {
    const values = this.rawValues();
    return this.arity < 0 ? values.length > 0 : values.length === this.arity;
  }

  /** The values as they go on the wire — a text shortcut with its wildcards already applied. */
  valuesForWire(parse) {
    const wrap = this.shortcut ? TEXT_SHORTCUTS[this.shortcut].wrap : (v) => v;
    return this.rawValues().map((v) => (this.shortcut ? wrap(v) : parse(v)));
  }

  toJSON(parse) {
    const json = { o: this.operator };
    if (this.arity !== 0) {
      json.v = this.valuesForWire(parse);
    }
    return json;
  }
}

/** A filterable field: the requests made on it and how they combine. */
export class Criterion {
  /** @param {object} field descriptor from the schema */
  constructor(field) {
    this.field = field;
    this.predicates = [];
    this.disjunctive = false;
  }

  get name() {
    return this.field.name;
  }

  /** Only complete requests count — a half-filled form does not become a filter. */
  get effective() {
    return this.predicates.filter((p) => p.complete);
  }

  get isSet() {
    return this.effective.length > 0;
  }

  add(operator = 'EQ', shortcut = null) {
    const predicate = new Predicate(operator, [], shortcut);
    this.predicates.push(predicate);
    return predicate;
  }

  remove(id) {
    this.predicates = this.predicates.filter((p) => p.id !== id);
  }

  clear() {
    this.predicates = [];
    this.disjunctive = false;
  }

  toJSON() {
    if (!this.isSet) return undefined;

    const parse = (v) => (this.field.kind === 'number' ? Number(v) : v);
    const json = {};
    if (this.disjunctive) json.or = true;
    json.p = this.effective.map((p) => p.toJSON(parse));
    return json;
  }
}

/** An entity's whole criteria: its fields, plus the ordering. */
export class Criteria {
  constructor(entityKey) {
    this.entityKey = entityKey;
    this.meta = entity(entityKey);
    this.criterions = this.meta.criteria.map((f) => new Criterion(f));
    this.orderBy = null;
  }

  criterion(name) {
    return this.criterions.find((c) => c.name === name);
  }

  get active() {
    return this.criterions.filter((c) => c.isSet);
  }

  clear() {
    this.criterions.forEach((c) => c.clear());
    this.orderBy = null;
  }

  /** The object that goes in the request body — fields left unset do not even appear. */
  toJSON() {
    const json = {};
    for (const criterion of this.criterions) {
      const value = criterion.toJSON();
      if (value !== undefined) json[criterion.name] = value;
    }
    if (this.orderBy) json.orderBy = this.orderBy;
    return json;
  }
}

// ---------------------------------------------------------------------------
// The other two readings of the same structure
// ---------------------------------------------------------------------------

const quote = (v) => (typeof v === 'number' ? String(v) : `'${v}'`);

/** A request, in plain English. */
function explainPredicate(criterion, predicate) {
  const { operator, shortcut } = predicate;
  const values = predicate.valuesForWire((v) => v);

  if (shortcut && values.length) {
    // The shortcut is clearer than the ILIKE it generates — whoever picked "starts with" thinks that way.
    return TEXT_SHORTCUTS[shortcut].explain(predicate.rawValues()[0]);
  }
  if (operator === 'IS_NULL') return 'is null';
  if (operator === 'IS_NOT_NULL') return 'is not null';
  if (operator === 'BETWEEN') return `is between ${values[0]} and ${values[1]}`;
  if (operator === 'IN') return `is one of [${values.join(', ')}]`;
  return `${OPERATORS[operator].label} ${values.map((v) => `“${v}”`).join(', ')}`;
}

/** The whole criteria, in plain English — the sentence the filter means. */
export function explain(criteria) {
  const parts = criteria.active.map((criterion) => {
    const joiner = criterion.disjunctive ? ' or ' : ' and ';
    const clauses = criterion.effective.map((p) => explainPredicate(criterion, p)).join(joiner);
    const many = criterion.effective.length > 1;
    return `${criterion.field.label} ${many ? `(${clauses})` : clauses}`;
  });

  if (parts.length === 0) return 'No filter — every row.';
  return parts.join('\nand ');
}

/** A request, as an SQL condition. */
function sqlPredicate(column, predicate, kind) {
  const { operator } = predicate;
  // The same parse as the JSON: without it, an id would come out as '1' — text —, suggesting a comparison other than
  // the one that happens.
  const values = predicate.valuesForWire((v) => (kind === 'number' ? Number(v) : v));
  const op = OPERATORS[operator].sql;

  if (operator === 'IS_NULL' || operator === 'IS_NOT_NULL') return `${column} ${op}`;
  if (operator === 'BETWEEN') return `${column} BETWEEN ${quote(values[0])} AND ${quote(values[1])}`;
  if (operator === 'IN') return `${column} IN (${values.map(quote).join(', ')})`;
  return `${column} ${op} ${quote(values[0])}`;
}

/**
 * The approximate `WHERE` — approximate on purpose.
 *
 * It shows how the requests combine: `AND` within the field by default, `OR` when the field is disjunctive, and
 * always `AND` between fields. Derived fields appear as the `EXISTS` they actually become.
 */
export function toSql(criteria) {
  const parts = criteria.active.map((criterion) => {
    const { column, derived } = criterion.field;
    const joiner = criterion.disjunctive ? ' OR ' : ' AND ';
    const clauses = criterion.effective.map((p) => sqlPredicate(column, p, criterion.field.kind)).join(joiner);
    const body = criterion.effective.length > 1 ? `(${clauses})` : clauses;

    if (derived) {
      const inner = criterion.effective.length > 1 ? `(${clauses})` : clauses;
      return `EXISTS (SELECT 1 FROM ${column.split('.')[0]} WHERE ${inner} /* correlated */)`;
    }
    return body;
  });

  // An ordering is not "field + direction": each constant has its own ORDER BY, declared in the schema.
  const ordering = (criteria.meta.orderings ?? []).find((o) => o.name === criteria.orderBy);
  const orderBy = ordering ? `ORDER BY ${ordering.sql}` : '';

  // With no filter there can still be an ordering: they are independent, and leaving out the ORDER BY here would hide
  // exactly what the choice of ordering produces.
  if (parts.length === 0) {
    return `-- no WHERE: empty criteria filter nothing${orderBy ? `\n${orderBy}` : ''}`;
  }

  const where = parts.length > 1 ? parts.map((p) => `      ${p}`).join('\n  AND\n') : `      ${parts[0]}`;
  return `WHERE\n${where}${orderBy ? `\n${orderBy}` : ''}`;
}
