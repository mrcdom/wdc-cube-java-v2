/**
 * O mínimo de ajuda para montar DOM sem framework.
 *
 * `html` escapa o que é interpolado — um valor digitado não vira marcação — mas devolve um {@link Html}, que é marcação
 * já pronta. Assim um `html` aninhado dentro de outro não é escapado duas vezes, e listas de fragmentos entram direto,
 * sem `join`. Onde a marcação vem de fora, `raw` a assume como confiável.
 */

/** Marcação pronta: o que sai de `html` e o que `raw` promete ser seguro. */
class Html {
  #value;

  constructor(value) {
    this.#value = value;
  }

  toString() {
    return this.#value;
  }
}

/** Assume um trecho como marcação confiável. */
export const raw = (value) => new Html(String(value));

const escapeHtml = (value) => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;');

const interpolate = (value) => {
  if (value === null || value === undefined || value === false) return '';
  if (value instanceof Html) return value.toString();
  if (Array.isArray(value)) return value.map(interpolate).join('');
  return escapeHtml(value);
};

/** Monta marcação, escapando os valores interpolados. */
export const html = (strings, ...values) => raw(
  strings.reduce((acc, str, i) => acc + str + (i < values.length ? interpolate(values[i]) : ''), ''),
);

/** Substitui o conteúdo de um elemento pela marcação informada. */
export const render = (target, markup) => {
  target.innerHTML = markup instanceof Html ? markup.toString() : interpolate(markup);
  return target;
};

export const $ = (selector, scope = document) => scope.querySelector(selector);
export const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

/**
 * Delegação de eventos: um ouvinte na raiz atende os elementos que casam com o seletor, inclusive os que ainda nem
 * existem. É o que permite redesenhar um painel inteiro sem religar ouvinte nenhum.
 *
 * <b>O `signal` não é opcional na prática.</b> Quem monta um painel novo sobre a mesma raiz precisa descartar os
 * ouvintes do anterior: eles continuam ligados à raiz, apontando para o modelo velho, e passam a responder por dados
 * que já não existem. Um {@link AbortController} por painel resolve os dois lados — registra e descarta em bloco.
 */
export const on = (root, type, selector, handler, { signal } = {}) => {
  root.addEventListener(type, (event) => {
    const target = event.target.closest(selector);
    if (target && root.contains(target)) handler(event, target);
  }, { signal });
};

/** JSON legível, com as chaves na ordem em que foram postas. */
export const pretty = (value) => JSON.stringify(value, null, 2);
