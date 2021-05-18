<template>
	<router-view :key="$route.fullPath"></router-view>
</template>

<script>

	export default {
		name: 'Experiment',

		data: () => ({}),
	}
</script>
