/**
 * O construtor de projeção: quais campos trazer e, na relação 1:N, o que fazer com a coleção.
 *
 * A parte da coleção é o que vale ver: ligar o recorte sem ligar a ordenação mostra, na prática, por que uma coisa
 * pede a outra — cortar sem ordenar devolve linhas em ordem indefinida.
 */

import { html, on, render } from '../core/dom.js';
import { entity } from '../domain/schema.js';
import { CriteriaBuilder } from './criteria-builder.js';

export class ProjectionBuilder extends EventTarget {
  #collectionCriteriaBuilder = null;

  /** Ver {@link CriteriaBuilder#destroy}: a delegação sobrevive ao redesenho, o modelo não. */
  #listeners = new AbortController();

  /** @param {HTMLElement} root @param {import('../domain/projection.js').Projection} projection */
  constructor(root, projection) {
    super();
    this.root = root;
    this.projection = projection;
    this.#wire();
    this.render();
  }

  /** Desliga o painel e o construtor de critério da coleção. */
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

  /** O aviso só faz sentido enquanto houver recorte sem ordem — e some sozinho quando a ordem entra. */
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
            <p class="hint">Pedir só a chave evita o subselect: o valor já está na linha, na coluna da chave estrangeira.</p>
          </fieldset>`;
      }

      return html`
        <label class="check">
          <input type="checkbox" data-role="field" data-name="${field.name}"
                 ${this.projection.selected.has(field.name) ? 'checked' : ''}>
          <code>${field.name}</code>
          <span class="kind">${field.kind}</span>
          ${field.heavy ? html`<span class="heavy" title="Campo pesado — traga só quando precisar">pesado</span>` : ''}
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
              <h4>shape <span class="sub">a forma de cada item</span></h4>
              ${itemMeta.fields.filter((f) => f.kind !== 'ref').map((f) => html`
                <label class="check">
                  <input type="checkbox" data-role="shape-field" data-name="${f.name}"
                         ${collection.shape.has(f.name) ? 'checked' : ''}>
                  <code>${f.name}</code>
                </label>`)}
            </div>

            <div class="slice">
              <h4>recorte <span class="sub">limit e offset</span></h4>
              <label>limit <input type="number" min="0" data-role="slice" data-name="limit"
                                  value="${collection.limit ?? ''}" placeholder="todas"></label>
              <label>offset <input type="number" min="0" data-role="slice" data-name="offset"
                                   value="${collection.offset ?? ''}" placeholder="0"></label>
              <p class="warning" data-role="slice-warning"
                 ${(collection.limit !== null || collection.offset !== null) && !collection.criteria.orderBy ? '' : 'hidden'}>
                Recortar sem ordenar devolve linhas em ordem indefinida — o banco não promete ordem sem
                <code>ORDER BY</code>. Escolha uma ordenação abaixo.
              </p>
            </div>

            <div class="where">
              <h4>where <span class="sub">o critério da entidade filha</span></h4>
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

    // O critério da coleção é o mesmo construtor da consulta principal: a estrutura do sub-critério é idêntica
    // à do critério de topo, e reusar a peça é a forma mais direta de mostrar isso.
    // `render` roda várias vezes; sem descartar o anterior, cada desenho deixaria mais um painel atendendo
    // os mesmos cliques.
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
