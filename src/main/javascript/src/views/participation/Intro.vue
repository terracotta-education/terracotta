<template>
    <div>
        <h1>In this section, you'll decide how students in your class become participants in your experiment</h1>
        <p>Terracotta can allow students to provide consent to participate, and can also allow you the opportunity to manually indicate who has and hasn't agreed to participant.</p>
        <v-btn elevation="0" color="primary" :to="{name: 'ExperimentParticipationSelectionMethod'}">Continue</v-btn>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'

    export default {
        name: 'ParticipationIntro',
        props: ['experiment'],
        computed: {
            ...mapGetters({
                editMode: 'navigation/editMode'
            }),
            getSaveExitPage() {
                return this.editMode?.callerPage?.name || 'Home';
            }
        },
        methods: {
            saveExit() {
                this.$router.push({
                    name: this.getSaveExitPage,
                    params: {
                        experiment: this.experiment.experimentId
                    }
                })
            }
        }
    }
</script>
