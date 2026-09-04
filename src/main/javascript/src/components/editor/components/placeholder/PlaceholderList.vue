<template>
  <div class="dropdown-menu">
    <template v-if="items.length">
      <button
        v-for="(item, index) in items"
        :key="index"
        :class="{ 'is-selected': index === selectedIndex }"
        type="button"
        @click="selectItem(index)"
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

<script setup>
import {
  ref,
  watch
} from "vue";

defineOptions({
  name: "SuggestionDropdown"
});

const props = defineProps({
  items: {
    type: Array,
    required: true
  },
  command: {
    type: Function,
    required: true
  }
});

const selectedIndex = ref(0);

watch(
  () => props.items,
  () => {
    selectedIndex.value = 0;
  }
);

const upHandler = () => {
  if (!props.items?.length) {
    return;
  }

  selectedIndex.value =
    (selectedIndex.value + props.items.length - 1) %
    props.items.length;
};

const downHandler = () => {
  if (!props.items?.length) {
    return;
  }

  selectedIndex.value =
    (selectedIndex.value + 1) %
    props.items.length;
};

const selectItem = index => {
  const item = props.items[index];

  if (!item) {
    return;
  }

  props.command({
    id: `${item.type}_${item.id}`,
    label: item.label
  });
};

const enterHandler = () => {
  selectItem(selectedIndex.value);
};

const onKeyDown = ({ event }) => {
  switch (event.key) {
    case "ArrowUp":
      upHandler();
      break;

    case "ArrowDown":
      downHandler();
      break;

    case "Enter":
      enterHandler();
      break;

    default:
      return false;
  }

  return true;
};

defineExpose({
  onKeyDown
});
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
