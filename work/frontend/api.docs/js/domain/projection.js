/**
 * A projeção: quais campos trazer, e — quando há relação 1:N — o que fazer com a coleção.
 *
 * Um campo presente na projeção entra no SELECT; ausente, nem é consultado. O valor que se põe é irrelevante: serve de
 * sentinela, e é por isso que a interface só oferece caixas de seleção.
 *
 * A coleção projetada é o caso que merece atenção. Ela não é uma lista de resultados: é *uma* forma de item mais o
 * critério que a filtra e o recorte a aplicar. No fio isso vira um envelope, distinto do array de resultado pelo
 * próprio formato:
 *
 *     "items": { "shape": {…um item…}, "where": {…critério…}, "limit": 5, "offset": 1 }
 *
 * Na resposta, o mesmo campo volta como o array de sempre. Quem lê decide pelo tipo do valor.
 */

import { Criteria } from './criteria.js';
import { entity } from './schema.js';

/** Sentinelas: qualquer valor não nulo marca o campo como pedido. O tipo só evita surpresa na desserialização. */
const SENTINEL = { number: 1, text: '~', date: '2000-01-01T00:00:00Z', ref: null };

export class Projection {
  /** @param {string} entityKey */
  constructor(entityKey) {
    this.entityKey = entityKey;
    this.meta = entity(entityKey);
    /** @type {Set<string>} campos escalares pedidos */
    this.selected = new Set(this.meta.fields.filter((f) => !f.heavy && f.kind !== 'ref').map((f) => f.name));
    /** @type {Map<string, Set<string>>} campos pedidos dentro de cada relação 1:1 */
    this.refs = new Map();
    /** @type {CollectionProjection|null} */
    this.collection = this.meta.collection ? new CollectionProjection(this.meta.collection) : null;
  }

  toggle(name, on) {
    if (on) this.selected.add(name);
    else this.selected.delete(name);
  }

  toggleRef(refName, fieldName, on) {
    const fields = this.refs.get(refName) ?? new Set();
    if (on) fields.add(fieldName);
    else fields.delete(fieldName);
    if (fields.size) this.refs.set(refName, fields);
    else this.refs.delete(refName);
  }

  /** Monta o objeto de projeção, ou `undefined` quando nada foi pedido — aí o servidor usa a projeção padrão. */
  toJSON() {
    const json = {};

    for (const field of this.meta.fields) {
      if (field.kind === 'ref') {
        const picked = this.refs.get(field.name);
        if (picked?.size) {
          const refMeta = entity(field.ref);
          json[field.name] = Object.fromEntries(
            [...picked].map((n) => [n, SENTINEL[refMeta.fields.find((f) => f.name === n)?.kind ?? 'text']]),
          );
        }
      } else if (this.selected.has(field.name)) {
        json[field.name] = SENTINEL[field.kind] ?? '~';
      }
    }

    const collection = this.collection?.toJSON();
    if (collection) json[this.meta.collection.name] = collection;

    return Object.keys(json).length ? json : undefined;
  }
}

/** A coleção projetada — forma, critério e recorte. */
export class CollectionProjection {
  constructor(meta) {
    this.meta = meta;
    this.enabled = false;
    this.itemEntityKey = meta.of;
    const itemMeta = entity(meta.of);
    this.shape = new Set(itemMeta.fields.filter((f) => f.kind !== 'ref').map((f) => f.name));
    this.criteria = new Criteria(meta.of);
    this.limit = null;
    this.offset = null;
  }

  toggleShape(name, on) {
    if (on) this.shape.add(name);
    else this.shape.delete(name);
  }

  /** O envelope, ou `undefined` quando a coleção não foi pedida. */
  toJSON() {
    if (!this.enabled) return undefined;

    const itemMeta = entity(this.itemEntityKey);
    const shape = Object.fromEntries(
      [...this.shape].map((n) => [n, SENTINEL[itemMeta.fields.find((f) => f.name === n)?.kind ?? 'text']]),
    );

    const envelope = { shape };

    const where = this.criteria.toJSON();
    if (Object.keys(where).length) envelope.where = where;
    if (this.limit !== null && this.limit !== '') envelope.limit = Number(this.limit);
    if (this.offset !== null && this.offset !== '') envelope.offset = Number(this.offset);

    return envelope;
  }
}
