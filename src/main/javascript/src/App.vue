<template>
  <v-app>
    <v-main>
      <router-view :key="$route.fullPath"/>
    </v-main>
  </v-app>
</template>

<script>
import { mapActions } from "vuex";
import store from "@/store";

export default {
  name: 'App',

  data: () => ({
    //
  }),
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
};
</script>

<style lang="scss" >
	h1, h2, h3, h4 {
		line-height: 1.2;
		font-weight: 400;
		padding-bottom: 10px;
	}
	p {
		padding-bottom: 15px;
	}
</style>