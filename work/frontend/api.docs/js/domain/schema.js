/**
 * Metadados do domínio — a fonte de verdade desta demonstração.
 *
 * Tudo o que a interface oferece sai daqui: quais entidades existem, que campos cada uma projeta, por quais campos se
 * pode filtrar e — o ponto que esta demonstração quer ensinar — que operadores cada tipo de campo aceita.
 *
 * A restrição por tipo não é enfeite da tela: ela espelha as classes do domínio. `Criterion` cobre o que só se compara
 * por identidade, `ComparableCriterion` acrescenta ordem e `TextCriterion` acrescenta busca por padrão. Pedir
 * `between` num campo sem ordem nem compila do lado Java; aqui o campo simplesmente não oferece a opção.
 */

/** Operadores por família de critério, na ordem em que fazem sentido para quem monta o filtro. */
export const OPERATORS = {
  EQ: { label: 'igual a', arity: 1, sql: '=' },
  NE: { label: 'diferente de', arity: 1, sql: '<>' },
  GT: { label: 'maior que', arity: 1, sql: '>' },
  GE: { label: 'maior ou igual a', arity: 1, sql: '>=' },
  LT: { label: 'menor que', arity: 1, sql: '<' },
  LE: { label: 'menor ou igual a', arity: 1, sql: '<=' },
  BETWEEN: { label: 'entre', arity: 2, sql: 'BETWEEN' },
  IN: { label: 'em (lista)', arity: -1, sql: 'IN' },
  LIKE: { label: 'como (sensível a maiúsculas)', arity: 1, sql: 'LIKE' },
  ILIKE: { label: 'como (ignora maiúsculas)', arity: 1, sql: 'ILIKE' },
  IS_NULL: { label: 'é nulo', arity: 0, sql: 'IS NULL' },
  IS_NOT_NULL: { label: 'não é nulo', arity: 0, sql: 'IS NOT NULL' },
};

/** Só identidade — enum, booleano, chave estrangeira. */
const IDENTITY_OPS = ['EQ', 'NE', 'IN', 'IS_NULL', 'IS_NOT_NULL'];

/** Identidade + ordem — número, data, timestamp. */
const COMPARABLE_OPS = ['EQ', 'NE', 'GT', 'GE', 'LT', 'LE', 'BETWEEN', 'IN', 'IS_NULL', 'IS_NOT_NULL'];

/** Tudo acima + busca por padrão. Texto também se ordena: `name().ge("M")` é consulta legítima. */
const TEXT_OPS = [...COMPARABLE_OPS, 'LIKE', 'ILIKE'];

/** A família de critério de cada tipo de campo, e como o valor é lido do formulário. */
export const FIELD_KINDS = {
  identity: { operators: IDENTITY_OPS, parse: (raw) => raw, input: 'text' },
  number: { operators: COMPARABLE_OPS, parse: (raw) => Number(raw), input: 'number' },
  text: { operators: TEXT_OPS, parse: (raw) => raw, input: 'text' },
  date: { operators: COMPARABLE_OPS, parse: (raw) => raw, input: 'datetime-local' },
};

/**
 * Atalhos textuais — não são operadores próprios, são `ILIKE` com os curingas já postos.
 *
 * Existem porque os curingas fazem parte do valor, como em SQL: `like('CAFE')` não é `like('%CAFE%')`. Ancorar no
 * início é justamente o que permite ao banco usar índice, e por isso vale ter um atalho que deixa isso explícito.
 */
export const TEXT_SHORTCUTS = {
  startingWith: { label: 'começa com', wrap: (v) => `${v}%`, explain: (v) => `começa com “${v}”` },
  containing: { label: 'contém', wrap: (v) => `%${v}%`, explain: (v) => `contém “${v}”` },
  endingWith: { label: 'termina com', wrap: (v) => `%${v}`, explain: (v) => `termina com “${v}”` },
};

