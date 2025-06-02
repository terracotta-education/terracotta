<template>
<div
  class="type-radio mb-2"
>
  <h4>{{ label }}</h4>
  <v-radio-group
    v-model="selectedType"
    :disabled="readOnly"
    :hide-details="!validationErrors"
    :error-messages="validationErrors"
    @change="handleTypeAlert"
    column
    dense
  >
    <v-radio
      v-for="type in types"
      :key="type.value"
      :value="type.value"
      :label="type.label"
      color="blue"
      class="mb-5"
    />
  </v-radio-group>
</div>
</template>

<script>
import { validations } from "@/helpers/messaging/validation";

export default {
  props: {
    type: {
      type: String,
      required: false
    },
    readOnly: {
      type: Boolean,
      default: false
    },
    label: {
      type: String
    },
    validatedErrors: {
      type: Object,
      default: null
    }
  },
  data: () => ({
    selectedType: null,
    validationErrors: null,
    types: [
      {
        value: "EMAIL",
        label: "Email"
      },
      {
        value: "CONVERSATION",
        label: "Canvas message"
      }
    ]
  }),
  watch: {
    type: {
      handler(newType) {
        this.selectedType = newType;
      },
      immediate: true,
    },
    validatedErrors: {
      handler(newValidatedErrors) {
        this.validationErrors = newValidatedErrors || validations.container.type;
      },
      immediate: true
    }
  },
  methods: {
    async handleTypeAlert(newType) {
      if (newType === "CONVERSATION" && this.type === "EMAIL") {
        await this.$swal({
          title: "Are you sure you want to switch?",
          html: "Changing from email to Canvas message will <b>erase all your formatting</b>. Do you want to proceed?",
          showCancelButton: true,
          confirmButtonText: "Yes, continue",
          cancelButtonText: "Cancel",
        })
        .then((result) => {
            const typeToSet = result.isConfirmed ? "CONVERSATION" : "EMAIL";
            this.$emit("updated", typeToSet);
            this.selectedType = typeToSet;
        });
      } else {
        this.$emit("updated", newType);
      }
    },
  },
}
</script>

<style scoped>
.type-radio {
  & .v-input__slot {
    margin-bottom: 0;
    max-width: fit-content;
    padding-right: 12px;
  }
}
</style>
