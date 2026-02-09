<template>
  <div>
    <h1>Create a title for your consent assignment</h1>
    <p>This will create an <strong>unpublished consent assignment</strong> in {{ lmsTitle }} and will be the way your students
      will read, review and sign your study's informed consent. The consent assignment will be a prerequisite for your
      first study treatment assignments.</p>
    <form
      v-if="experiment && consent"
      @submit.prevent="saveTitle('ParticipationTypeConsentFile')"
      class="my-5"
    >
      <v-text-field
        v-model="title"
        :rules="rules"
        label="Assignment title"
        placeholder="e.g. Lorem ipsum"
        autofocus
        outlined
        required
      ></v-text-field>
      <v-btn
        :disabled="!title || !title.trim() || title.length > 255"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </form>

    <v-card
      class="mt-10 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
      outlined
    >
      <p><strong>Note:</strong> Once the assignment is created, you won't be able to change the title.</p>
    </v-card>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "ParticipationTypeConsentTitle",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    titleProxy: "",
    rules: [
      v => v && !!v.trim() || "Title is required",
      v => (v || "").length <= 255 || "A maximum of 255 characters is allowed"
    ],
  }),
  computed: {
    ...mapGetters({
      consent: "consent/consent",
      editMode: "navigation/editMode",
      configurations: "configuration/get"
    }),
    getSaveExitPage() {
        return this.editMode?.callerPage?.name || "Home";
    },
    title: {
      get() {
        return this.titleProxy === "" ? this.experiment?.consent?.title || this.consent?.title : this.titleProxy;
      },
      set(value) {
        this.titleProxy = value;
        this.setConsentTitle(value);
      }
    },
    lmsTitle() {
      return this.configurations?.lmsTitle || "LMS";
    }
  },
  methods: {
    ...mapActions({
      setConsentTitle: "consent/setConsentTitle"
    }),
    saveTitle(path) {
      this.$router.push({name: path, params: {experiment: this.experiment.experimentId}})
    },
    saveExit() {
      this.saveTitle(this.getSaveExitPage)
    }
  }
}
</script>
