<template>
	<ul class="component-steps">
		<li
			v-for="(section) in sectionList"
			:key="section.key"
		>
			<template v-if="section.key === currentSection">
				<strong>{{section.name}}</strong>
			</template>
			<template v-else>
				<span>{{section.name}}</span>
			</template>

			<v-stepper
				vertical
				v-if="section.key === currentSection"
			>
				<v-stepper-step
					v-for="(step) in section.steps"
					:key="step.key"
					:complete="section.steps.findIndex((el,i) => {return el.key === step.key}) <= currentStepNum"
					step=""
				>
					{{step.name}}
				</v-stepper-step>
			</v-stepper>
		</li>
	</ul>

</template>

<script>
	export default {
		name: 'Steps',
		props: ['currentSection','currentStep'],

		computed: {
			currentStepNum() {
				return this.sectionList
									.filter(obj => {
										return obj.key === this.currentSection
									})[0]
									.steps.findIndex((el) => {
										return el.key === this.currentStep
									})
			}
		},

		data() {
			return {
				sectionList: [
					{
						key: "design",
						name: "Section 1: Design",
						steps: [
							{
								key: "design_title",
								name: "Title"
							},
							{
								key: "design_description",
								name: "Description"
							},
							{
								key: "design_conditions",
								name: "Conditions"
							},
							{
								key: "design_experiment",
								name: "Experiment type"
							},
						]
					},
					{
						key: "participation",
						name: "Section 2: Participation",
						steps: [
							{
								key: "design_title",
								name: "Title"
							}
						]
					},
					{
						key: "assignments",
						name: "Section 3: Assignments",
						steps: [
							{
								key: "design_title",
								name: "Title"
							}
						]
					}
				]
			}
		}
	}
</script>

<style lang="scss">

@import '~@/styles/variables';
	ul.component-steps {
		list-style: none;
		padding: 0 !important;
		font-size: 15px;

		> li {
			text-align: left;
			margin-bottom: 20px;

			.v-stepper {
				background: $gray-lightest;
				padding: 30px 0 30px 0;

				&__step {
					padding: 0 0 28px 0 !important;

					&:last-child {
						padding-bottom: 0 !important;
					}
				}
			}
		}
	}
	.v-stepper--vertical {
		padding-bottom: 0;
		box-shadow: none !important;

		.v-stepper {
			border-radius: 0;
			padding: 0;

			&__label {
				color: black !important;
			}
			&__step {
				padding: 0;

				> span {
					display: block;
					background:white !important;
					width: 14px !important;
					height: 14px !important;
					min-width: 14px !important;
					border:5px solid #E2E2E2;

					i {
						display: none;
					}
				}
			}
		}
	}
</style>