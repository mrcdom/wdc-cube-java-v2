/**
 * The projection: which fields to fetch and — when there is a 1:N relation — what to do with the collection.
 *
 * A field present in the projection goes into the SELECT; absent, it is not even queried. The value set is
 * irrelevant: it serves as a sentinel, which is why the interface offers only checkboxes.
 *
 * The projected collection is the case that deserves attention. It is not a list of results: it is *one* item shape
 * plus the criteria that filter it and the slice to apply. On the wire this becomes an envelope, told apart from the
 * result array by its very format:
 *
 *     "items": { "shape": {…one item…}, "where": {…criteria…}, "limit": 5, "offset": 1 }
 *
 * In the response, the same field comes back as the usual array. The reader decides by the value's type.
 */

import { Criteria } from './criteria.js';
import { entity } from './schema.js';

/** Sentinels: any non-null value marks the field as requested. The type only avoids surprises in deserialization. */
const SENTINEL = { number: 1, text: '~', date: '2000-01-01T00:00:00Z', ref: null };

export class Projection {
  /** @param {string} entityKey */
  constructor(entityKey) {
    this.entityKey = entityKey;
    this.meta = entity(entityKey);
    /** @type {Set<string>} requested scalar fields */
    this.selected = new Set(this.meta.fields.filter((f) => !f.heavy && f.kind !== 'ref').map((f) => f.name));
    /** @type {Map<string, Set<string>>} fields requested inside each 1:1 relation */
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

  /** Builds the projection object, or `undefined` when nothing was requested — then the server uses its default. */
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

/** The projected collection — shape, criteria, and slice. */
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

  /** The envelope, or `undefined` when the collection was not requested. */
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
