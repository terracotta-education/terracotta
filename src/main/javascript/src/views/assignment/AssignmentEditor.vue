<template>
  <div>
    <v-row>
    <div class="col-6">
        <v-text-field
            v-model="assignment.title"
            label="Component name"
            outlined
        ></v-text-field>
    </div>
    </v-row>
    <p>This will create an unpublished component shell in Canvas and will be the way Terracotta will deliver treatments to students.</p>
    <v-divider class=""></v-divider>
    <v-tabs v-model="tab" class="tabs">
      <v-tab>Settings</v-tab>
    </v-tabs>
    <v-divider class=""></v-divider>
    <v-tabs-items v-model="tab">
        <v-tab-item class="my-5">
            <assignment-settings />
        </v-tab-item>
    </v-tabs-items>
    <v-btn
      v-if="!this.editMode"
      :disabled="contDisabled"
      elevation="0"
      color="primary"
      class="mr-4"
      @click="saveNext('AssignmentYourAssignments')"
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
  props: ['experiment'],
  data() {
    return {
      tab: null,
    };
  },
  computed: {
    ...mapGetters({
      assignment: "assignment/assignment",
      editMode: "navigation/editMode"
    }),
    experiment_id() {
      return parseInt(this.$route.params.experiment_id);
    },
    assignment_id() {
      return parseInt(this.$route.params.assignment_id);
    },
    exposure_id() {
      return parseInt(this.$route.params.exposure_id);
    },
    condition_id() {
      return parseInt(this.$route.params.condition_id);
    },
    contDisabled() {
      return (
        !this.assignment.title
      );
    },
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
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
            exposure_id: isNaN(this.exposure_id) ? this.$route.params.exposure_id : this.exposure_id
          }
        });
      }
    },
    async handleSaveAssignment() {
      // PUT ASSESSMENT TITLE & HTML (description) & SETTINGS
      const response = await this.updateAssignment([
        this.experiment_id,
        this.exposure_id,
        this.assignment_id,
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
    console.log(this.experiment.experimentId,
      this.exposure_id,
      this.assignment_id,)
    await this.fetchAssignment([
      this.experiment.experimentId,
      this.exposure_id,
      this.assignment_id,
    ]);
  },
  components: {
    AssignmentSettings,
  },
};
</script>
