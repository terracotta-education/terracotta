<template>
	<v-app>
		<v-main>
			<template v-if="hasTokens && userInfo === 'Instructor'">
				<router-view :key="$route.fullPath"/>
			</template>
			<template v-else-if="hasTokens && userInfo === 'Learner'">
				<div class="studentView mt-5">
					<StudentConsent v-if="consent" :experimentId="experimentId" :userId="userId"></StudentConsent>
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
									Error
								</v-col>
							</v-row>
						</v-alert>
					</v-col>
				</v-row>
			</template>
		</v-main>
	</v-app>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import StudentConsent from './views/student/StudentConsent.vue';

export default {
  name: 'App',

  data: () => ({
    //
  }),
  components: {StudentConsent},
	computed: {
		...mapGetters({
			hasTokens: 'api/hasTokens',
			userInfo: 'api/userInfo',
			experimentId: 'api/experimentId',
			consent: 'api/consent',
			userId: 'api/userId',
			lti_token: 'api/lti_token',
		}),
	},
	methods: {
		...mapActions({
			refreshToken: 'api/refreshToken',
			setApiToken: 'api/setApiToken', 
			setLtiToken: 'api/setLtiToken'
		}),
	},
	async created() {
		if (this.lti_token) {
			localStorage.clear()
			await this.refreshToken(this.lti_token)
			setInterval(function () {
				localStorage.clear()
				this.refreshToken(this.lti_token)
			}.bind(this), 1000 * 60 * 59)
		}
	},
	mounted() {
		
      this.refreshToken(this.lti_token)
	}
};
</script>

<style lang="scss" >
	@import "./styles/custom";

	h1, h2, h3, h4 {
		line-height: 1.2;
		font-weight: 400;
		padding-bottom: 10px;
	}
	p {
		padding-bottom: 15px;
	}
	.studentView {
		display: flex;
	}
</style>