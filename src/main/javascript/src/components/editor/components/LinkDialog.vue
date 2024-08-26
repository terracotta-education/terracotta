<template>
  <v-dialog
    v-model="dialog"
    max-width="500px"
  >
    <v-card>
      <v-card-title>
        <span
          class="headline"
        >
          Link URL
        </span>
        <v-spacer />
        <v-btn
          @click="close"
          icon
        >
          <v-icon>
            mdi-close
          </v-icon>
        </v-btn>
      </v-card-title>
      <v-card-text>
        <v-text-field
          class="input-url"
          label="URL"
          v-model="url"
        />
      </v-card-text>
      <v-card-actions>
        <v-btn
          text
          @click="close"
        >
          CLOSE
        </v-btn>

        <v-btn
          :disabled="isDisabled"
          @click="apply"
          text
        >
          APPLY
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: {
    editor: {
      type: Object,
      required: true,
    },
    href: {
      type: String,
      required: false,
    },
  },
  data() {
    return {
      dialog: false,
      url: null,
      resolve: null
    }
  },
  computed: {
    getHref() {
      return this.href || "";
    },
    isDisabled() {
      return !this.url;
    }
  },
  methods: {
    openDialog() {
      this.dialog = true;
      return new Promise(
        (resolve) => {
          this.resolve = resolve;
        }
      );
    },
    apply() {
      this.resolve(this.url);
      this.dialog = false;
    },
    close() {
      this.dialog = false;
    }
  },
  mounted() {
    this.url = this.href;
  }
}
</script>

<style scoped>
.input-url {
  & .v-text-field__slot > label {
    left: 0px !important;
    right: auto !important;
  }
}
</style>
