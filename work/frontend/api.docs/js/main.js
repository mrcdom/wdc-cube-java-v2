/**
 * A aplicação: liga o modelo aos painéis e mantém as quatro vistas em sincronia.
 *
 * As quatro vistas são a mesma coisa vista de ângulos diferentes — o formulário, o JSON que ele produz, a frase que
 * ele significa e o SQL que ele vira. É a comparação entre elas que ensina, então todas se atualizam a cada tecla.
 */

import { $, html, on, pretty, render } from './core/dom.js';
import { Session } from './core/session.js';
import { Criteria, explain, toSql } from './domain/criteria.js';
import { Projection } from './domain/projection.js';
import { COLLECTION_RECIPE, recipesFor } from './domain/recipes.js';
import { ENTITIES, QUERY_OPERATIONS, entity } from './domain/schema.js';
import { CriteriaBuilder } from './ui/criteria-builder.js';
import { ProjectionBuilder } from './ui/projection-builder.js';

class Application {
  #session = new Session();
  #entityKey = 'product';
  #operation = 'fetch';
  #criteria;
  #projection;
  #criteriaBuilder;
  #projectionBuilder;
  #paging = { offset: 0, limit: 0, page: 0, pageSize: 10 };

  start() {
    this.#buildModel();
    this.#wireTabs();
    this.#renderEntityTabs();
    this.#renderOperations();
    this.#wireAuth();
    this.#wireRun();
    this.#mountBuilders();
    this.#session.addEventListener('change', () => this.#renderAuthState());
    this.#renderAuthState();
    this.#refresh();
  }

