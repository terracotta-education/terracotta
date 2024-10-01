<template>
	<router-view
		:key="$route.fullPath"
	/>
</template>

<script>
export default {
	name: 'Experiment',

	data: () => ({}),
}
</script>
