import { defineStore } from "pinia";

import { assignmentService } from "@/services";

// Shared by every action below that writes this.assignment (a singular "currently
// loaded assignment" field, not the assignments list) so a stale response from one
// can't clobber a newer one from another - see the identical guard in
// assessment.module.js's fetchAssessment for the full reasoning. Concretely: an
// instructor navigating from one assignment's grading/edit screen to another's fires
// a new fetchAssignment before the previous one necessarily resolves (both
// AssignmentScores.vue and AssignmentEditor.vue remount per :assignmentId route
// param), and whichever response lands last previously won even if it was the
// older, now-abandoned one.
let assignmentRequestId = 0;

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
      const requestId = ++assignmentRequestId;

      try {
        const response =
          await assignmentService.updateAssignment(...payload);

        if (requestId === assignmentRequestId) {
          this.assignment = response;
        }

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
      const requestId = ++assignmentRequestId;

      try {
        const response =
          await assignmentService.fetchAssignment(...payload);

        this.upsertAssignments([response]);

        if (requestId !== assignmentRequestId) {
          return this.assignment;
        }

        this.assignment = response;

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
            a => a.assignmentId !== assignmentId
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
      const requestId = ++assignmentRequestId;

      try {
        const response =
          await assignmentService.duplicateAssignment(...payload);

        if (response?.assignmentId) {
          if (requestId === assignmentRequestId) {
            this.assignment = response;
          }

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
      const requestId = ++assignmentRequestId;

      try {
        const response = await assignmentService.create(...payload);

        if (response?.assignmentId) {
          if (requestId === assignmentRequestId) {
            this.assignment = response;
          }

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
      const requestId = ++assignmentRequestId;

      try {
        const response =
          await assignmentService.moveAssignment(...payload);

        if (response && !response.error) {
          this.assignments = this.assignments.filter(
            a => a.assignmentId !== assignmentId
          );

          if (response?.assignmentId) {
            if (requestId === assignmentRequestId) {
              this.assignment = response;
            }

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
      // invalidate any still-in-flight fetch/save above so its eventual response can't
      // overwrite this deliberate, synchronous set once it resolves
      assignmentRequestId += 1;
      this.assignment = assignment;
    },

    setAssignment(assignment) {
      assignmentRequestId += 1;
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
          item => item.assignmentId === assignment.assignmentId
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
