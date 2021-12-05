/**
 * Deep clone a simple JSON object.
 */
export function clone(obj) {
  return JSON.parse(JSON.stringify(obj));
}
