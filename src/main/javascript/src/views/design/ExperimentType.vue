<template>
    <div>
        <template v-if="experiment && experiment.conditions">
            <h1 class="mb-5">
                <span>You have defined <strong>{{ numConditions }} conditions</strong></span><br><br>
                <span>How do you want students to be exposed to these different conditions?</span>
            </h1>

            <v-expansion-panels class="v-expansion-panels--icon" flat>
                <v-expansion-panel :class="{'v-expansion-panel--selected': experiment.exposureType === 'WITHIN'}">
                    <v-expansion-panel-header hide-actions>
                        <img src="@/assets/all_conditions.svg" alt="all conditions"> All conditions
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                        <p>All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual student. This is called a within-subject design.</p>
                        <v-btn
                            @click="saveType('WITHIN')"
                            color="primary"
                            elevation="0"
                        >
                            Select
                        </v-btn>
                    </v-expansion-panel-content>
                </v-expansion-panel>
                <v-expansion-panel :class="{'v-expansion-panel--selected': experiment.exposureType === 'BETWEEN'}">
                    <v-expansion-panel-header hide-actions>
                        <img src="@/assets/one_condition.svg" alt="only one condition"> Only one condition
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                        <p>Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a between-subjects design.</p>
                        <v-btn
                            @click="saveType('BETWEEN')"
                            color="primary"
                            elevation="0"
                        >
                            Select
                        </v-btn>
                    </v-expansion-panel-content>
                </v-expansion-panel>
            </v-expansion-panels>
        </template>
        <template v-else>
            <v-alert
                prominent
                type="error"
            >
                <v-row align="center">
                    <v-col class="grow">
                        No conditions found
                    </v-col>
                </v-row>
            </v-alert>
        </template>
    </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
    name: 'ExperimentType',
    props: ['experiment'],
    data() {
        return {
            initialExperimentType: null
        }
    },
    computed: {
        ...mapGetters({
            editMode: 'navigation/editMode'
        }),
        numConditions() {
            return this.experiment?.conditions?.length || 0
        },
        getExposureTypes() {
            return ['BETWEEN', 'WITHIN'];
        },
        getSaveExitPage() {
            return this.editMode?.callerPage?.name || 'Home';
        }
    },
    methods: {
        ...mapActions({
            reportStep: 'api/reportStep',
            updateExperiment: 'experiment/updateExperiment',
            createExposures: 'exposures/createExposures',
            createAndAssignGroups: 'groups/createAndAssignGroups'
        }),
        async saveType(type) {
            const e = this.experiment
            e.exposureType = type

            const experimentId = e.experimentId
            const step = "exposure_type"

            await this.updateExperiment(e)
                .then(
                    async response => {
                        if (typeof response?.status !== "undefined" && response?.status === 200) {
                            if (!this.editMode) {
                                // report the current step
                                await this.reportStep({experimentId, step})
                            }
                            if (this.initialExperimentType !== type) {
                                // experiment type has change; update exposures and groups
                                await this.createExposures(this.experiment.experimentId);
                                await this.createAndAssignGroups(this.experiment.experimentId);
                            }
                            if (this.getExposureTypes.includes(this.experiment.exposureType)) {
                                this.$router.push({
                                    name: 'ExperimentDesignDefaultCondition',
                                    params:{
                                        experiment: experimentId
                                    }
                                });
                            } else {
                                this.$swal("Select an experiment type")
                            }
                        } else if (response?.message) {
                            this.$swal(`Error: ${response.message}`)
                        } else {
                            this.$swal('There was an error saving your experiment.')
                        }
                    }
                )
                .catch(
                    response => {
                        console.error("updateExperiment | catch", {response})
                        this.$swal('There was an error saving the experiment.')
                    }
                )
        },
        async saveExit() {
            this.$router.push({
                name: this.getSaveExitPage,
                params: {
                    experiment: this.experiment.experimentId
                }
            });
        }
    },
    async mounted() {
        this.initialExperimentType = this.experiment?.experimentType;
    }
}
</script>
