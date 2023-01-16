<template>
  <div>
    <h1>Create a title for your consent assignment</h1>
    <p>This will create an <strong>unpublished consent assignment</strong> in Canvas and will be the way your students
      will read, review and sign your study’s informed consent. The consent assignment will be a prerequisite for your
      first study treatment assignments.</p>
    <form
      @submit.prevent="saveTitle('ParticipationTypeConsentFile')"
      class="my-5"
      v-if="experiment && consent"
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
import { mapGetters } from 'vuex';

export default {
  name: 'ParticipationTypeConsentTitle',
  props: ['experiment'],
  computed: {
    ...mapGetters({
      consent: 'consent/consent',
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
        return this.editMode?.callerPage?.name || 'Home';
    },
    title: {
      get() {
        return this.titleProxy === '' ? this.experiment?.consent?.title || this.consent?.title : this.titleProxy
      },
      set(value) {
        this.titleProxy = value
        this.$store.commit('consent/setConsentTitle', value)
      }
    }
  },
  data: () => ({
    titleProxy: '',
    rules: [
      v => v && !!v.trim() || 'Title is required',
      v => (v || '').length <= 255 || 'A maximum of 255 characters is allowed'
    ],
  }),
  methods: {
    saveTitle(path) {
      this.$router.push({name: path, params: {experiment: this.experiment.experimentId}})
    },
    saveExit() {
      this.saveTitle(this.getSaveExitPage)
    }
  },
}
</script>