import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    // Experiment paths
    path: '/experiment/:experiment_id',
    component: () => import('../views/Experiment.vue'),
    children: [
      {
        path: 'experiment-summary',
        name: 'ExperimentSummary',
        component: () => import('../views/ExperimentSummary.vue')
      },
      {
        path: 'exposure/:exposure_id',
        component: () => import('../views/ExperimentOutcome.vue'),
        children: [
          {
            path: 'outcome/:outcome_id/outcome-scoring',
            alias: 'outcome-scoring',
            name: 'OutcomeScoring',
            meta: {
              previousStep: 'ExperimentSummary'
            },
            component: () => import('../views/outcome/OutcomeScoring.vue')
          },
          {
            path: 'outcome-gradebook',
            name: 'OutcomeGradebook',
            meta: {
              previousStep: 'ExperimentSummary'
            },
            component: () => import('../views/outcome/OutcomeGradebook.vue')
          },
          {
            path: 'assignment/:assignment_id/assignment-scores',
            name: 'AssignmentScores',
            meta: {
              previousStep: 'ExperimentSummary'
            },
            component: () => import('../views/grading/AssignmentScores.vue')
          },
        ]
      },
      {
        path: '',
        alias: 'design',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: 'design',
          currentStep: 'design',
          previousStep: 'Home'
        },
        children: [
          // Experiment | Design Steps paths
          {
            path: '',
            alias: 'intro',
            name: 'ExperimentDesignIntro',
            component: () => import('../views/design/Intro.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design',
              previousStep: 'Home'
            }
          },
          {
            path: 'title',
            name: 'ExperimentDesignTitle',
            component: () => import('../views/design/Title.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_title',
              previousStep: 'ExperimentDesignIntro',
            }
          },
          {
            path: 'description',
            name: 'ExperimentDesignDescription',
            component: () => import('../views/design/Description.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_description',
              previousStep: 'ExperimentDesignTitle'
            }
          },
          {
            path: 'conditions',
            name: 'ExperimentDesignConditions',
            component: () => import('../views/design/Conditions.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_conditions',
              previousStep: 'ExperimentDesignDescription'
            }
          },
          {
            path: 'type',
            name: 'ExperimentDesignType',
            component: () => import('../views/design/ExperimentType.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_type',
              previousStep: 'ExperimentDesignConditions'
            }
          },
          {
            path: 'default-condition',
            name: 'ExperimentDesignDefaultCondition',
            component: () => import('../views/design/DefaultCondition.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_type',
              previousStep: 'ExperimentDesignType'
            }
          },
          {
            path: 'summary',
            name: 'ExperimentDesignSummary',
            component: () => import('../views/design/Summary.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_type',
              stepsComplete: true,
              previousStep: 'ExperimentDesignType'
            }
          },
        ]
      },
      {
        path: 'participation',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: 'participation',
          previousStep: 'ExperimentDesignSummary'
        },
        children: [
          // Experiment | Participation Steps paths
          {
            path: '',
            alias: 'intro',
            name: 'ExperimentParticipationIntro',
            component: () => import('../views/participation/Intro.vue'),
            meta: {
              currentSection: 'participation',
              currentStep: 'participation',
              previousStep: 'ExperimentDesignSummary'
            }
          },
          {
            path: 'selection-method',
            name: 'ExperimentParticipationSelectionMethod',
            component: () => import('../views/participation/SelectionMethod.vue'),
            meta: {
              currentSection: 'participation',
              currentStep: 'participation_selection_method',
              previousStep: 'ExperimentParticipationIntro'
            }
          },
          {
            path: 'participation-type-auto-confirm',
            name: 'ParticipationTypeAutoConfirm',
            component: () => import('../views/participation/ParticipationTypeAutoConfirm.vue'),
            meta: {
              selectionType: 'auto',
              currentSection: 'participation',
              currentStep: 'participation_selection_method',
              previousStep: 'ExperimentParticipationSelectionMethod'
            }
          },
          {
            path: 'participation-type-consent/intro',
            name: 'ParticipationTypeConsentOverview',
            component: () => import('../views/participation/ParticipationTypeConsentOverview.vue'),
            meta: {
              selectionType: 'consent',
              currentSection: 'participation',
              currentStep: 'participation_selection_method',
              previousStep: 'ExperimentParticipationSelectionMethod'
            }
          },
          {
            path: 'participation-type-consent/title',
            name: 'ParticipationTypeConsentTitle',
            component: () => import('../views/participation/ParticipationTypeConsentTitle.vue'),
            meta: {
              selectionType: 'consent',
              currentSection: 'participation',
              currentStep: 'participation_selection_consent_title',
              previousStep: 'ParticipationTypeConsentOverview'
            }
          },
          {
            path: 'participation-type-consent/file',
            name: 'ParticipationTypeConsentFile',
            component: () => import('../views/participation/ParticipationTypeConsentFile.vue'),
            meta: {
              selectionType: 'consent',
              currentSection: 'participation',
              currentStep: 'participation_selection_consent_file',
              previousStep: 'ParticipationTypeConsentTitle'
            }
          },
          {
            path: 'manual-selection',
            name: 'ParticipationTypeManual',
            component: () => import('../views/participation/manual-participation/ParticipationTypeManual.vue'),
            meta: {
              selectionType: 'manual',
              currentSection: 'participation',
              currentStep: 'participation_selection_method',
              previousStep: 'ExperimentParticipationSelectionMethod'
            }
          },
          {
            path: 'manual-participant-selection',
            name: 'ParticipationTypeManualSelection',
            component: () => import('../views/participation/manual-participation/ParticipationTypeManualSelection.vue'),
            meta: {
              selectionType: 'manual',
              currentSection: 'participation',
              currentStep: 'select_participants',
              previousStep: 'ParticipationTypeManual'
            }
          },
          {
            path: 'participant-distribution',
            name: 'ParticipationDistribution',
            component: () => import('../views/participation/distribution/ParticipationDistribution.vue'),
            meta: {
              selectionType: 'any',
              currentSection: 'participation',
              currentStep: 'participation_distribution',
              previousStep: 'ExperimentParticipationSelectionMethod'
            }
          },
          {
            path: 'participant-custom-distribution',
            name: 'ParticipationCustomDistribution',
            component: () => import('../views/participation/distribution/ParticipationCustomDistribution.vue'),
            meta: {
              selectionType: 'any',
              currentSection: 'participation',
              currentStep: 'participation_distribution',
              previousStep: 'ParticipationDistribution'
            }
          },
          {
            path: 'participant-manual-distribution',
            name: 'ParticipationManualDistribution',
            component: () => import('../views/participation/distribution/ParticipationManualDistribution.vue'),
            meta: {
              selectionType: 'any',
              currentSection: 'participation',
              currentStep: 'participation_distribution',
              previousStep: 'ParticipationDistribution'
            }
          },
          {
            path: 'participation-summary',
            name: 'ParticipationSummary',
            component: () => import('../views/participation/ParticipationSummary.vue'),
            meta: {
              currentSection: 'participation',
              currentStep: 'select_participants',
              previousStep: 'ParticipationDistribution'
            }
          }
        ]
      },
      {
        path: 'assignments',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: 'assignments',
          previousStep: 'ParticipationSummary'
        },
        children: [
          // Experiment | Assignments Steps paths
          {
            path: '',
            alias: 'intro',
            name: 'AssignmentIntro',
            component: () => import('../views/assignment/Intro.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'assignment_intro',
              previousStep: 'ParticipationSummary'
            }
          },
          {
            path: 'exposure-sets',
            name: 'AssignmentExposureSets',
            component: () => import('../views/assignment/ExposureSets.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'assignment_intro',
              previousStep: 'AssignmentIntro'
            }
          },
          {
            path: 'exposure-sets/:exposure_id/intro',
            name: 'AssignmentExposureSetsIntro',
            component: () => import('../views/assignment/ExposureSetsIntro.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'assignment_intro',
              previousStep: 'AssignmentExposureSets'
            }
          },
          {
            path: 'exposure-sets/:exposure_id/assignments',
            name: 'AssignmentYourAssignments',
            component: () => import('../views/assignment/YourAssignments.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'your_assignments',
              previousStep: 'AssignmentExposureSets'
            }
          },
          {
            path: 'exposure-sets/:exposure_id/create-assignment',
            name: 'AssignmentCreateAssignment',
            component: () => import('../views/assignment/CreateAssignment.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'your_assignments',
              previousStep: 'AssignmentExposureSets'
            }
          },
          {
            path: 'exposure-sets/:exposure_id/assignment/:assignment_id/select-assignment-treatment',
            name: 'AssignmentTreatmentSelect',
            component: () => import('../views/assignment/AssignmentTreatmentSelect.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'your_assignments',
              previousStep: 'AssignmentYourAssignments'
            }
          },
          {
            path: 'exposure-sets/:exposure_id/assignment/:assignment_id/condition/:condition_id/treatment/:treatment_id/assessment/:assessment_id/builder',
            name: 'TerracottaBuilder',
            component: () => import('../views/assignment/TerracottaBuilder.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'your_assignments',
              previousStep: 'AssignmentTreatmentSelect'
            }
          },
        ]
      },
    ]
  },
  {
    path: '*',
    component: Home
  },
]

const router = new VueRouter({
  base: process.env.BASE_URL,
  routes
})

export default router
