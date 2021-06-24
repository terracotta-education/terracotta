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
        path: 'summary',
        name: 'ExperimentSummary',
        component: () => import('../views/ExperimentSummary.vue')
      },
      {
        path: '',
        alias: 'design',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: 'design',
          currentStep: 'design'
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
              currentStep: 'design'
            }
          },
          {
            path: 'title',
            name: 'ExperimentDesignTitle',
            component: () => import('../views/design/Title.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_title'
            }
          },
          {
            path: 'description',
            name: 'ExperimentDesignDescription',
            component: () => import('../views/design/Description.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_description'
            }
          },
          {
            path: 'conditions',
            name: 'ExperimentDesignConditions',
            component: () => import('../views/design/Conditions.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_conditions'
            }
          },
          {
            path: 'type',
            name: 'ExperimentDesignType',
            component: () => import('../views/design/ExperimentType.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_type'
            }
          },
          {
            path: 'default-condition',
            name: 'ExperimentDesignDefaultCondition',
            component: () => import('../views/design/DefaultCondition.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_type'
            }
          },
          {
            path: 'summary',
            name: 'ExperimentDesignSummary',
            component: () => import('../views/design/Summary.vue'),
            meta: {
              currentSection: 'design',
              currentStep: 'design_type',
              stepsComplete: true
            }
          },
        ]
      },
      {
        path: 'participation',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: 'participation'
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
              currentStep: 'participation'
            }
          },
          {
            path: 'selection-method',
            name: 'ExperimentParticipationSelectionMethod',
            component: () => import('../views/participation/SelectionMethod.vue'),
            meta: {
              currentSection: 'participation',
              currentStep: 'participation_selection_method'
            }
          },
          {
            path: 'participation-type-auto-confirm',
            name: 'ParticipationTypeAutoConfirm',
            component: () => import('../views/participation/ParticipationTypeAutoConfirm.vue'),
            meta: {
              selectionType: 'auto',
              currentSection: 'participation',
              currentStep: 'participation_selection_method'
            }
          },
          {
            path: 'participation-type-consent/intro',
            name: 'ParticipationTypeConsentOverview',
            component: () => import('../views/participation/ParticipationTypeConsentOverview.vue'),
            meta: {
              selectionType: 'consent',
              currentSection: 'participation',
              currentStep: 'participation_selection_method'
            }
          },
          {
            path: 'participation-type-consent/title',
            name: 'ParticipationTypeConsentTitle',
            component: () => import('../views/participation/ParticipationTypeConsentTitle.vue'),
            meta: {
              selectionType: 'consent',
              currentSection: 'participation',
              currentStep: 'participation_selection_consent_title'
            }
          },
          {
            path: 'participation-type-consent/file',
            name: 'ParticipationTypeConsentFile',
            component: () => import('../views/participation/ParticipationTypeConsentFile.vue'),
            meta: {
              selectionType: 'consent',
              currentSection: 'participation',
              currentStep: 'participation_selection_consent_file'
            }
          },
          {
            path: 'manual-selection',
            name: 'ParticipationTypeManual',
            component: () => import('../views/participation/manual-participation/ParticipationTypeManual.vue'),
            meta: {
              selectionType: 'manual',
              currentSection: 'participation',
              currentStep: 'participation_selection_method'
            }
          },
          {
            path: 'manual-participant-selection',
            name: 'ParticipationTypeManualSelection',
            component: () => import('../views/participation/manual-participation/ParticipationTypeManualSelection.vue'),
            meta: {
              selectionType: 'manual',
              currentSection: 'participation',
              currentStep: 'select_participants'
            }
          },
          {
            path: 'participant-distribution',
            name: 'ParticipationDistribution',
            component: () => import('../views/participation/distribution/ParticipationDistribution.vue'),
            meta: {
              selectionType: 'any',
              currentSection: 'participation',
              currentStep: 'participation_distribution'
            }
          },
          {
            path: 'participant-manual-distribution',
            name: 'ParticipationCustomDistribution',
            component: () => import('../views/participation/distribution/ParticipationCustomDistribution.vue'),
            meta: {
              selectionType: 'any',
              currentSection: 'participation',
              currentStep: 'participation_distribution'
            }
          },
          {
            path: 'participation-summary',
            name: 'ParticipationSummary',
            component: () => import('../views/participation/ParticipationSummary.vue'),
            meta: {
              currentSection: 'participation',
              currentStep: 'select_participants',
            }
          }
        ]
      },
      {
        path: 'assignments',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: 'assignments'
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
              currentStep: 'assignment_intro'
            }
          },
          {
            path: 'exposure-sets',
            name: 'AssignmentExposureSets',
            component: () => import('../views/assignment/ExposureSets.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'assignment_intro'
            }
          },
          {
            path: 'exposure-sets/intro',
            name: 'AssignmentExposureSetsIntro',
            component: () => import('../views/assignment/ExposureSetsIntro.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'assignment_intro'
            }
          },
          {
            path: 'create-assignment',
            name: 'AssignmentCreateAssignment',
            component: () => import('../views/assignment/CreateAssignment.vue'),
            meta: {
              currentSection: 'assignments',
              currentStep: 'your_assignments'
            }
          },
        ]
      },
    ]
  },
  {
    path: '*',
    name: 'Home',
    component: Home
  },
]

const router = new VueRouter({
  base: process.env.BASE_URL,
  routes
})

export default router
