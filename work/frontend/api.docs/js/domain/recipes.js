/**
 * Receitas: filtros prontos que mostram um ponto cada.
 *
 * Cada uma existe para tornar visível uma regra que a documentação enuncia mas que só convence quando se vê o JSON e o
 * resultado. Clicar numa receita monta o filtro; o que se aprende é a diferença entre ela e a vizinha.
 */

export const RECIPES = [
  {
    id: 'range',
    entity: 'product',
    title: 'Intervalo em duas linhas',
    teaches: 'Dois pedidos no mesmo campo valem juntos: é o AND por padrão, e é o que faz um intervalo montado a partir de dois campos de tela significar o que se espera.',
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
    title: 'O mesmo intervalo, com BETWEEN',
    teaches: 'Um pedido só, com dois valores. O resultado é o mesmo da receita anterior — a diferença está no SQL gerado, não no conjunto devolvido.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('BETWEEN').values = ['1', '3'];
    },
  },
  {
    id: 'two-ne',
    entity: 'product',
    title: 'Excluir dois valores',
    teaches: 'Dois “diferente de” excluem os dois. Fosse OR o padrão, cada linha satisfaria um dos dois e a consulta devolveria quase tudo — sem nada indicar o erro.',
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
    title: 'Alternativas com OU',
    teaches: 'Com AND isto daria zero: nenhum id é dois valores ao mesmo tempo. A disjunção vale dentro do campo; entre campos a junção é sempre AND.',
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
    title: 'Vários valores com IN',
    teaches: 'Um pedido de aridade variável. Diz o mesmo que a receita do OU, em uma linha — e é o que o banco costuma otimizar melhor.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('IN').values = ['0,1,3'];
    },
  },
  {
    id: 'starting-with',
    entity: 'user',
    title: 'Texto ancorado no início',
    teaches: 'O atalho “começa com” gera ILIKE com o curinga só no fim. Ancorar no início é o que permite ao banco usar índice — por isso ele é diferente de “contém”.',
    apply: (criteria) => {
      const c = criteria.criterion('userName');
      c.clear();
      c.add('ILIKE', 'startingWith').values = ['adm'];
    },
  },
  {
    id: 'like-vs-ilike',
    entity: 'user',
    title: 'LIKE não ignora maiúsculas',
    teaches: 'Este devolve zero linhas; trocando para ILIKE, devolve o admin. É o par que justifica existirem os dois operadores — e lembra que os curingas fazem parte do valor.',
    apply: (criteria) => {
      const c = criteria.criterion('userName');
      c.clear();
      c.add('LIKE').values = ['ADMIN'];
    },
  },
  {
    id: 'derived',
    entity: 'purchase',
    title: 'Filtrar compra pelo produto',
    teaches: 'productId não é coluna da compra — vive nos itens. A condição sai como EXISTS, mas o campo se usa como qualquer outro: in, between e a disjunção valem aqui também.',
    apply: (criteria) => {
      const c = criteria.criterion('productId');
      c.clear();
      c.add('EQ').values = ['1'];
    },
  },
];

/** Receitas da entidade corrente. */
export const recipesFor = (entityKey) => RECIPES.filter((r) => r.entity === entityKey);

/**
 * Receita da coleção projetada — mexe na projeção, não no critério, e por isso é tratada à parte.
 */
export const COLLECTION_RECIPE = {
  id: 'collection',
  title: 'Itens ordenados e recortados',
  teaches: 'A coleção projetada leva forma, critério e recorte. Sem isso, pedir “o item mais recente” traria todos, sem ordem — que era o que acontecia antes deste envelope existir.',
  apply: (projection) => {
    const { collection } = projection;
    collection.enabled = true;
    collection.limit = '1';
    collection.offset = null;
    collection.criteria.clear();
    collection.criteria.orderBy = 'NEWEST_FIRST';
  },
};
