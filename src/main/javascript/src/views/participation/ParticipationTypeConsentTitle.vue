<template>
  <div>
    <h1>Create a title for your consent assignment</h1>
    <p>This will create an <strong>unpublished consent assignment</strong> in Canvas and will be the way your students
      will read, review and sign your study’s informed consent. The consent assignment will be a prerequisite for your
      first study treatment assignments.</p>
    <form
      @submit.prevent="saveTitle"
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
        :disabled="!title"
        elevation="0"
        color="primary"
        class="mr-4"
        type="submit"
      >
        Next
      </v-btn>
    </form>
  </div>
</template>

<script>
import {mapGetters} from 'vuex';

export default {
  name: 'ParticipationTypeConsentTitle',
  props: ['experiment'],
  computed: {
    ...mapGetters({
      consent: 'consent/consent',
    }),
    title: {
      get() {
        return this.consent.title
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
    saveTitle() {
      this.$router.push({name: 'ParticipationTypeConsentFile', params: {experiment: this.experiment.experimentId}})
    }
  },
}
</script>