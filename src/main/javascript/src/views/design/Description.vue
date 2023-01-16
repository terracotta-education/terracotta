<template>
    <div>
        <h1>Describe your experiment</h1>
        <p>Use this space to write down some notes about your research question, why this question is meaningful, and your hypothesis.</p>
        <form
            @submit.prevent="saveExperiment('ExperimentDesignConditions')"
            class="my-5 mb-15"
            v-if="experiment"
        >
            <v-textarea
                v-model="experiment.description"
                :rules="requiredText"
                label="Experiment description"
                placeholder="e.g. Lorem ipsum"
                outlined
                required
            ></v-textarea>
            <v-btn
                v-if="!this.editMode"
                :disabled="!experiment.description || !experiment.description.trim()"
                elevation="0"
                color="primary"
                class="mr-4"
                type="submit"
            >
                Next
            </v-btn>
        </form>

        <h4 class="mb-3">Examples</h4>
        <v-carousel
            v-model="slide"
            hide-delimiters
            height="auto"
            light
        >
            <v-carousel-item
                v-for="(blurb, i) in blurbs"
                :key="i"
            >
                <v-card
                    class="pt-5 px-5 mx-auto"
                    outlined
                >
                    <v-card-text>
                        <p>{{ blurb }}</p>
                    </v-card-text>
                </v-card>
            </v-carousel-item>
        </v-carousel>
    </div>
</template>

<script>
import {mapActions, mapGetters} from "vuex";

export default {
    name: 'DesignDescription',
    props: ['experiment'],
    data: () => ({
        requiredText: [
            v => v && !!v.trim() || 'Description is required'
        ],
        slide: 0,
        blurbs: [
            "The study looks to test whether exposure to ones' own lexile level (and seeing how it improves over time) might improve students' learning outcomes.",
            "The study explores how learning about the biology of skin color (vs. social construction of race) impacts students’ conception of race. It is important for science teachers to understand the impacts of teaching about skin color biology and how it may impact students' concepts of race.",
            "The purpose of this study is to evaluate the benefits of explicitly including student learning outcomes in the assignment description.  The TILT movement hypothesizes that this will help students be in better control of their learning and will reduce inequity.",
            "This study will test whether presenting assessment questions before students watch an instructional video (“pre-questions”) will improve learning outcomes, compared with presenting the same questions after students watch the video.",
            "This experiment tests whether multiple-choice questions can improve critical thinking performance in Introductory Psychology.  Some students will categorize critical thinking scenarios, while others will answer conventional practice questions about brain structures and psychological functions.",
            "In this study, students will see worked examples of math problems, and will then solve similar problems.  Some of the worked examples will include a common mistake along with a correction, and other worked examples will be entirely correct.  We will examine how exposure to mistakes affects student performance."
        ]
    }),
    computed: {
        ...mapGetters({
            editMode: 'navigation/editMode'
        }),
        getSaveExitPage() {
            return this.editMode?.callerPage?.name || 'Home';
        }
    },
    methods: {
        ...mapActions({
            updateExperiment: 'experiment/updateExperiment',
        }),
        saveExperiment(path) {
            const _this = this
            const e = _this.experiment

            this.updateExperiment(e)
                    .then(response => {
            if (typeof response?.status !== "undefined" && response?.status === 200) {
                this.$router.push({
                    name: path,
                    params: {
                        experiment: this.experiment.experiment_id
                    }
                })
            } else if (response?.message) {
              this.$swal(`Error: ${response.message}`)
            } else {
              this.$swal('There was an error saving your experiment.')
            }
                    })
                    .catch(response => {
            console.error("updateExperiment | catch", {response})
            this.$swal('There was an error saving the experiment.')
                    })
        },
        saveExit() {
            this.saveExperiment(this.getSaveExitPage)
        }
    }
}
</script>

<style lang="scss">
    @import '~vuetify/src/styles/main.sass';
    @import '~@/styles/variables';

    .v-window.v-carousel {
        overflow: unset !important;

        .v-window__container {
            //overflow: visible;
        }

        .v-window__prev,
        .v-window__next {

            .v-btn--icon.v-size--default {
                background: white;
                height: 40px;
                width: 40px;

                @extend .elevation-4 !optional;

                i.v-icon {
                    color: black;
                    font-size: 20px !important;
                }
            }
        }
        .v-window__prev {
            left: -32px;
        }
        .v-window__next {
            right: -32px;
        }
    }
</style>