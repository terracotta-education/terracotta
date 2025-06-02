<template>
  <div
    class="dropdown-menu"
  >
    <template
      v-if="items.length"
    >
      <button
        v-for="(item, index) in items"
        :class="{ 'is-selected': index === selectedIndex }"
        :key="index"
        @click="selectItem(item)"
      >
        {{ item.label }}
      </button>
    </template>
    <div
      v-else
      class="item"
    >
      No result
    </div>
  </div>
</template>

<script>
export default {
  props: {
    items: {
      type: Array,
      required: true,
    },
    command: {
      type: Function,
      required: true,
    }
  },
  data: () => ({
    selectedIndex: 0
  }),
  watch: {
    items() {
      this.selectedIndex = 0;
    },
  },
  methods: {
    onKeyDown({ event }) {
      switch (event.key) {
        case "ArrowUp":
          this.upHandler();
          break;
        case "ArrowDown":
          this.downHandler();
          break;
        case "Enter":
          this.enterHandler();
          break;
        default:
          return false;
      }

      return true;
    },
    upHandler() {
      this.selectedIndex = ((this.selectedIndex + this.items.length) - 1) % this.items.length;
    },
    downHandler() {
      this.selectedIndex = (this.selectedIndex + 1) % this.items.length;
    },
    enterHandler() {
      this.selectItem(this.selectedIndex);
    },
    selectItem(index) {
      const item = this.items[index];

      if (item) {
        this.command({ id: item.type + "_" + item.id, label: item.label });
      }
    },
  }
}
</script>

<style lang="scss">
.dropdown-menu {
  width: 400px;
  background-color: lightgrey;
  border: 1px solid grey;
  border-radius: 0.7rem;
  box-shadow: darkgrey;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  overflow: auto;
  padding: 0.4rem;
  position: relative;

  button {
    align-items: center;
    background-color: transparent;
    display: flex;
    gap: 0.25rem;
    text-align: left;
    width: 100%;

    &:hover,
    &:hover.is-selected {
      background-color: lightblue;
    }

    &.is-selected {
      background-color: white;
    }
  }
}
</style>
