/**
 * O construtor de critérios: escolher campo, operador e valores, e ver o filtro tomar forma.
 *
 * Cada campo é uma linha que pode receber vários pedidos. É de propósito que acrescentar um segundo pedido seja fácil:
 * é aí que se percebe que o padrão é `AND` — dois `≠` excluem os dois valores — e que `OR` é uma escolha explícita.
 */

import { $$, html, on, render } from '../core/dom.js';
import { OPERATORS, TEXT_SHORTCUTS, operatorsOf } from '../domain/schema.js';

export class CriteriaBuilder extends EventTarget {
  /** @param {HTMLElement} root @param {import('../domain/criteria.js').Criteria} criteria */
  constructor(root, criteria) {
    super();
    this.root = root;
    this.criteria = criteria;
    this.#wire();
    this.render();
  }

  #changed() {
    this.render();
    this.dispatchEvent(new CustomEvent('change'));
  }

  #criterionOf(element) {
    return this.criteria.criterion(element.closest('[data-field]').dataset.field);
  }

  #wire() {
    on(this.root, 'click', '[data-action="add-predicate"]', (_e, el) => {
      const criterion = this.#criterionOf(el);
      criterion.add(operatorsOf(criterion.field)[0]);
      this.#changed();
    });

    on(this.root, 'click', '[data-action="remove-predicate"]', (_e, el) => {
      this.#criterionOf(el).remove(el.dataset.predicate);
      this.#changed();
    });

    on(this.root, 'click', '[data-action="clear-field"]', (_e, el) => {
      this.#criterionOf(el).clear();
      this.#changed();
    });

    on(this.root, 'change', '[data-role="operator"]', (_e, el) => {
      const criterion = this.#criterionOf(el);
      const predicate = criterion.predicates.find((p) => p.id === el.dataset.predicate);
      const [kind, value] = el.value.split(':');
      if (kind === 'shortcut') {
        predicate.shortcut = value;
        predicate.operator = 'ILIKE';
      } else {
        predicate.shortcut = null;
        predicate.operator = value;
      }
      predicate.values = [];
      this.#changed();
    });

    on(this.root, 'change', '[data-role="disjunctive"]', (_e, el) => {
      this.#criterionOf(el).disjunctive = el.checked;
      this.#changed();
    });

    // `input` em vez de `change`: o JSON e a explicação acompanham a digitação, que é o que faz a relação
    // entre o formulário e o corpo da requisição ficar evidente.
    on(this.root, 'input', '[data-role="value"]', (_e, el) => {
      const criterion = this.#criterionOf(el);
      const predicate = criterion.predicates.find((p) => p.id === el.dataset.predicate);
      predicate.values[Number(el.dataset.index)] = el.value;
      this.dispatchEvent(new CustomEvent('change'));
      this.#refreshBadges();
    });

    on(this.root, 'change', '[data-role="order-by"]', (_e, el) => {
      this.criteria.orderBy = el.value || null;
      this.#changed();
    });
  }

  /** Atualiza só os selos de estado, para não redesenhar campos enquanto se digita neles. */
  #refreshBadges() {
    for (const el of $$('[data-role="state"]', this.root)) {
      const criterion = this.criteria.criterion(el.closest('[data-field]').dataset.field);
      el.textContent = criterion.isSet ? 'filtra' : 'não informado';
      el.className = `state ${criterion.isSet ? 'on' : 'off'}`;
    }
  }

  #operatorOptions(criterion, predicate) {
    const isText = criterion.field.kind === 'text';
    const selected = predicate.shortcut ? `shortcut:${predicate.shortcut}` : `op:${predicate.operator}`;

    const shortcuts = isText
      ? Object.entries(TEXT_SHORTCUTS).map(([key, { label }]) => {
        const value = `shortcut:${key}`;
        return html`<option value="${value}" ${value === selected ? 'selected' : ''}>${label}</option>`;
      })
      : [];

    const operators = operatorsOf(criterion.field).map((op) => {
      const value = `op:${op}`;
      return html`<option value="${value}" ${value === selected ? 'selected' : ''}>${OPERATORS[op].label}</option>`;
    });

    return html`
      ${shortcuts.length ? html`<optgroup label="atalhos de texto">${shortcuts}</optgroup>` : ''}
      ${shortcuts.length ? html`<optgroup label="operadores">${operators}</optgroup>` : operators}
    `;
  }

  #valueInputs(criterion, predicate) {
    const { arity } = predicate;
    if (arity === 0) {
      return html`<span class="no-value">sem valor — o operador já diz tudo</span>`;
    }

    const type = criterion.field.kind === 'number' && !predicate.shortcut ? 'number' : 'text';

    if (arity < 0) {
      return html`
        <input class="value" data-role="value" data-predicate="${predicate.id}" data-index="0" type="text"
               value="${predicate.values[0] ?? ''}" placeholder="valores separados por vírgula">
      `;
    }

    return Array.from({ length: arity }, (_, i) => html`
      <input class="value" data-role="value" data-predicate="${predicate.id}" data-index="${i}" type="${type}"
             value="${predicate.values[i] ?? ''}" placeholder="${arity === 2 ? (i === 0 ? 'de' : 'até') : 'valor'}">
    `);
  }

  #predicateRow(criterion, predicate, index) {
    return html`
      <div class="predicate">
        <span class="joiner">${index === 0 ? '' : (criterion.disjunctive ? 'OU' : 'E')}</span>
        <select data-role="operator" data-predicate="${predicate.id}">
          ${this.#operatorOptions(criterion, predicate)}
        </select>
        ${this.#valueInputs(criterion, predicate)}
        <button type="button" class="icon" data-action="remove-predicate" data-predicate="${predicate.id}"
                title="remover este pedido">✕</button>
      </div>
    `;
  }

  #fieldCard(criterion) {
    const { field } = criterion;
    const many = criterion.predicates.length > 1;

    return html`
      <article class="field" data-field="${field.name}">
        <header>
          <div class="field-id">
            <code>${field.name}</code>
            <span class="kind">${field.kind}</span>
            ${field.derived ? html`<span class="derived" title="${field.note ?? ''}">${field.derived}</span>` : ''}
          </div>
          <span class="state ${criterion.isSet ? 'on' : 'off'}" data-role="state">
            ${criterion.isSet ? 'filtra' : 'não informado'}
          </span>
        </header>

        ${field.note && !field.derived ? html`<p class="note">${field.note}</p>` : ''}

        <div class="predicates">
          ${criterion.predicates.map((p, i) => this.#predicateRow(criterion, p, i))}
          ${criterion.predicates.length === 0
            ? html`<p class="empty">Nenhum pedido. Este campo não entra no filtro.</p>`
            : ''}
        </div>

        <footer>
          <button type="button" class="link" data-action="add-predicate">+ pedido</button>
          ${many ? html`
            <label class="or" title="Vale para o campo inteiro, não só para o pedido seguinte">
              <input type="checkbox" data-role="disjunctive" ${criterion.disjunctive ? 'checked' : ''}>
              combinar com OU
            </label>` : ''}
          ${criterion.predicates.length ? html`<button type="button" class="link muted" data-action="clear-field">limpar</button>` : ''}
        </footer>
      </article>
    `;
  }

  render() {
    render(this.root, html`
      <div class="fields">
        ${this.criteria.criterions.map((c) => this.#fieldCard(c))}
      </div>
      <div class="order-by">
        <label>Ordenação</label>
        <select data-role="order-by">
          <option value="" ${!this.criteria.orderBy ? 'selected' : ''}>— a cargo do banco —</option>
          <option value="ASCENDING" ${this.criteria.orderBy === 'ASCENDING' ? 'selected' : ''}>ASCENDING</option>
          <option value="DESCENDING" ${this.criteria.orderBy === 'DESCENDING' ? 'selected' : ''}>DESCENDING</option>
        </select>
      </div>
    `);
  }
}
