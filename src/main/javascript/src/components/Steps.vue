<template>
	<ul>
		<li><strong>Section 1: Design</strong></li>
		<li>
			<v-stepper
				v-model="step"
				vertical
			>
				<v-stepper-step
					:complete="step > 1"
					step=""
				>
					Title
				</v-stepper-step>

				<v-stepper-step
					:complete="step > 2"
					step=""
				>
					Description
				</v-stepper-step>
			</v-stepper>
		</li>
		<li>Section 2: Participation</li>
		<li>Section 3: Assignments</li>
	</ul>

</template>

<script>
	export default {
		name: 'Steps',

		components: {},

		data() {
			return {
				step: 0
			}
		}
	}
</script>

<style lang="scss" scoped>
	ul {
		list-style: none;

		li {
			text-align: left;
		}
	}
	.v-stepper--vertical {
		padding-bottom: 0;
		box-shadow: none;

		.v-stepper {
			border-radius: 0;
			padding: 0;

			&__step {
				padding: 0;
			}
		}
		.v-stepper__step__step {
			width: 4px;
			height: 4px;
			min-width: 4px;
		}
	}
</style>