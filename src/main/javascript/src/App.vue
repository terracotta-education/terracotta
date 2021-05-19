<template>
  <v-app>
    <v-main>
      <router-view :key="$route.fullPath"/>
    </v-main>
  </v-app>
</template>

<script>
export default {
  name: 'App',

  data: () => ({
    //
  }),
};
</script>

<style lang="scss" >
@import '~@/styles/forms.scss';

	h1, h2, h3, h4 {
		font-weight: 400;
		padding-bottom: 10px;
	}
	p {
		padding-bottom: 15px;
	}
	input[type="text"] {
		@extend .outlined-input;
	}
</style>