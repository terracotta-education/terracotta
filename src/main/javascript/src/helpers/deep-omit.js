export const omitDeep = (obj, omitKeys) => {

  console.log(obj.constructor === Object, omitKeys);

  if (Array.isArray(obj)) {
    return obj.map((v) => omitDeep(v, omitKeys));
  } else if (obj !== null && obj.constructor === Object) {
    for (const key in obj) {
      const value = obj[key];
      if (value !== null && value.constructor === Object || Array.isArray(value)) {
        omitDeep(value, omitKeys);
      } else if (omitKeys.includes(key)) {
        delete obj[key];
      }
    }
  }
  return obj;
};

export default omitDeep;