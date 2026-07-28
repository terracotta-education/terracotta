import { defineStore } from "pinia";

import { conditionService } from "@/services";
import { exposures as useExposuresStore } from "@/store/exposures.module";
import { experiment as useExperimentStore } from "@/store/experiment.module";

const CONDITION_COLORS = [
  "#FFCCBC",
  "#FFECB3",
  "#F0F4C3",
  "#C8E6C9",
  "#B2EBF2",
  "#BBDEFB",
  "#D1C4E9",
  "#F8BBD0",
  "#D7CCC8",
  "#FFE0B2",
  "#FFF9C4",
  "#DCEDC8",
  "#B2DFDB",
  "#B3E5FC",
  "#C5CAE9",
  "#E1BEE7",
  "#E1BEE7",
  "#CFD8DC"
];

export const condition = defineStore("condition", {
  state: () => ({
    condition: null,
    conditions: []
  }),

  getters: {
    conditionColorMapping() {
      const exposuresStore = useExposuresStore();
      const groupConditionList =
        exposuresStore.exposures?.[0]?.groupConditionList || [];

      return groupConditionList.reduce(
        (conditionColorMap, groupCondition, index) => {
          conditionColorMap[groupCondition.conditionName] =
            CONDITION_COLORS[index % CONDITION_COLORS.length];

          return conditionColorMap;
        },
        {}
      );
    }
  },

  actions: {
    resetCondition() {
      this.condition = null;
    },

    async createDefaultConditions(experimentId) {
      return this.createConditions({
        conditions: [{}, {}],
        experimentId
      });
    },

    async createConditions(payload) {
      const conditions = payload?.conditions || [];

      if (!conditions.length) {
        return [];
      }

      return Promise.all(
        conditions.map(() => this.createCondition(payload.experimentId))
      );
    },

    async createCondition(experimentId) {
      try {
        const cond = await conditionService.create(experimentId);

        if (cond?.message) {
          return cond;
        }

        useExperimentStore().setCondition(cond);
        this.condition = cond;
        this.upsertCondition(cond);

        return cond;
      } catch (error) {
        console.error("condition/createCondition | catch", error);

        return null;
      }
    },

    async updateCondition(cond) {
      try {
        const response = await conditionService.update(cond);

        if (response?.status === 200) {
          useExperimentStore().setCondition(cond);
          this.condition = cond;
          this.upsertCondition(cond);
        }

        return response;
      } catch (error) {
        console.error("condition/updateCondition | catch", error);

        return null;
      }
    },

    async updateConditions(conditions) {
      try {
        const response = await conditionService.updateAll(conditions);

        if (response?.status === 200) {
          useExperimentStore().setConditions(conditions);
          this.conditions = Array.isArray(conditions) ? conditions : [];
        }

        return response;
      } catch (error) {
        console.error("condition/updateConditions | catch", error);

        return null;
      }
    },

    async setDefaultCondition(payload) {
      if (!payload?.conditions || !payload.defaultConditionId) {
        return false;
      }

      // one batched request for all conditions instead of one PUT per condition
      const updatedConditions = payload.conditions.map(cond => ({
        ...cond,
        defaultCondition:
          cond.conditionId === payload.defaultConditionId ? 1 : 0
      }));

      return this.updateConditions(updatedConditions);
    },

    async deleteCondition(cond) {
      try {
        const response = await conditionService.delete(cond);

        if (response?.status === 200) {
          useExperimentStore().deleteCondition(cond);
          this.conditions = this.conditions.filter(
            item =>
              parseInt(item.conditionId) !== parseInt(cond.conditionId)
          );

          if (
            parseInt(this.condition?.conditionId) ===
            parseInt(cond.conditionId)
          ) {
            this.condition = null;
          }
        }

        return response;
      } catch (error) {
        console.error("condition/deleteCondition | catch", error);

        return null;
      }
    },

    resetConditions() {
      this.condition = null;
      this.conditions = [];
    },

    upsertCondition(cond) {
      if (!cond) {
        return;
      }

      const index = this.conditions.findIndex(
        item =>
          parseInt(item.conditionId) === parseInt(cond.conditionId)
      );

      if (index >= 0) {
        this.conditions.splice(index, 1, cond);
      } else {
        this.conditions.push(cond);
      }
    }
  }
});
