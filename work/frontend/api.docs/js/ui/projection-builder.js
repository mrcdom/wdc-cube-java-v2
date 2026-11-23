/**
 * The projection builder: which fields to fetch and, on the 1:N relation, what to do with the collection.
 *
 * The collection part is the one worth watching: turning on the slice without an ordering shows, in practice, why one
 * calls for the other — slicing without ordering returns rows in an undefined order.
 */

import { html, on, render } from '../core/dom.js';
import { entity } from '../domain/schema.js';
import { CriteriaBuilder } from './criteria-builder.js';

export class ProjectionBuilder extends EventTarget {
  #collectionCriteriaBuilder = null;

  /** See {@link CriteriaBuilder#destroy}: delegation survives the redraw, the model does not. */
  #listeners = new AbortController();

  /** @param {HTMLElement} root @param {import('../domain/projection.js').Projection} projection */
  constructor(root, projection) {
    super();
    this.root = root;
    this.projection = projection;
    this.#wire();
    this.render();
  }

  /** Shuts down the panel and the collection's criteria builder. */
  destroy() {
    this.#listeners.abort();
    this.#collectionCriteriaBuilder?.destroy();
    this.#collectionCriteriaBuilder = null;
  }

  #changed({ redraw = true } = {}) {
    if (redraw) this.render();
    this.dispatchEvent(new CustomEvent('change'));
  }

  #wire() {
    const { signal } = this.#listeners;

    on(this.root, 'change', '[data-role="field"]', (_e, el) => {
      this.projection.toggle(el.dataset.name, el.checked);
      this.#changed({ redraw: false });
    }, { signal });

    on(this.root, 'change', '[data-role="ref-field"]', (_e, el) => {
      this.projection.toggleRef(el.dataset.ref, el.dataset.name, el.checked);
      this.#changed({ redraw: false });
    }, { signal });

    on(this.root, 'change', '[data-role="collection-enabled"]', (_e, el) => {
      this.projection.collection.enabled = el.checked;
      this.#changed();
    }, { signal });

    on(this.root, 'change', '[data-role="shape-field"]', (_e, el) => {
      this.projection.collection.toggleShape(el.dataset.name, el.checked);
      this.#changed({ redraw: false });
    }, { signal });

    on(this.root, 'input', '[data-role="slice"]', (_e, el) => {
      this.projection.collection[el.dataset.name] = el.value === '' ? null : el.value;
      this.#changed({ redraw: false });
      this.#refreshSliceWarning();
    }, { signal });
  }

  /** The warning only makes sense while there is a slice without an ordering — it goes away once one is set. */
  #refreshSliceWarning() {
    const warning = this.root.querySelector('[data-role="slice-warning"]');
    if (!warning) return;
    const { collection } = this.projection;
    const slicing = collection.limit !== null || collection.offset !== null;
    warning.hidden = !(slicing && !collection.criteria.orderBy);
  }

  #scalarFields() {
    return this.projection.meta.fields.map((field) => {
      if (field.kind === 'ref') {
        const refMeta = entity(field.ref);
        const picked = this.projection.refs.get(field.name) ?? new Set();
        return html`
          <fieldset class="ref">
            <legend>${field.label} <code>${field.name}</code> <span class="kind">1:1</span></legend>
            ${refMeta.fields.filter((f) => f.kind !== 'ref').map((f) => html`
              <label class="check">
                <input type="checkbox" data-role="ref-field" data-ref="${field.name}" data-name="${f.name}"
                       ${picked.has(f.name) ? 'checked' : ''}>
                <code>${f.name}</code>
              </label>`)}
            <p class="hint">Requesting only the key avoids the subselect: the value is already in the row, in the foreign key column.</p>
          </fieldset>`;
      }

      return html`
        <label class="check">
          <input type="checkbox" data-role="field" data-name="${field.name}"
                 ${this.projection.selected.has(field.name) ? 'checked' : ''}>
          <code>${field.name}</code>
          <span class="kind">${field.kind}</span>
          ${field.heavy ? html`<span class="heavy" title="Heavy field — fetch it only when needed">heavy</span>` : ''}
        </label>`;
    });
  }

  #collectionSection() {
    const { collection } = this.projection;
    if (!collection) return '';

    const itemMeta = entity(collection.itemEntityKey);

    return html`
      <section class="collection">
        <label class="check strong">
          <input type="checkbox" data-role="collection-enabled" ${collection.enabled ? 'checked' : ''}>
          <code>${collection.meta.name}</code>
          <span class="kind">1:N</span>
        </label>
        <p class="hint">${collection.meta.note}</p>

        ${collection.enabled ? html`
          <div class="collection-body">
            <div class="shape">
              <h4>shape <span class="sub">the shape of each item</span></h4>
              ${itemMeta.fields.filter((f) => f.kind !== 'ref').map((f) => html`
                <label class="check">
                  <input type="checkbox" data-role="shape-field" data-name="${f.name}"
                         ${collection.shape.has(f.name) ? 'checked' : ''}>
                  <code>${f.name}</code>
                </label>`)}
            </div>

            <div class="slice">
              <h4>slice <span class="sub">limit and offset</span></h4>
              <label>limit <input type="number" min="0" data-role="slice" data-name="limit"
                                  value="${collection.limit ?? ''}" placeholder="all"></label>
              <label>offset <input type="number" min="0" data-role="slice" data-name="offset"
                                   value="${collection.offset ?? ''}" placeholder="0"></label>
              <p class="warning" data-role="slice-warning"
                 ${(collection.limit !== null || collection.offset !== null) && !collection.criteria.orderBy ? '' : 'hidden'}>
                Slicing without ordering returns rows in an undefined order — the database promises no order without
                <code>ORDER BY</code>. Pick an ordering below.
              </p>
            </div>

            <div class="where">
              <h4>where <span class="sub">the child entity's criteria</span></h4>
              <div data-role="collection-criteria"></div>
            </div>
          </div>` : ''}
      </section>`;
  }

  render() {
    render(this.root, html`
      <div class="scalar-fields">${this.#scalarFields()}</div>
      ${this.#collectionSection()}
    `);

    // The collection's criteria use the same builder as the main query: the sub-criteria structure is identical to
    // the top-level criteria, and reusing the piece is the most direct way to show it.
    // `render` runs many times; without discarding the previous builder, each redraw would leave one more panel
    // handling the same clicks.
    this.#collectionCriteriaBuilder?.destroy();
    this.#collectionCriteriaBuilder = null;

    const host = this.root.querySelector('[data-role="collection-criteria"]');
    if (host) {
      this.#collectionCriteriaBuilder = new CriteriaBuilder(host, this.projection.collection.criteria);
      this.#collectionCriteriaBuilder.addEventListener('change', () => {
        this.dispatchEvent(new CustomEvent('change'));
        this.#refreshSliceWarning();
      });
    }
  }
}
