<template>
    <div>
        <template v-if="experiment">
            <div class="experiment-steps">
                <aside
                    v-if="!this.noSidebar.includes(this.$router.currentRoute.name)"
                    class="experiment-steps__sidebar">
                    <steps
                        :current-section="currentSection"
                        :current-step="currentStep"
                        :participationType="experiment.participationType"
                    />
                </aside>
                <nav>
                    <router-link
                        v-if="this.editModePage"
                        :to="{
                            name: this.editModePage,
                        }"
                    >
                        <v-icon>mdi-chevron-left</v-icon> Back
                    </router-link>
                    <v-btn
                        v-show="this.$router.currentRoute.name !== 'ExperimentDesignIntro'"
                        color="primary"
                        elevation="0"
                        class="saveButton"
                        @click="$refs.childComponent.saveExit()"
                    >
                        <span v-if="this.$router.currentRoute.meta.stepActionText">{{ this.$router.currentRoute.meta.stepActionText }}</span>
                        <span v-else-if="editMode">SAVE & CLOSE</span>
                        <span v-else>SAVE & EXIT</span>
                    </v-btn>
                </nav>
                <article class="experiment-steps__body">
                    <v-container>
                        <v-row justify="center">
                            <v-col md="6">
                                <router-view
                                    :key="$route.fullPath"
                                    ref="childComponent"
                                    :experiment="experiment"
                                >
                                </router-view>
                            </v-col>
                        </v-row>
                    </v-container>
                </article>
            </div>
        </template>
        <template v-else>
            <v-row justify="center">
                <v-col md="6">
                    <v-alert
                        prominent
                        type="error"
                    >
                        <v-row align="center">
                            <v-col class="grow">
                                Experiment not found
                            </v-col>
                        </v-row>
                    </v-alert>
                </v-col>
            </v-row>
        </template>
    </div>
</template>

<script>
    import Steps from '../components/Steps'
    import store from '@/store'
    import {mapActions, mapGetters} from "vuex";

    export default {
        name: 'ExperimentSteps',

        data: () => ({}),

        computed: {
            ...mapGetters({
                experiment: 'experiment/experiment',
                editMode: 'navigation/editMode'
            }),
            currentSection() {
                return this.$router.currentRoute.meta.currentSection
            },
            currentStep() {
                return this.$router.currentRoute.meta.currentStep
            },
            routeExperimentId() {
                return this.$route.params.experiment_id
            },
            noSidebar() {
                // these pages should not show the sidebar
                return ['TerracottaBuilder', 'AssignmentCreateAssignment', 'AssignmentEditor'];
            },
            editModePage() {
                if (this.editMode?.initialPage === this.$router.currentRoute.name) {
                    // this page is where we initially began, return to the caller page on exit
                    return this.editMode.callerPage.name;
                }
                return this.$router.currentRoute.meta.previousStep;
            }
        },

        beforeRouteEnter (to, from, next) {
            // don't load new data after consent title screen
            if (from.name==='ParticipationTypeConsentTitle' && to.name==='ParticipationTypeConsentFile') { next(); return;}
            return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
        },
        beforeRouteUpdate (to, from, next) {
            // don't load new data after consent title screen
            if (from.name==='ParticipationTypeConsentTitle' && to.name==='ParticipationTypeConsentFile') { next(); return;}
            return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
        },

        methods: {
            ...mapActions({
                fetchExperimentById: 'experiment/fetchExperimentById'
            }),
        },

        components: {
            Steps
        }
    }
</script>

<style lang="scss" scoped>
    @import '~vuetify/src/styles/main.sass';
    @import '~@/styles/variables';

    .experiment-steps {
        display: grid;
        min-height: 100%;
        grid-template-rows: auto 1fr;
        grid-template-columns: auto 1fr;
        grid-template-areas:
            "aside nav"
            "aside article";

        > nav {
            position: fixed;
            top: 0;
            width: 100%;
            height: 50px;
            grid-area: nav;
            padding: 30px;
            display: flex;
            justify-content: space-between;
            z-index: 100;
            background: white;
            a {
                text-decoration: none;

                * {
                    vertical-align: sub;
                    @extend .blue--text;
                }
            }
            .saveButton {
                margin-left:auto;
                background: none!important;
                border: none;
                padding: 0!important;
                color: #069;
                cursor: pointer;
            }
        }
        > aside {
            grid-area: aside;
        }
        > article {
            grid-area: article;
            padding: 0;
            padding-top: 100px !important;
        }

        &__sidebar {
            background: map-get($grey, 'lighten-4');
            padding: 30px 45px;
        }
    }
</style>