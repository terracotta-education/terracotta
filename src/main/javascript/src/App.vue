<template>
	<v-app>
		<v-main>
			<template v-if="hasTokens">
				<router-view :key="$route.fullPath"/>
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
import store from "@/store";

export default {
  name: 'App',

  data: () => ({
    //
  }),
	computed: {
		...mapGetters({
			hasTokens: 'api/hasTokens',
		}),
	},
	methods: {
		...mapActions({
			refreshToken: 'api/refreshToken',
		}),
	},
	created() {
		if (store.state.api.api_token) {
			setInterval(function () {
				this.refreshToken()
			}.bind(this), 1000 * 60 * 59)
		}
	},
	mounted() {
    if (store.state.api.api_token) {
      this.refreshToken()
    }
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
</style>