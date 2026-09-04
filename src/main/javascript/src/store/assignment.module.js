import { defineStore } from "pinia";

import { assignmentService } from "@/services";

export const assignment = defineStore("assignment", {
  state: () => ({
    assignments: [],
    assignment: null,
    fileRequest: null
  }),

  getters: {
    hasAssignments: state => state.assignments.length > 0
  },

  actions: {
    async updateAssignment(payload) {
      try {
        const response =
          await assignmentService.updateAssignment(...payload);

        this.assignment = response;
        this.upsertAssignments([response]);

        return {
          status: response?.status || 200,
          data: response
        };
      } catch (error) {
        console.error(
          "assignment/updateAssignment | catch",
          error
        );

        return null;
      }
    },

    async saveAssignmentOrder(payload) {
      try {
        const response =
          await assignmentService.updateAssignments(...payload);

        const assignments = Array.isArray(response) ? response : [];

        this.upsertAssignments(assignments);

        return assignments;
      } catch (error) {
        console.error(
          "assignment/saveAssignmentOrder | catch",
          error
        );

        return [];
      }
    },

    async fetchAssignment(payload) {
      try {
        const response =
          await assignmentService.fetchAssignment(...payload);

        this.assignment = response;
        this.upsertAssignments([response]);

        return response;
      } catch (error) {
        console.error(
          "assignment/fetchAssignment | catch",
          error
        );

        return null;
      }
    },

    async fetchAssignmentsByExposure(payload) {
      try {
        const assignments =
          await assignmentService.fetchAssignmentsByExposure(
            ...payload
          );

        this.upsertAssignments(
          Array.isArray(assignments) ? assignments : []
        );

        return assignments;
      } catch (error) {
        console.error(
          "assignment/fetchAssignmentsByExposure | catch",
          error
        );

        return [];
      }
    },

    async deleteAssignment(payload) {
      const assignmentId = payload[2];

      try {
        const response =
          await assignmentService.deleteAssignment(...payload);

        if (response?.status === 200) {
          this.assignments = this.assignments.filter(
            a => parseInt(a.assignmentId) !== parseInt(assignmentId)
          );

          return {
            status: response.status,
            data: null
          };
        }

        return response;
      } catch (error) {
        console.error(
          "assignment/deleteAssignment | catch",
          error
        );

        return null;
      }
    },

    async duplicateAssignment(payload) {
      try {
        const response =
          await assignmentService.duplicateAssignment(...payload);

        if (response?.assignmentId) {
          this.assignment = response;
          this.upsertAssignments([response]);

          return {
            status: 201,
            data: response
          };
        }

        return response;
      } catch (error) {
        console.error(
          "assignment/duplicateAssignment | catch",
          error
        );

        return null;
      }
    },

    async createAssignment(payload) {
      try {
        const response = await assignmentService.create(...payload);

        if (response?.assignmentId) {
          this.assignment = response;
          this.upsertAssignments([response]);

          return {
            status: 201,
            data: response
          };
        }

        return response;
      } catch (error) {
        console.error(
          "assignment/createAssignment | catch",
          error
        );

        return null;
      }
    },

    async moveAssignment(payload) {
      const assignmentId = payload[2];

      try {
        const response =
          await assignmentService.moveAssignment(...payload);

        if (response && !response.error) {
          this.assignments = this.assignments.filter(
            a => parseInt(a.assignmentId) !== parseInt(assignmentId)
          );

          if (response?.assignmentId) {
            this.assignment = response;
            this.upsertAssignments([response]);
          }

          return {
            status: 201,
            data: response
          };
        }

        return response;
      } catch (error) {
        console.error(
          "assignment/moveAssignment | catch",
          error
        );

        return null;
      }
    },

    setCurrentAssignment(assignment) {
      this.assignment = assignment;
    },

    setAssignment(assignment) {
      this.assignment = assignment;
    },

    resetAssignments() {
      this.assignments = [];
    },

    resetAssignment() {
      this.assignment = null;
    },

    reset() {
      this.assignments = [];
      this.assignment = null;
      this.fileRequest = null;
    },

    upsertAssignments(assignments) {
      if (!Array.isArray(assignments)) {
        return;
      }

      assignments.filter(Boolean).forEach(assignment => {
        const index = this.assignments.findIndex(
          item =>
            parseInt(item.assignmentId) ===
            parseInt(assignment.assignmentId)
        );

        if (index >= 0) {
          this.assignments.splice(index, 1, assignment);
        } else {
          this.assignments.push(assignment);
        }
      });
    }
  }
});
