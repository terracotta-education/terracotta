<template>
	<v-app>
		<v-main>
			<template v-if="hasTokens && userInfo === 'Instructor'">
				<router-view :key="$route.fullPath"/>
			</template>
			<template v-else-if="hasTokens && userInfo === 'Learner'">
				<div class="studentView mt-5">
					<StudentConsent v-if="consent" :experimentId="experimentId" :userId="userId"></StudentConsent>
        <StudentQuiz v-if="!consent && assignmentId" :experimentId="experimentId" :assignmentId="assignmentId" ></StudentQuiz>
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
import StudentQuiz from './views/student/StudentQuiz.vue';
import StudentConsent from './views/student/StudentConsent.vue';

export default {
  name: 'App',

  data: () => ({
    //
  }),
  components: {
    StudentQuiz,
    StudentConsent
  },
	computed: {
		...mapGetters({
			hasTokens: 'api/hasTokens',
			userInfo: 'api/userInfo',
			experimentId: 'api/experimentId',
      assignmentId: 'api/assignmentId',
			consent: 'api/consent',
			userId: 'api/userId',
			api_token: 'api/api_token',
			lmsApiOAuthURL: 'api/lmsApiOAuthURL',
		}),
	},
	methods: {
		...mapActions({
			refreshToken: 'api/refreshToken',
		}),
	},
	async created() {
    localStorage.clear()
    setInterval(function () {
      this.refreshToken(this.api_token)
    }.bind(this), 1000 * 60 * 59)

	if (this.lmsApiOAuthURL) {
		await this.$swal("We need to redirect you back to Canvas so that you can authorize Terracotta to do things on your behalf");
		window.location.replace(this.lmsApiOAuthURL);
	}
	},
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
