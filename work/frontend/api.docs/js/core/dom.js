/**
 * The bare minimum of help for building DOM without a framework.
 *
 * `html` escapes whatever is interpolated — a typed value does not become markup — but returns an {@link Html}, which
 * is ready-made markup. That way an `html` nested inside another is not escaped twice, and lists of fragments go in
 * directly, without `join`. Where the markup comes from outside, `raw` takes it as trusted.
 */

/** Ready-made markup: what comes out of `html` and what `raw` promises is safe. */
class Html {
  #value;

  constructor(value) {
    this.#value = value;
  }

  toString() {
    return this.#value;
  }
}

/** Takes a snippet as trusted markup. */
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

/** Builds markup, escaping the interpolated values. */
export const html = (strings, ...values) => raw(
  strings.reduce((acc, str, i) => acc + str + (i < values.length ? interpolate(values[i]) : ''), ''),
);

/** Replaces an element's content with the given markup. */
export const render = (target, markup) => {
  target.innerHTML = markup instanceof Html ? markup.toString() : interpolate(markup);
  return target;
};

export const $ = (selector, scope = document) => scope.querySelector(selector);
export const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

/**
 * Event delegation: one listener on the root serves the elements that match the selector, including those that do not
 * exist yet. It is what lets a whole panel be redrawn without rewiring any listener.
 *
 * <b>The `signal` is not optional in practice.</b> Whoever mounts a new panel on the same root must discard the
 * previous one's listeners: they stay attached to the root, pointing at the old model, and end up answering for data
 * that no longer exists. One {@link AbortController} per panel covers both sides — it registers and discards in bulk.
 */
export const on = (root, type, selector, handler, { signal } = {}) => {
  root.addEventListener(type, (event) => {
    const target = event.target.closest(selector);
    if (target && root.contains(target)) handler(event, target);
  }, { signal });
};

/** Readable JSON, with the keys in the order they were added. */
export const pretty = (value) => JSON.stringify(value, null, 2);
