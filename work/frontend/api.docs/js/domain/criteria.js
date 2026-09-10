/**
 * O critério: o que se pede, como isso vira JSON, e o que isso significa.
 *
 * Esta é a peça que a demonstração existe para ensinar. Um campo de critério não carrega um valor — carrega uma lista
 * de *pedidos de comparação*, cada um com seu operador e seus valores, mais a informação de como eles se combinam.
 * Por isso o JSON tem a forma que tem:
 *
 *     "productId": { "or": true, "p": [ { "o": "GE", "v": [10] }, { "o": "IS_NULL" } ] }
 *
 * O valor solto não bastaria: `BETWEEN` leva dois valores, `IN` leva muitos, `IS_NULL` nenhum, e a disjunção é do
 * campo, não do pedido. Reduzir isso a `"productId": 10` descartaria tudo menos a igualdade — e em silêncio.
 *
 * Além do JSON, cada critério sabe se explicar em português e mostrar o `WHERE` aproximado. É a mesma estrutura vista
 * de três ângulos, e é a comparação entre eles que ensina.
 */

import { OPERATORS, TEXT_SHORTCUTS, entity } from './schema.js';

/** Um pedido de comparação: operador e os valores que o acompanham. */
export class Predicate {
  static #seq = 0;

  id = `p${Predicate.#seq++}`;

  /** @param {string} operator @param {unknown[]} values @param {string|null} shortcut atalho textual que o originou */
  constructor(operator = 'EQ', values = [], shortcut = null) {
    this.operator = operator;
    this.values = values;
    this.shortcut = shortcut;
  }

  get arity() {
    return OPERATORS[this.operator]?.arity ?? 1;
  }

  /**
   * Os valores informados, já desdobrados.
   *
   * `IN` é digitado num campo só, separado por vírgula, porque é assim que se pensa numa lista — mas no fio ele é um
   * array, e é aqui que essa tradução acontece.
   */
  rawValues() {
    const filled = this.values.filter((v) => v !== '' && v !== null && v !== undefined);
    if (this.arity < 0) {
      return String(filled[0] ?? '').split(',').map((v) => v.trim()).filter((v) => v !== '');
    }
    return filled;
  }

  /** Um pedido só entra no JSON quando tem os valores que o operador exige. */
  get complete() {
    const values = this.rawValues();
    return this.arity < 0 ? values.length > 0 : values.length === this.arity;
  }

  /** Os valores como vão para o fio — o atalho textual já com os curingas aplicados. */
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

/** Um campo filtrável: os pedidos feitos sobre ele e como eles se combinam. */
export class Criterion {
  /** @param {object} field descritor vindo do schema */
  constructor(field) {
    this.field = field;
    this.predicates = [];
    this.disjunctive = false;
  }

  get name() {
    return this.field.name;
  }

  /** Só os pedidos completos contam — um formulário meio preenchido não vira filtro. */
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

/** O critério inteiro de uma entidade: seus campos, mais a ordenação. */
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

  /** O objeto que vai no corpo da requisição — campos não informados nem aparecem. */
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
// As outras duas leituras da mesma estrutura
// ---------------------------------------------------------------------------

const quote = (v) => (typeof v === 'number' ? String(v) : `'${v}'`);

/** Um pedido, em português. */
function explainPredicate(criterion, predicate) {
  const { operator, shortcut } = predicate;
  const values = predicate.valuesForWire((v) => v);

  if (shortcut && values.length) {
    // O atalho é mais claro que o ILIKE que ele gera — quem escolheu "começa com" pensa assim.
    return TEXT_SHORTCUTS[shortcut].explain(predicate.rawValues()[0]);
  }
  if (operator === 'IS_NULL') return 'é nulo';
  if (operator === 'IS_NOT_NULL') return 'não é nulo';
  if (operator === 'BETWEEN') return `está entre ${values[0]} e ${values[1]}`;
  if (operator === 'IN') return `é um de [${values.join(', ')}]`;
  return `${OPERATORS[operator].label} ${values.map((v) => `“${v}”`).join(', ')}`;
}

/** O critério inteiro, em português — a frase que o filtro significa. */
export function explain(criteria) {
  const parts = criteria.active.map((criterion) => {
    const joiner = criterion.disjunctive ? ' ou ' : ' e ';
    const clauses = criterion.effective.map((p) => explainPredicate(criterion, p)).join(joiner);
    const many = criterion.effective.length > 1;
    return `${criterion.field.label} ${many ? `(${clauses})` : clauses}`;
  });

  if (parts.length === 0) return 'Sem filtro — todas as linhas.';
  return parts.join('\ne ');
}

/** Um pedido, como condição SQL. */
function sqlPredicate(column, predicate, kind) {
  const { operator } = predicate;
  // O mesmo parse do JSON: sem ele, um id sairia como '1' — texto —, sugerindo uma comparação que não é a que acontece.
  const values = predicate.valuesForWire((v) => (kind === 'number' ? Number(v) : v));
  const op = OPERATORS[operator].sql;

  if (operator === 'IS_NULL' || operator === 'IS_NOT_NULL') return `${column} ${op}`;
  if (operator === 'BETWEEN') return `${column} BETWEEN ${quote(values[0])} AND ${quote(values[1])}`;
  if (operator === 'IN') return `${column} IN (${values.map(quote).join(', ')})`;
  return `${column} ${op} ${quote(values[0])}`;
}

/**
 * O `WHERE` aproximado — aproximado de propósito.
 *
 * Serve para mostrar como os pedidos se combinam: `AND` dentro do campo por padrão, `OR` quando o campo é disjuntivo,
 * `AND` sempre entre campos. Campos derivados aparecem como o `EXISTS` que de fato viram.
 */
export function toSql(criteria) {
  const parts = criteria.active.map((criterion) => {
    const { column, derived } = criterion.field;
    const joiner = criterion.disjunctive ? ' OR ' : ' AND ';
    const clauses = criterion.effective.map((p) => sqlPredicate(column, p, criterion.field.kind)).join(joiner);
    const body = criterion.effective.length > 1 ? `(${clauses})` : clauses;

    if (derived) {
      const inner = criterion.effective.length > 1 ? `(${clauses})` : clauses;
      return `EXISTS (SELECT 1 FROM ${column.split('.')[0]} WHERE ${inner} /* correlacionado */)`;
    }
    return body;
  });

  // A ordenação não é "campo + direção": cada constante tem o seu ORDER BY, declarado no esquema.
  const ordering = (criteria.meta.orderings ?? []).find((o) => o.name === criteria.orderBy);
  const orderBy = ordering ? `ORDER BY ${ordering.sql}` : '';

  // Sem filtro ainda pode haver ordenação: são coisas independentes, e omitir o ORDER BY aqui esconderia
  // justamente o que a escolha de ordenação produz.
  if (parts.length === 0) {
    return `-- sem WHERE: o critério vazio não filtra nada${orderBy ? `\n${orderBy}` : ''}`;
  }

  const where = parts.length > 1 ? parts.map((p) => `      ${p}`).join('\n  AND\n') : `      ${parts[0]}`;
  return `WHERE\n${where}${orderBy ? `\n${orderBy}` : ''}`;
}
