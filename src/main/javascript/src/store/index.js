import Vue from "vue";
import Vuex from "vuex";
import createPersistedState from "vuex-persistedstate";

import { api } from "./api.module";
import { alert } from "./alert.module";
import { assignment } from "./assignment.module";
import { assessment } from "./assessment.module";
import { experiment } from "./experiment.module";
import { condition } from "./condition.module";
import { consent } from "./consent.module";
import { exposures } from "./exposures.module";
import { participants } from "./participants.module";
import { outcome } from "./outcome.module";
import { treatment } from "./treatment.module";
import { dataexportrequest } from "./experimentdataexport.module";
import { submissions } from "./submission.module";
import { mediaevents } from "./mediaevents.module";
import { navigation } from "./navigation.module";
import { groups } from "./groups.module";
import { resultsDashboard } from "./dashboard/results.module";
import { preview } from "./preview/preview.module";
import { assignmentfilearchive } from "./assignmentfilearchive.module";
import { configuration } from "./configuration.module";

Vue.use(Vuex)

const store = new Vuex.Store({
    plugins: [
      createPersistedState({
        key: "terracotta"
      }),
    ],
    modules: {
        api,
        alert,
        assignment,
        assessment,
        experiment,
        condition,
        consent,
        exposures,
        participants,
        outcome,
        treatment,
        dataexportrequest,
        submissions,
        mediaevents,
        navigation,
        groups,
        resultsDashboard,
        preview,
        assignmentfilearchive,
        configuration
    },
    strict: process.env.NODE_ENV !== "production",
})

export default store
