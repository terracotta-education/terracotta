/**
 * Compares strings the way people read them, with runs of digits compared as numbers, so
 * "Group 2" sorts before "Group 10". Use as a sort comparator.
 */
export const naturalCompare = (a, b) => {
  return String(a).localeCompare(String(b), undefined, { numeric: true });
};
