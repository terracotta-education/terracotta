export function isJson(str) {
  try {
    JSON.parse(str);
  } catch {
    return false;
  }

  return true;
}
