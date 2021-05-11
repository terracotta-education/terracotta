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
        children: [
          // Experiment | Design Steps paths
          {
            path: '',
            alias: 'intro',
            name: 'ExperimentDesignIntro',
            component: () => import('../views/design/Intro.vue')
          },
          {
            path: 'title',
            name: 'ExperimentDesignTitle',
            component: () => import('../views/design/Title.vue')
          },
        ]
      },
      {
        path: 'participation',
        component: () => import('../views/ExperimentSteps.vue'),
        children: [
          // Experiment | Participation Steps paths
        ]
      },
      {
        path: 'assignments',
        component: () => import('../views/ExperimentSteps.vue'),
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
