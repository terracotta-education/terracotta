export const hasDuplicateValues = {
  methods: {
    hasDuplicateValues(arr, key) {
      const uniqueValues = new Set(arr.map(c => c[key]))
      return uniqueValues.size < arr.length
    }
  }
}