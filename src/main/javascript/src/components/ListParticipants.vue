<template>
  <div
    v-if="participantsList.length > 0"
    class="list-participants-container mt-4"
  >
    <v-row class="d-flex align-center mx-3">
      <v-row
        class="d-flex align-center mx-4"
      >
        <v-checkbox
          :model-value="tempSelectedInAGroup.length > 0"
          aria-label="select all participants"
          color="primary"
          @update:model-value="handleOnChange(tempSelectedInAGroup.length)"
          hide-details
        />

        {{ tempSelectedInAGroup.length }} Selected
      </v-row>

      <v-menu>
        <template #activator="{ props }">
          <v-btn
            v-bind="props"
            color="primary"
          >
            MOVE TO
          </v-btn>
        </template>

        <v-list
          aria-label="select destination to move participants to"
        >
          <template
            v-for="(option, index) in moveToOptions"
            :key="option"
          >
            <v-list-item
              v-if="index.toString() !== selectedOption"
              @click="
                moveToHandlerComponent(
                  option,
                  tempSelectedInAGroup
                )
              "
            >
              <v-list-item-title>
                {{ option }}
              </v-list-item-title>
            </v-list-item>
          </template>
        </v-list>
      </v-menu>
    </v-row>

    <v-list
      aria-label="select participants to move to another group"
      class="participant-list-group mx-3 px-2"
    >
      <div
        v-for="participant in participantsList"
        :key="participant.userId || participant.participantId"
        class="participant-item-container my-2"
      >
        <v-list-item
          class="participant-item"
          @click="toggleParticipant(participant)"
        >
          <template #prepend>
            <v-icon
              :color="
                isSelected(participant)
                  ? 'primary'
                  : 'grey'
              "
            >
              {{
                isSelected(participant)
                  ? "mdi-checkbox-marked"
                  : "mdi-checkbox-blank-outline"
              }}
            </v-icon>
          </template>

          <v-list-item-title>
            {{ participant.user.displayName }}
          </v-list-item-title>
        </v-list-item>
      </div>
    </v-list>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

defineOptions({
  name: "ListParticipants"
});

const props = defineProps({
  listOfParticipants: {
    type: Array,
    required: true
  },
  moveToOptions: {
    type: Array,
    required: true
  },
  moveToHandler: {
    type: Function,
    required: true
  },
  selectedOption: {
    type: String,
    required: true
  }
});

const tempSelectedInAGroup = ref([]);

const participantsList = computed(() => {
  return props.listOfParticipants || [];
});

watch(
  participantsList,
  () => {
    tempSelectedInAGroup.value =
      tempSelectedInAGroup.value.filter(
        selected =>
          participantsList.value.some(
            participant =>
              getParticipantId(participant) ===
              getParticipantId(selected)
          )
      );
  }
);

const getParticipantId = participant => {
  return (
    participant.participantId ??
    participant.userId ??
    participant.user?.userId
  );
};

const isSelected = participant => {
  return tempSelectedInAGroup.value.some(
    selected =>
      getParticipantId(selected) ===
      getParticipantId(participant)
  );
};

const toggleParticipant = participant => {
  if (isSelected(participant)) {
    tempSelectedInAGroup.value =
      tempSelectedInAGroup.value.filter(
        selected =>
          getParticipantId(selected) !==
          getParticipantId(participant)
      );

    return;
  }

  tempSelectedInAGroup.value = [
    ...tempSelectedInAGroup.value,
    participant
  ];
};

const moveToHandlerComponent = (
  option,
  selectedParticipants
) => {
  props.moveToHandler(
    option,
    selectedParticipants
  );

  tempSelectedInAGroup.value = [];
};

const handleOnChange = selectedCount => {
  tempSelectedInAGroup.value =
    selectedCount === 0
      ? [...participantsList.value]
      : [];
};
</script>

<style lang="scss" scoped>
.mdi-minus-box {
  color: map.get($blue, "base") !important;
}

.participant-list-group {
  border: 1px solid lightgrey;
  border-radius: 10px;

  & .participant-item-container {
    border-radius: 24px;
    overflow: hidden;

    & .participant-item {
      border-bottom: 1px solid lightgrey;
    }
  }
}
</style>
