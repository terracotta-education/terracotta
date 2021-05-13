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