/** As quatro entidades: o caminho REST, os campos projetáveis e os campos filtráveis. */
export const ENTITIES = {
  product: {
    label: 'Produto',
    path: 'product',
    criteriaName: 'ProductCriteria',
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'name', kind: 'text', label: 'nome' },
      { name: 'price', kind: 'number', label: 'preço' },
      { name: 'description', kind: 'text', label: 'descrição' },
      { name: 'image', kind: 'text', label: 'imagem (base64)', heavy: true },
    ],
    criteria: [
      { name: 'productId', kind: 'number', label: 'productId', column: 'EN_PRODUCT.ID' },
      { name: 'name', kind: 'text', label: 'name', column: 'EN_PRODUCT.NAME' },
      {
        name: 'price',
        kind: 'number',
        label: 'price',
        column: 'EN_PRODUCT.PRICE',
        note: 'A coluna é NUMERIC e o domínio fala em Double: o valor é convertido antes da comparação, e isso vale igual para between e in.',
      },
      { name: 'description', kind: 'text', label: 'description', column: 'EN_PRODUCT.DESCRIPTION' },
    ],
  },

  user: {
    label: 'Usuário',
    path: 'user',
    criteriaName: 'UserCriteria',
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'userName', kind: 'text', label: 'login' },
      { name: 'name', kind: 'text', label: 'nome' },
      { name: 'roles', kind: 'text', label: 'papéis' },
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
        note: 'Papéis separados por vírgula na coluna; “contém” é o filtro que responde “tem este papel”.',
      },
    ],
  },

  purchase: {
    label: 'Compra',
    path: 'purchase',
    criteriaName: 'PurchaseCriteria',
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'buyDate', kind: 'date', label: 'data da compra' },
      { name: 'user', kind: 'ref', label: 'usuário', ref: 'user' },
    ],
    collection: {
      name: 'items',
      label: 'itens',
      of: 'purchaseItem',
      note: 'Relação 1:N. Na projeção é um envelope com forma, critério e recorte; na resposta, o array de linhas.',
    },
    criteria: [
      { name: 'purchaseId', kind: 'number', label: 'purchaseId', column: 'EN_PURCHASE.ID' },
      { name: 'userId', kind: 'number', label: 'userId', column: 'EN_PURCHASE.USERID' },
      {
        name: 'buyDate',
        kind: 'date',
        label: 'buyDate',
        column: 'EN_PURCHASE.BUYDATE',
        note: 'A coluna não guarda fuso: o valor é convertido em LocalDateTime antes da comparação.',
      },
      {
        name: 'productId',
        kind: 'number',
        label: 'productId',
        column: 'EN_PURCHASEITEM.PRODUCTID',
        derived: 'EXISTS sobre os itens',
        note: 'Não é coluna da compra: o produto vive nos itens. A condição sai como EXISTS, mas o campo se usa como qualquer outro.',
      },
    ],
  },

  purchaseItem: {
    label: 'Item de compra',
    path: 'purchase-item',
    criteriaName: 'PurchaseItemCriteria',
    fields: [
      { name: 'id', kind: 'number', label: 'id' },
      { name: 'amount', kind: 'number', label: 'quantidade' },
      { name: 'price', kind: 'number', label: 'preço' },
      { name: 'product', kind: 'ref', label: 'produto', ref: 'product' },
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
        note: 'Como no produto, a coluna é NUMERIC e o valor é convertido antes da comparação.',
      },
      {
        name: 'userId',
        kind: 'number',
        label: 'userId',
        column: 'EN_PURCHASE.USERID',
        derived: 'EXISTS sobre a compra',
        note: 'O usuário é dono da compra, não do item. A condição incide sobre EN_PURCHASE, correlacionada pela chave estrangeira.',
      },
    ],
  },
};

/** Operações de consulta — as que carregam critério, e por isso interessam a esta demonstração. */
export const QUERY_OPERATIONS = {
  fetch: { label: 'fetch', method: 'POST', suffix: '/fetch', paging: 'offset', describe: 'Lista as linhas que satisfazem o critério.' },
  'fetch-page': { label: 'fetch-page', method: 'POST', suffix: '/fetch-page', paging: 'page', describe: 'Como fetch, devolvendo também o total de linhas.' },
  count: { label: 'count', method: 'POST', suffix: '/count', paging: null, describe: 'Conta as linhas que satisfazem o critério, sem trazê-las.' },
};

export const entity = (key) => ENTITIES[key];

export const fieldKind = (kind) => FIELD_KINDS[kind] ?? FIELD_KINDS.identity;

/** Os operadores oferecidos por um campo, segundo a família do seu tipo. */
export const operatorsOf = (field) => fieldKind(field.kind).operators;
