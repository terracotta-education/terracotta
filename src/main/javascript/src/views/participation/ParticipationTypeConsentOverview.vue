<template>
    <div>
        <h1 class="mb-5">Because you would like <strong>students to be invited to agree to participate in study within Canvas</strong>, we’ll need to create a consent assignment.</h1>
        <v-btn elevation="0" color="primary" class="mb-4" @click="setConsent">Continue</v-btn>
    </div>
</template>

<script>
    import { mapGetters } from "vuex";

    export default {
        name: 'ParticipationTypeConsentOverview',
        props: ['experiment'],
        computed: {
            ...mapGetters({
                consent: "consent/consent",
                editMode: 'navigation/editMode'
            }),
            getSaveExitPage() {
                return this.editMode?.callerPage?.name || 'Home';
            }
        },
        methods: {
            setConsent() {
                this.$router.push({name:'ParticipationTypeConsentTitle', params:{experiment: this.experiment.experimentId}})
            },
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