<template>
  <div>
    <v-row>
    <div
      class="col-6"
    >
        <v-text-field
            v-model="assignment.title"
            label="Component name"
            outlined
        ></v-text-field>
    </div>
    </v-row>
    <p>This will create an unpublished component shell in {{ lmsTitle }} and will be the way Terracotta will deliver treatments to students.</p>
    <v-divider />
    <v-tabs
      v-model="tab"
      class="tabs"
    >
      <v-tab>Settings</v-tab>
    </v-tabs>
    <v-divider />
    <v-tabs-items
      v-model="tab"
    >
        <v-tab-item
          class="my-5"
        >
          <assignment-settings />
        </v-tab-item>
    </v-tabs-items>
    <v-btn
      v-if="!this.editMode"
      :disabled="contDisabled"
      @click="saveNext('AssignmentYourAssignments')"
      elevation="0"
      color="primary"
      class="mr-4"
    >
      Continue
    </v-btn>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import AssignmentSettings from './AssignmentSettings.vue';

export default {
  name: "AssignmentEditor",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  data: () => ({
    tab: null
  }),
  computed: {
    ...mapGetters({
      assignment: "assignment/assignment",
      editMode: "navigation/editMode",
      configurations: "configuration/get"
    }),
    experimentId() {
      return parseInt(this.$route.params.experimentId);
    },
    assignmentId() {
      return parseInt(this.$route.params.assignmentId);
    },
    exposureId() {
      return parseInt(this.$route.params.exposureId);
    },
    contDisabled() {
      return (
        !this.assignment.title
      );
    },
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    },
    lmsTitle() {
      return this.configurations?.lmsTitle || "LMS";
    }
  },
  methods: {
    async saveExit() {
      if (!this.contDisabled) {
        const savedAssignment = await this.handleSaveAssignment();
        if (savedAssignment) {
          this.$router.push({
            name: this.getSaveExitPage,
            params: {
              experiment: this.experiment.experimentId
            }
          });
        }
      }
    },
    ...mapActions({
      updateAssignment: 'assignment/updateAssignment',
      fetchAssignment: 'assignment/fetchAssignment',
    }),
    async saveNext(routeName) {
      const savedAssignment = await this.handleSaveAssignment();
      if (savedAssignment) {
        this.$router.push({
          name: routeName,
          params: {
            experiment: this.experiment.experimentId,
            exposureId: isNaN(this.exposureId) ? this.$route.params.exposureId : this.exposureId
          }
        });
      }
    },
    async handleSaveAssignment() {
      // PUT ASSESSMENT TITLE & HTML (description) & SETTINGS
      const response = await this.updateAssignment([
        this.experimentId,
        this.exposureId,
        this.assignmentId,
        {
            ...this.assignment
        }
      ]);

      console.log(response);

      if (response.status === 400) {
        this.$swal(response.data);
        return false;
      }
      return response;
    },
  },
  async created() {
    await this.fetchAssignment([
      this.experiment.experimentId,
      this.exposureId,
      this.assignmentId,
    ]);
  },
  components: {
    AssignmentSettings,
  },
};
</script>