  #buildModel() {
    this.#criteria = new Criteria(this.#entityKey);
    this.#projection = new Projection(this.#entityKey);
  }

  // -- Estrutura -----------------------------------------------------------

  /**
   * Os ouvintes das abas são ligados uma única vez.
   *
   * Ligá-los dentro do desenho — que roda a cada troca — acrescentaria um ouvinte por vez, e um clique passaria a
   * disparar tantas trocas quantas já tivessem ocorrido.
   */
  #wireTabs() {
    on($('#entity-tabs'), 'click', 'button', (_e, el) => {
      this.#entityKey = el.dataset.entity;
      this.#buildModel();
      this.#renderEntityTabs();
      this.#mountBuilders();
      this.#refresh();
    });

    on($('#operations'), 'click', 'button', (_e, el) => {
      this.#operation = el.dataset.operation;
      this.#renderOperations();
      this.#refresh();
    });
  }

  #renderEntityTabs() {
    render($('#entity-tabs'), Object.entries(ENTITIES).map(([key, meta]) => html`
      <button type="button" data-entity="${key}" class="${key === this.#entityKey ? 'active' : ''}">
        ${meta.label}
      </button>`));
  }

  #renderOperations() {
    render($('#operations'), Object.entries(QUERY_OPERATIONS).map(([key, op]) => html`
      <button type="button" data-operation="${key}" class="${key === this.#operation ? 'active' : ''}"
              title="${op.describe}">${op.label}</button>`));
  }

  #mountBuilders() {
    // Os painéis anteriores ficaram ligados às mesmas raízes, apontando para o modelo da entidade que saiu.
    this.#criteriaBuilder?.destroy();
    this.#projectionBuilder?.destroy();

    this.#criteriaBuilder = new CriteriaBuilder($('#criteria'), this.#criteria);
    this.#criteriaBuilder.addEventListener('change', () => this.#refresh());

    this.#projectionBuilder = new ProjectionBuilder($('#projection'), this.#projection);
    this.#projectionBuilder.addEventListener('change', () => this.#refresh());

    this.#renderRecipes();
  }

  #renderRecipes() {
    const recipes = recipesFor(this.#entityKey);
    const collection = entity(this.#entityKey).collection ? [COLLECTION_RECIPE] : [];

    render($('#recipes'), [...recipes, ...collection].map((r) => html`
      <button type="button" class="recipe" data-recipe="${r.id}">
        <strong>${r.title}</strong>
        <span>${r.teaches}</span>
      </button>`));
  }

  // -- Requisição ----------------------------------------------------------

  /** O corpo exato que será enviado — é ele que a aba JSON mostra. */
  #requestBody() {
    const body = this.#criteria.toJSON();

    const projection = this.#projection.toJSON();
    if (projection) body.projection = projection;

    const { paging } = QUERY_OPERATIONS[this.#operation];
    if (paging === 'offset') {
      if (this.#paging.offset) body.offset = Number(this.#paging.offset);
      if (this.#paging.limit) body.limit = Number(this.#paging.limit);
    } else if (paging === 'page') {
      body.page = Number(this.#paging.page);
      body.pageSize = Number(this.#paging.pageSize);
    }
    return body;
  }

  #url() {
    return `/api/repo/${entity(this.#entityKey).path}${QUERY_OPERATIONS[this.#operation].suffix}`;
  }

  /** Redesenha as vistas derivadas. Chamado a cada tecla, então não pode custar caro. */
  #refresh() {
    const body = this.#requestBody();

    render($('#request-json'), html`<code>${pretty(body)}</code>`);
    render($('#request-target'), html`<span class="method">POST</span> <code>${this.#url()}</code>`);
    render($('#explanation'), html`<pre>${explain(this.#criteria)}</pre>`);
    render($('#sql'), html`<code>${toSql(this.#criteria)}</code>`);

    const active = this.#criteria.active.length;
    render($('#criteria-summary'), active === 0
      ? html`<span class="off">nenhum campo informado — a consulta devolve todas as linhas</span>`
      : html`<span class="on">${active} campo${active > 1 ? 's' : ''} no filtro</span>`);
  }

  // -- Autenticação --------------------------------------------------------

  #wireAuth() {
    $('#login').addEventListener('submit', async (event) => {
      event.preventDefault();
      const status = $('#auth-status');
      status.textContent = 'autenticando…';
      status.className = 'auth-status pending';
      try {
        await this.#session.login($('#auth-user').value, $('#auth-pass').value);
      } catch (error) {
        status.textContent = error.message;
        status.className = 'auth-status no';
      }
    });

    $('#logout').addEventListener('click', () => this.#session.logout());
  }

  #renderAuthState() {
    const status = $('#auth-status');
    const { authenticated, userName, userId } = this.#session;
    status.textContent = authenticated ? `autenticado — ${userName} (userId ${userId})` : 'não autenticado';
    status.className = `auth-status ${authenticated ? 'yes' : 'no'}`;
    $('#logout').hidden = !authenticated;
  }

  // -- Execução ------------------------------------------------------------

  #wireRun() {
    $('#run').addEventListener('click', () => this.#run());

    on($('#recipes'), 'click', '.recipe', (_e, el) => {
      const id = el.dataset.recipe;
      if (id === COLLECTION_RECIPE.id) {
        COLLECTION_RECIPE.apply(this.#projection);
        this.#projectionBuilder.render();
      } else {
        recipesFor(this.#entityKey).find((r) => r.id === id)?.apply(this.#criteria);
        this.#criteriaBuilder.render();
      }
      this.#refresh();
    });

    on($('#paging'), 'input', 'input', (_e, el) => {
      this.#paging[el.dataset.name] = el.value;
      this.#refresh();
    });

    $('#clear').addEventListener('click', () => {
      this.#criteria.clear();
      this.#criteriaBuilder.render();
      this.#refresh();
    });
  }

  async #run() {
    const target = $('#response');
    render(target, html`<p class="pending">executando…</p>`);

    const exchange = await this.#session.send('POST', this.#url(), this.#requestBody());

    const rows = Array.isArray(exchange.responseBody?.items)
      ? exchange.responseBody.items.length
      : null;

    render(target, html`
      <div class="response-head ${exchange.ok ? 'ok' : 'fail'}">
        <span class="status">${exchange.error ? 'erro de rede' : `HTTP ${exchange.status}`}</span>
        <span class="elapsed">${exchange.elapsedMs} ms</span>
        ${rows !== null ? html`<span class="rows">${rows} linha${rows === 1 ? '' : 's'}</span>` : ''}
        ${exchange.status === 401 ? html`<span class="tip">autentique-se para esta operação</span>` : ''}
      </div>
      <pre><code>${exchange.error ?? pretty(exchange.responseBody)}</code></pre>
    `);
  }
}

// As abas do painel de saída são pura apresentação — não precisam do modelo.
const wireTabs = () => {
  on($('#output-tabs'), 'click', 'button', (_e, el) => {
    for (const button of $('#output-tabs').querySelectorAll('button')) {
      button.classList.toggle('active', button === el);
    }
    for (const panel of document.querySelectorAll('[data-output]')) {
      panel.hidden = panel.dataset.output !== el.dataset.target;
    }
  });
};

wireTabs();
new Application().start();
