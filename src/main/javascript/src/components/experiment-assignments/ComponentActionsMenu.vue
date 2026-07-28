<template>
  <v-menu
    v-model="isOpen"
    class="component-actions-menu"
    location="start"
  >
    <template #activator="{ props: menuProps }">
      <v-btn
        v-bind="menuProps"
        aria-label="actions"
        icon="mdi-dots-horizontal"
        variant="text"
      />
    </template>

    <v-list>
      <v-list-item
        v-if="showMoveAction"
        :aria-label="`move ${row.title}`"
        @click="$emit('move', row)"
      >
        <v-list-item-title class="d-flex justify-content-center">
          <v-icon>mdi-arrow-right-top</v-icon>
          Move
        </v-list-item-title>
      </v-list-item>

      <v-list-item
        :aria-label="`edit ${row.title}`"
        @click="$emit('edit', row)"
      >
        <v-list-item-title class="d-flex justify-content-center">
          <v-icon>mdi-pencil</v-icon>
          Edit
        </v-list-item-title>
      </v-list-item>

      <v-list-item
        :aria-label="`duplicate ${row.title}`"
        @click="$emit('duplicate', row)"
      >
        <v-list-item-title class="d-flex justify-content-center">
          <v-icon>mdi-content-duplicate</v-icon>
          Duplicate
        </v-list-item-title>
      </v-list-item>

      <v-list-item
        v-if="showDeleteComponent"
        :aria-label="`delete ${row.title}`"
        @click="$emit('delete', row)"
      >
        <v-list-item-title class="d-flex justify-content-center">
          <v-icon>mdi-delete</v-icon>
          Delete
        </v-list-item-title>
      </v-list-item>

      <v-list-item
        v-if="showPublishComponent"
        :aria-label="`publish ${row.title}`"
        @click="$emit('publish', row)"
      >
        <v-list-item-title class="d-flex justify-content-center">
          <v-icon>mdi-publish</v-icon>
          Publish
        </v-list-item-title>
      </v-list-item>

      <v-list-item
        v-if="showUnpublishComponent"
        :aria-label="`unpublish ${row.title}`"
        @click="$emit('unpublish', row)"
      >
        <v-list-item-title class="d-flex justify-content-center">
          <v-icon>mdi-publish-off</v-icon>
          Unpublish
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup>
import { computed } from "vue";
import { message as messageStatus } from "@/helpers/messaging/status.js";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  row: {
    type: Object,
    required: true
  },
  canDeleteAssignment: {
    type: Boolean,
    default: false
  },
  exposureCount: {
    type: Number,
    default: 1
  },
  hasIncompleteTreatments: {
    type: Function,
    required: true
  }
});

const emit = defineEmits([
  "update:modelValue",
  "move",
  "edit",
  "duplicate",
  "delete",
  "publish",
  "unpublish"
]);

const rowType = {
  assignment: "assignment",
  message: "message"
};

const isOpen = computed({
  get() {
    return props.modelValue;
  },
  set(value) {
    emit("update:modelValue", value);
  }
});

const showMoveAction = computed(() => {
  if (props.exposureCount <= 1) {
    return false;
  }

  if (props.row.type === rowType.assignment) {
    return true;
  }

  if (props.row.type === rowType.message) {
    return ![
      messageStatus.queued,
      messageStatus.processing,
      messageStatus.sent,
      messageStatus.deleted
    ].includes(props.row.configuration.status) &&
      props.row.treatments.every(treatment => treatment.status !== messageStatus.sent);
  }

  return false;
});

const showDeleteComponent = computed(() => {
  if (props.row.type === rowType.assignment) {
    return props.canDeleteAssignment;
  }

  if (props.row.type === rowType.message) {
    return ![
      messageStatus.queued,
      messageStatus.sent,
      messageStatus.deleted
    ].includes(props.row.configuration.status) &&
      props.row.treatments.every(treatment => treatment.status !== messageStatus.sent);
  }

  return false;
});

const showPublishComponent = computed(() => {
  if (props.row.type !== rowType.message) {
    return false;
  }

  return props.row.configuration.status === messageStatus.unpublished &&
    !props.hasIncompleteTreatments(props.row);
});

const showUnpublishComponent = computed(() => {
  return props.row.type === rowType.message &&
    props.row.configuration.status === messageStatus.published;
});
</script>
