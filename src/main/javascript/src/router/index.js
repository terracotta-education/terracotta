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
        path: '',
        alias: 'design',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: "design",
          currentStep: "design"
        },
        children: [
          // Experiment | Design Steps paths
          {
            path: '',
            alias: 'intro',
            name: 'ExperimentDesignIntro',
            component: () => import('../views/design/Intro.vue'),
            meta: {
              currentSection: "design",
              currentStep: "design"
            }
          },
          {
            path: 'title',
            name: 'ExperimentDesignTitle',
            component: () => import('../views/design/Title.vue'),
            meta: {
              currentSection: "design",
              currentStep: "design_title"
            }
          },
          {
            path: 'description',
            name: 'ExperimentDesignDescription',
            component: () => import('../views/design/Description.vue'),
            meta: {
              currentSection: "design",
              currentStep: "design_description"
            }
          },
          {
            path: 'conditions',
            name: 'ExperimentDesignConditions',
            component: () => import('../views/design/Conditions.vue'),
            meta: {
              currentSection: "design",
              currentStep: "design_conditions"
            }
          },
          {
            path: 'type',
            name: 'ExperimentDesignType',
            component: () => import('../views/design/ExperimentType.vue'),
            meta: {
              currentSection: "design",
              currentStep: "design_type"
            }
          },
          {
            path: 'default-condition',
            name: 'ExperimentDesignDefaultCondition',
            component: () => import('../views/design/DefaultCondition.vue'),
            meta: {
              currentSection: "design",
              currentStep: "design_type"
            }
          },
          {
            path: 'summary',
            name: 'ExperimentDesignSummary',
            component: () => import('../views/design/Summary.vue'),
            meta: {
              currentSection: "design",
              currentStep: "design_type",
              stepsComplete: true
            }
          },
        ]
      },
      {
        path: 'participation',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: "participation"
        },
        children: [
          // Experiment | Participation Steps paths
          {
            path: '',
            alias: 'intro',
            name: 'ExperimentParticipationIntro',
            component: () => import('../views/participation/Intro.vue'),
            meta: {
              currentSection: "participation",
              currentStep: "participation"
            }
          },
          {
            path: 'selection-method',
            name: 'ExperimentParticipationSelectionMethod',
            component: () => import('../views/participation/SelectionMethod.vue'),
            meta: {
              currentSection: "participation",
              currentStep: "participation_selection_method"
            }
          },
          {
            path: 'participation-type-auto-confirm',
            name: 'ExperimentParticipationAutoConfirm',
            component: () => import('../views/participation/ParticipationTypeAutoConfirm.vue'),
            meta: {
              currentSection: "participation",
              currentStep: "participation_selection_method"
            }
          },
          {
            path: 'participation-type-consent/intro',
            name: 'ParticipationTypeConsentOverview',
            component: () => import('../views/participation/ParticipationTypeConsentOverview.vue'),
            meta: {
              currentSection: "participation",
              currentStep: "participation_selection_method"
            }
          },
          {
            path: 'participation-type-consent/title',
            name: 'ParticipationTypeConsentTitle',
            component: () => import('../views/participation/ParticipationTypeConsentTitle.vue'),
            meta: {
              currentSection: "participation",
              currentStep: "participation_selection_assignment_title"
            }
          },
          {
            path: 'manual-selection',
            name: 'ParticipationTypeManual',
            component: () => import('../views/participation/manual-participation/ParticipationTypeManual.vue'),
            meta: {
              currentSection: "participation",
              currentStep: "participation_selection_method"
            }
          },
        ]
      },
      {
        path: 'assignments',
        component: () => import('../views/ExperimentSteps.vue'),
        meta: {
          currentSection: "assignments"
        },
        children: [
          // Experiment | Assignments Steps paths
        ]
      },
      {
        path: 'summary',
        name: 'ExperimentSummary',
        component: () => import('../views/ExperimentSummary.vue')
      },
    ]
  }
]

const router = new VueRouter({
  base: process.env.BASE_URL,
  routes
})

export default router
