<template>
    <div>
        <template
            v-if="experiment && experiment.conditions"
        >
            <h1 class="mb-5">
                <span>You have defined <strong>{{ numConditions }} condition{{ hasConditions ? "s" : "" }}</strong></span><br><br>
                <span>How do you want students to be exposed to these different conditions?</span>
            </h1>

            <v-expansion-panels
                :value="expanded"
                class="v-expansion-panels--icon"
                flat
            >
                <v-expansion-panel
                    :class="{'v-expansion-panel--selected': experiment.exposureType === 'WITHIN'}"
                    :key="getExposureTypes.indexOf('WITHIN')"
                >
                    <v-expansion-panel-header hide-actions>
                        <img
                            src="@/assets/all_conditions.svg"
                            alt="all conditions"
                        >
                        All conditions
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                        <p>All students are exposed to every condition, in different orders. This way you can compare how the different conditions affected each individual student. This is called a within-subject design.</p>
                        <v-btn
                            v-if="!hasSelectedExposureType || this.initialExposureType === 'WITHIN'"
                            @click="saveType('WITHIN')"
                            color="primary"
                            elevation="0"
                        >
                            Select
                        </v-btn>
                    </v-expansion-panel-content>
                </v-expansion-panel>
                <v-expansion-panel
                    :class="{'v-expansion-panel--selected': experiment.exposureType === 'BETWEEN'}"
                    :key="getExposureTypes.indexOf('BETWEEN')"
                >
                    <v-expansion-panel-header hide-actions>
                        <img
                            src="@/assets/one_condition.svg"
                            alt="only one condition"
                        >
                        Only one condition
                    </v-expansion-panel-header>
                    <v-expansion-panel-content>
                        <p>Each student is only exposed to one condition, so that you can compare how the different conditions affected different students. This is called a between-subjects design.</p>
                        <v-btn
                            v-if="!hasSelectedExposureType || this.initialExposureType === 'BETWEEN'"
                            @click="saveType('BETWEEN')"
                            color="primary"
                            elevation="0"
                        >
                            Select
                        </v-btn>
                    </v-expansion-panel-content>
                </v-expansion-panel>
            </v-expansion-panels>
            <v-card
                v-if="!hasSelectedExposureType"
                class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
                outlined
            >
                <p>
                    Please note that you will not be able to switch between “All conditions” and “Only one condition” after you click SELECT.
                </p>
                <p>
                    Additionally, once you click SELECT to leave this screen, you will be able to add, but not delete conditions,
                    so please use the back button now to double-check that you have included what you need. To change your decisions beyond this point,
                    you will need to create a new experiment.
                </p>
            </v-card>
            <v-card
                v-if="hasSelectedExposureType"
                class="mt-15 pt-5 px-5 mx-auto blue lighten-5 rounded-lg"
                outlined
            >
                <p>
                    Please note that you are not able to switch between “All conditions” and “Only one condition” as you have previously selected
                    a type. To change the experiment type, please create a new experiment.
                </p>
            </v-card>
        </template>
        <template
            v-else
        >
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
            initialExposureType: null,
            initialExposureTypeIndex: null,
            expanded: []
        }
    },
    watch: {
        initialExposureTypeIndex(newIndex) {
            if (newIndex === -1) {
                return;
            }
            this.expanded = [newIndex];
        }
    },
    computed: {
        ...mapGetters({
            editMode: 'navigation/editMode'
        }),
        conditions() {
            return this.experiment.conditions;
        },
        numConditions() {
            return this.conditions?.length || 0;
        },
        hasConditiions() {
            return this.numConditions > 1;
        },
        getExposureTypes() {
            return ['WITHIN', 'BETWEEN'];
        },
        getSaveExitPage() {
            return this.editMode?.callerPage?.name || 'Home';
        },
        hasSelectedExposureType() {
            return this.getExposureTypes.includes(this.initialExposureType);
        }
    },
    methods: {
        ...mapActions({
            reportStep: 'api/reportStep',
            updateExperiment: 'experiment/updateExperiment'
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
        this.initialExposureType = this.experiment?.exposureType;
        this.initialExposureTypeIndex = this.getExposureTypes.indexOf(this.initialExposureType);
    }
}
</script>
