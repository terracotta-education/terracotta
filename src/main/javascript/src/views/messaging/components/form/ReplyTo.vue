<template>
<div
  v-if="isLoaded"
  class="reply-to mb-5 pb-2"
>
  <h4>Reply-to addresses</h4>
  <p
    class="grey--text text--darken-2 pb-0"
  >
    Decide who will receive email replies
  </p>
  <v-combobox
    v-model="replyToSelection"
    :items="replyTo"
    :search-input.sync="newEmail"
    :readonly="readOnly"
    :persistent-hint="true"
    :hide-no-data="true"
    :menu-props="replyToMenuProps"
    :error-messages="errorMessages"
    @keydown.enter="updateReplyTo"
    @blur="updateReplyTo"
    item-text="email"
    item-value="order"
    label="Email addresses"
    hint="Type an email address and press enter to add it"
    return-object
    hide-selected
    multiple
    outlined
    chips
  >
    <template
      v-slot:selection="{ attrs, item }"
    >
        <v-chip
          :close="allowRemoveReplyTo"
          @click:close="removeReplyTo(item.order)"
          v-bind="attrs"
        >
          {{ item.email }}
        </v-chip>
    </template>
  </v-combobox>
</div>
</template>

<script>
import { initValidations } from "@/helpers/messaging/validation.js";

export default {
  props: {
    replyTos: {
      type: Array
    },
    required: {
      type: Boolean,
      default: true
    },
    readOnly: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    replyTo: [],
    newEmail: null,
    replyToSelection: null,
    validationErrors: null,
    hasErrors: false,
    isLoaded: false
  }),
  computed: {
    replyToMenuProps() {
      return !this.newEmail ? { value: false } : {};
    },
    allowAddReplyTo() {
      return this.replyTo.every((replyTo) => this.validateEmail(replyTo.email));
    },
    allowRemoveReplyTo() {
      return this.replyTo.length > 1;
    },
    errorMessages() {
      let errors = [];

      if (!this.validationErrors) {
        return errors;
      }

      if (this.validationErrors.invalid) {
        errors.push(this.validationErrors.invalid);
      }

      if (this.validationErrors.required) {
        errors.push(this.validationErrors.required);
      }

      return errors;
    }
  },
  methods: {
    async updateReplyTo() {
      if (!(this.replyToSelection.length || this.newEmail) && this.required) {
        this.replyToSelection.pop();
        this.replyTo = this.replyToSelection ? [...this.replyToSelection] : [];
        this.$emit("updated", this.replyTo);
        this.$swal({
          title: "Reply-to email is required",
          html: "Please add a reply-to email address.",
          icon: "error",
          confirmButtonText: "Ok",
        });
        this.validationErrors.invalid = "A reply-to email is required.";
        return false;
      }

      if (this.newEmail) {
        if (typeof this.newEmail === "string") {
          if (!this.validateEmail(this.newEmail)) {
            // invalid email; skip adding
            this.$swal({
              title: "Invalid email",
              html: `The email you entered is not valid: <b>${this.newEmail}</b>`,
              icon: "error",
              confirmButtonText: "Ok",
            });
            this.validationErrors.invalid = `The email you entered is not valid: "${this.newEmail}"`;
            this.replyTo = this.replyToSelection ? [...this.replyToSelection] : [];
            this.newEmail = null;
            this.$emit("updated", this.replyTo);
            return false;
          }

          const exists = this.replyToSelection.some(
            reply => reply.email === this.newEmail
          );

          // replace email string with replyTo object, if it does not exist
          if (!exists) {
            this.replyToSelection.push(
              {
                id: null,
                containerConfigurationId: null,
                email: this.newEmail,
                messageConfigurationId: null,
                order: this.replyToSelection.length
              }
            );
          }
        } else {
          // validate replyTo object email
          if (!this.validateEmail(this.newEmail.email)) {
            // invalid email; skip adding
            this.$swal({
              title: "Invalid email",
              html: `The email you entered is not valid: <b>${this.newEmail.email}</b>`,
              icon: "error",
              confirmButtonText: "Ok",
            });
            this.validationErrors.invalid = `The email you entered is not valid: "${this.newEmail.email}"`;
            this.replyTo = this.replyToSelection ? [...this.replyToSelection] : [];
            this.newEmail = null;
            this.$emit("updated", this.replyTo);
            return false;
          }
        }
      }

      this.replyTo = this.replyToSelection ? [...this.replyToSelection] : [];
      this.validationErrors = initValidations().container.replyTo;
      this.$emit("updated", this.replyTo);
      return true;
    },
    removeReplyTo(order) {
      const index = this.replyToSelection.findIndex(reply => reply.order === order);

      if (index === -1) {
        return;
      }

      this.replyToSelection.splice(index, 1);
      this.replyTo = this.replyToSelection;
      this.$emit("updated", this.replyTo);
    },
    validateEmail(email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(email);
    },
    isValid() {
      return !this.hasErrors;
    },
    initializeReplyTo() {
      /*
        NOTE: there is a bug in Vue 2 that removes all values (emails) that are duplicates. It is only fixed in Vue 3.
        https://github.com/vuetifyjs/vuetify/issues/16100

        We add a property "order" to each replyTo object to ensure that we can keep track of the emails.
        The "order" is the value of each replyTo object in the combobox.
      */
      this.replyTo = this.replyTos
          .map(
            (r, i) => ({
              ...r,
              order: i,
            })
        ) || [];
      this.replyToSelection = this.replyTo;
    }
  },
  mounted() {
    this.initializeReplyTo();
    this.validationErrors = initValidations().container.replyTo;
    this.isLoaded = true;
  }
}
</script>

<style scoped>
div.reply-to {
  & .v-input__append-inner {
    display: none;
  }
}
</style>
