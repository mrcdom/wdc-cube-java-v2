/**
 * The criteria builder: pick a field, an operator, and values, and watch the filter take shape.
 *
 * Each field is a row that can take several requests. Adding a second request is easy on purpose: that is where you
 * notice the default is `AND` — two `≠` exclude both values — and that `OR` is an explicit choice.
 */

import { $$, html, on, render } from '../core/dom.js';
import { OPERATORS, TEXT_SHORTCUTS, operatorsOf } from '../domain/schema.js';

export class CriteriaBuilder extends EventTarget {
  /**
   * Discards all of this panel's listeners at once.
   *
   * Delegation attaches the listener to the root, which survives an entity switch. Without this discard, the previous
   * panel would keep handling clicks with the old criteria — which lack the new entity's fields.
   */
  #listeners = new AbortController();

  /** @param {HTMLElement} root @param {import('../domain/criteria.js').Criteria} criteria */
  constructor(root, criteria) {
    super();
    this.root = root;
    this.criteria = criteria;
    this.#wire();
    this.render();
  }

  /** Shuts the panel down. Required before mounting another one on the same root. */
  destroy() {
    this.#listeners.abort();
  }

  #changed() {
    this.render();
    this.dispatchEvent(new CustomEvent('change'));
  }

  #criterionOf(element) {
    return this.criteria.criterion(element.closest('[data-field]').dataset.field);
  }

  #wire() {
    const { signal } = this.#listeners;

    on(this.root, 'click', '[data-action="add-predicate"]', (_e, el) => {
      const criterion = this.#criterionOf(el);
      criterion.add(operatorsOf(criterion.field)[0]);
      this.#changed();
    }, { signal });

    on(this.root, 'click', '[data-action="remove-predicate"]', (_e, el) => {
      this.#criterionOf(el).remove(el.dataset.predicate);
      this.#changed();
    }, { signal });

    on(this.root, 'click', '[data-action="clear-field"]', (_e, el) => {
      this.#criterionOf(el).clear();
      this.#changed();
    }, { signal });

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
    }, { signal });

    on(this.root, 'change', '[data-role="disjunctive"]', (_e, el) => {
      this.#criterionOf(el).disjunctive = el.checked;
      this.#changed();
    }, { signal });

    // `input` rather than `change`: the JSON and the explanation follow the typing, which is what makes the link
    // between the form and the request body evident.
    on(this.root, 'input', '[data-role="value"]', (_e, el) => {
      const criterion = this.#criterionOf(el);
      const predicate = criterion.predicates.find((p) => p.id === el.dataset.predicate);
      predicate.values[Number(el.dataset.index)] = el.value;
      this.dispatchEvent(new CustomEvent('change'));
      this.#refreshBadges();
    }, { signal });

    on(this.root, 'change', '[data-role="order-by"]', (_e, el) => {
      this.criteria.orderBy = el.value || null;
      this.#changed();
    }, { signal });
  }

  /** Updates only the state badges, so fields are not redrawn while being typed into. */
  #refreshBadges() {
    for (const el of $$('[data-role="state"]', this.root)) {
      const criterion = this.criteria.criterion(el.closest('[data-field]').dataset.field);
      el.textContent = criterion.isSet ? 'filters' : 'not set';
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
      ${shortcuts.length ? html`<optgroup label="text shortcuts">${shortcuts}</optgroup>` : ''}
      ${shortcuts.length ? html`<optgroup label="operators">${operators}</optgroup>` : operators}
    `;
  }

  #valueInputs(criterion, predicate) {
    const { arity } = predicate;
    if (arity === 0) {
      return html`<span class="no-value">no value — the operator says it all</span>`;
    }

    const type = criterion.field.kind === 'number' && !predicate.shortcut ? 'number' : 'text';

    if (arity < 0) {
      return html`
        <input class="value" data-role="value" data-predicate="${predicate.id}" data-index="0" type="text"
               value="${predicate.values[0] ?? ''}" placeholder="comma-separated values">
      `;
    }

    return Array.from({ length: arity }, (_, i) => html`
      <input class="value" data-role="value" data-predicate="${predicate.id}" data-index="${i}" type="${type}"
             value="${predicate.values[i] ?? ''}" placeholder="${arity === 2 ? (i === 0 ? 'from' : 'to') : 'value'}">
    `);
  }

  #predicateRow(criterion, predicate, index) {
    return html`
      <div class="predicate">
        <span class="joiner">${index === 0 ? '' : (criterion.disjunctive ? 'OR' : 'AND')}</span>
        <select data-role="operator" data-predicate="${predicate.id}">
          ${this.#operatorOptions(criterion, predicate)}
        </select>
        ${this.#valueInputs(criterion, predicate)}
        <button type="button" class="icon" data-action="remove-predicate" data-predicate="${predicate.id}"
                title="remove this request">✕</button>
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
            ${criterion.isSet ? 'filters' : 'not set'}
          </span>
        </header>

        ${field.note && !field.derived ? html`<p class="note">${field.note}</p>` : ''}

        <div class="predicates">
          ${criterion.predicates.map((p, i) => this.#predicateRow(criterion, p, i))}
          ${criterion.predicates.length === 0
            ? html`<p class="empty">No requests. This field is not part of the filter.</p>`
            : ''}
        </div>

        <footer>
          <button type="button" class="link" data-action="add-predicate">+ request</button>
          ${many ? html`
            <label class="or" title="Applies to the whole field, not just the next request">
              <input type="checkbox" data-role="disjunctive" ${criterion.disjunctive ? 'checked' : ''}>
              combine with OR
            </label>` : ''}
          ${criterion.predicates.length ? html`<button type="button" class="link muted" data-action="clear-field">clear</button>` : ''}
        </footer>
      </article>
    `;
  }

  /** Shows which columns the chosen ordering sorts by — that is what sets it apart from "sorting by a field". */
  #orderingHint() {
    const chosen = (this.criteria.meta.orderings ?? []).find((o) => o.name === this.criteria.orderBy);
    if (!chosen) {
      return html`<span class="ordering-hint">Each option is a provisioned ordering, not a field.</span>`;
    }
    return html`<span class="ordering-hint"><code>${chosen.name}</code> → <code>ORDER BY ${chosen.sql}</code></span>`;
  }

  render() {
    render(this.root, html`
      <div class="fields">
        ${this.criteria.criterions.map((c) => this.#fieldCard(c))}
      </div>
      <div class="order-by">
        <label>Ordering</label>
        <select data-role="order-by">
          <option value="" ${!this.criteria.orderBy ? 'selected' : ''}>— up to the database —</option>
          ${(this.criteria.meta.orderings ?? []).map((o) => html`
            <option value="${o.name}" title="ORDER BY ${o.sql}"
                    ${this.criteria.orderBy === o.name ? 'selected' : ''}>${o.label}</option>`)}
        </select>
        ${this.#orderingHint()}
      </div>
    `);
  }
}
