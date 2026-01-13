<template>
  <v-container
    v-show="pageLoaded && !loading"
    :class="{'preview': preview}"
    fluid
  >
    <v-row
      v-if="!preview"
    >
      <v-col
        v-if="canTryAgain"
      >
        <v-btn
          @click="handleTryAgain"
          elevation="0"
          color="primary"
          class="mt-4 mb-2"
          type="button"
        >
          Try Again
        </v-btn>
        <p>
          <span v-if="assignmentData.multipleSubmissionScoringScheme === 'HIGHEST'">The highest</span>
          <span v-else-if="assignmentData.multipleSubmissionScoringScheme === 'MOST_RECENT'">The most recent</span>
          <span v-else-if="assignmentData.multipleSubmissionScoringScheme === 'AVERAGE'">The average</span>
          <span v-else-if="assignmentData.multipleSubmissionScoringScheme === 'CUMULATIVE'">A cumulative</span>
          score will be kept
        </p>
      </v-col>
      <v-spacer />
      <v-col
        v-if="showSubmissionDetails"
      >
        <h2>Submission Details</h2>
        <v-divider />
          <v-list dense flat>
            <v-list-item>
              <v-list-item-content>
                <strong>Time</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ timeBeforeSubmission }}</span>
              </v-list-item-icon>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <strong>Allowed Attempts</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ allowedAttempts }}</span>
              </v-list-item-icon>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <strong>Submitted</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ selectedSubmissionDateSubmitted }}</span>
              </v-list-item-icon>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <strong>Current Score</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ currentScore }}</span>
              </v-list-item-icon>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <strong>Kept Score</strong>
              </v-list-item-content>
              <v-list-item-icon>
                <span>{{ keptScore }}</span>
              </v-list-item-icon>
            </v-list-item>
          </v-list>
        <v-divider />
      </v-col>
    </v-row>
    <v-row
      v-if="!preview && cantTryAgainMessage"
    >
      <v-col>
        <v-card
          class="pt-5 px-5 mx-auto yellow lighten-5 rounded-lg"
          outlined
        >
          <p
            v-if="cantTryAgainMessage === 'MAX_NUMBER_ATTEMPTS_REACHED'"
            class="pb-0"
          >
            You have reached the maximum number of attempts for this assignment.
          </p>
          <p
            v-if="cantTryAgainMessage === 'WAIT_TIME_NOT_REACHED'"
            class="pb-0"
          >
            Wait time not reached... You must wait a period of time before submitting again.
          </p>
        </v-card>
      </v-col>
    </v-row>
    <v-row v-if="!preview && readonly">
      <v-col>
        <v-card
          v-if="muted"
          class="pt-5 px-5 mx-auto yellow lighten-5 rounded-lg"
          outlined
        >
          <h3>Your assignment is muted</h3>
          <p class="pb-0">
            Your instructor has not released the grades yet.
          </p>
        </v-card>
        <div
          v-if="!muted && assignmentData && assignmentData.submissions"
        >
          <submission-selector
            :submissions="assignmentData.submissions"
            @select="(id) => selectedSubmissionId = id" />
        </div>
      </v-col>
    </v-row>
    <v-row
      v-if="isIntegration"
      class="integration mt-0"
    >
      <v-col
        v-if="!submitted"
        class="py-0"
      >
        <div
          v-if="assessment.html"
          v-html="assessment.html"
        ></div>
        <iframe
          v-if="!readonly"
          :src="integrationLaunchUrl"
          :class="{'no-resize': !hasResizeMessage}"
          id="integration-iframe"
          title="student assignment"
          aria-label="student assignment"
        >
        </iframe>
        <external-integration-response-editor
          v-if="readonly"
          :submission="selectedSubmission"
        />
      </v-col>
      <v-col
        v-if="submitted"
      >
        <v-alert
          type="success"
        >
          Your answers have been submitted.
        </v-alert>
      </v-col>
    </v-row>
    <v-row
      v-if="!isIntegration && assessment && questionValues.length > 0"
      :class="{'preview-treatment': preview}"
    >
      <v-col>
        <template
          v-if="!submitted"
        >
          <!-- only display assessment instructions on the first page -->
          <div
            v-if="assessment.html && questionPageIndex === 0"
            v-html="assessment.html"
          ></div>
          <form
            v-if="!isIntegration"
            v-on:submit.prevent="handleSubmit"
            style="width: 100%;"
            ref="form"
          >
            <div
              class="answerSection mt-5 w-100"
            >
              <v-card
                v-for="(question, index) in currentQuestionPage.questions"
                :key="question.questionId"
                class="mt-5 mb-2"
                outlined
              >
                <v-card-title>
                  <v-row>
                    <v-col
                      cols="1"
                    >
                      <span>
                        {{ currentQuestionPage.questionStartIndex + index + 1 }}
                      </span>
                    </v-col>
                    <v-col
                      cols="8"
                    >
                      <youtube-event-capture
                        :experimentId="experimentId"
                        :assessmentId="assessmentId"
                        :conditionId="conditionId"
                        :questionId="question.questionId"
                        :submissionId="submissionId"
                        :treatmentId="treatmentId"
                      >
                        <span
                          v-html="question.html"
                        >
                        </span>
                      </youtube-event-capture>
                    </v-col>
                    <v-col>
                      <div
                        v-if="!readonly"
                        class="total-points text-right ml-2"
                      >
                        {{ question.points }} Point{{ question.points > 1 ? "s" : "" }}
                      </div>
                      <div class="total-points text-right ml-2" v-if="readonly">
                        {{ getQuestionSubmissionValue(question) }}
                        /
                        {{ question.points }} Point{{ question.points > 1 ? "s" : "" }}
                      </div>
                    </v-col>
                  </v-row>
                </v-card-title>
                <!-- Options (Answers) -->
                <v-card-text
                  v-if="questionValues && questionValues.length > 0"
                >
                  <template
                    v-if="question.questionType === 'MC'"
                  >
                    <multiple-choice-response-editor
                      :answers="getQuestionAnswers(question)"
                      :readonly="readonly"
                      :showAnswers="showAnswers"
                      v-model="questionValues.find(
                          ({ questionId }) => questionId === question.questionId
                        ).answerId"
                    />
                  </template>
                  <template
                    v-else-if="question.questionType === 'ESSAY'"
                  >
                    <essay-response-editor
                      :answer="getEssayResponse(question)"
                      :readonly="readonly"
                      v-model="
                        questionValues.find(
                          ({ questionId }) => questionId === question.questionId
                        ).response
                      "
                    />
                  </template>
                  <template
                    v-else-if="question.questionType === 'FILE'"
                  >
                    <file-upload-response-editor
                      :selectedSubmission="selectedSubmission"
                      :fileResponses="getFileResponses(question)"
                      :selectedDownloadId="selectedDownloadId"
                      :readonly="readonly"
                      :submissionId="submissionId"
                      :questionId="question.questionId"
                      @download-file-response="downloadFileResponse"
                      v-model="
                        questionValues.find(
                          ({ questionId }) => questionId === question.questionId
                        ).response
                      "
                    />
                  </template>
                </v-card-text>
              </v-card>
            </div>

            <v-btn
              v-if="showBackButton"
              :disabled="disableBackButton"
              @click.prevent="backPage"
              elevation="0"
              color="primary"
              class="mt-4 mr-2"
              type="button"
            >
              Back
            </v-btn>
            <v-btn
              v-if="showNextButton"
              @click.prevent="nextPage"
              :disabled="disableNextButton"
              elevation="0"
              color="primary"
              class="mt-4"
              type="button"
            >
              Next
            </v-btn>
            <v-btn
              v-if="!preview && showSubmitButton"
              :disabled="disableSubmitButton"
              elevation="0"
              color="primary"
              class="mt-4"
              type="submit"
            >
              Submit
            </v-btn>
            <v-btn
              v-if="preview && showSubmitButton"
              :disabled="disableSubmitButton"
              :href="`/preview/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/complete?ownerId=${ownerId}`"
              elevation="0"
              color="primary"
              class="mt-4"
            >
              Submit
            </v-btn>
          </form>
        </template>
        <template
          v-if="submitted"
        >
          <v-alert
            type="success"
          >
            Your answers have been submitted.
          </v-alert>
        </template>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { mapActions, mapGetters, mapMutations } from "vuex";
import EssayResponseEditor from "./EssayResponseEditor.vue";
import FileUploadResponseEditor from "@/views/student/FileUploadResponseEditor";
import ExternalIntegrationResponseEditor from "@/views/integrations/ExternalIntegrationResponseEditor";
import moment from 'moment';
import MultipleChoiceResponseEditor from "./MultipleChoiceResponseEditor.vue";
import SubmissionSelector from '../assignment/SubmissionSelector.vue';
import Vue from 'vue';
import YoutubeEventCapture from "./YoutubeEventCapture.vue";

Vue.filter('formatDate', (value) => {
  if (value) {
    return moment(value).format('MM/DD/YYYY hh:mm')
  }
});

export default {
  name: "StudentQuiz",
  props: [
    "experimentId",
    "previewConditionId",
    "previewTreatmentId",
    "ownerId",
    "previewId",
    "preview"
  ],
  components: {
    EssayResponseEditor,
    FileUploadResponseEditor,
    ExternalIntegrationResponseEditor,
    MultipleChoiceResponseEditor,
    SubmissionSelector,
    YoutubeEventCapture
  },
  data() {
    return {
      maxPoints: 0,
      questionValues: [],
      conditionId: null,
      treatmentId: null,
      submissionId: null,
      assessmentId: null,
      submitted: false,
      questionPageIndex: 0,
      assignmentData: null,
      selectedSubmissionId: null,
      readonly: false,
      answers: [],
      loading: false,
      pageLoaded: false,
      submissions: [],
      answerSubmissionId: null,
      downloadId: null,
      integrationLaunchUrl: null,
      treatment: null,
      hasResizeMessage: false
    };
  },
  watch: {
    selectedSubmissionId() {
      if (this.selectedSubmission) {
        const { experimentId, conditionId, assessmentId, treatmentId, submissionId } = this.selectedSubmission;
        this.getQuestions(experimentId, conditionId, assessmentId, treatmentId, submissionId);
        this.getAnswers(experimentId, conditionId, assessmentId, treatmentId, submissionId);
      }
    },
    answerableQuestions(newValue) {
      if (newValue.questionType === this.questionTypes.integration) {
        // integration type question; skip adding to answerable
        return;
      }

      this.questionValues = newValue.map((q) => {
        return {
          questionId: q.questionId,
          answerId: null,
          response: null,
        };
      });
    }
  },
  computed: {
    ...mapGetters({
      assessment: "assessment/assessment",
      answerableQuestions: "assessment/answerableQuestions",
      questionPages: "assessment/questionPages",
      questionSubmissions: "submissions/questionSubmissions"
    }),
    allCurrentPageQuestionsAnswered() {
      return this.areAllQuestionsAnswered(this.currentQuestionPage.questions);
    },
    allQuestionsAnswered() {
      return this.areAllQuestionsAnswered(this.answerableQuestions);
    },
    currentQuestionPage() {
      return this.questionPages[this.questionPageIndex];
    },
    hasNextQuestionPage() {
      return this.questionPageIndex < this.questionPages.length - 1;
    },
    showNextButton() {
      if (this.readonly) {
        return this.questionPages.length > 1;
      }

      return this.hasNextQuestionPage;
    },
    disableNextButton() {
      if (this.readonly) {
        return !this.hasNextQuestionPage;
      }

      return !this.allCurrentPageQuestionsAnswered;
    },
    showBackButton() {
      return (this.preview || this.readonly) && this.questionPages.length > 1;
    },
    disableBackButton() {
      return !this.hasBackQuestionPage;
    },
    hasBackQuestionPage() {
      if (!this.preview && !this.readonly) {
        // only readonly mode can go back
        return false;
      }

      // all pages besides first has a back button
      return this.questionPageIndex > 0;
    },
    showSubmitButton() {
      return !this.readonly && !this.hasNextQuestionPage;
    },
    disableSubmitButton() {
      return !this.allQuestionsAnswered
    },
    canTryAgain() {
      return this.readonly && (this.assignmentData ? this.assignmentData.retakeDetails.retakeAllowed : false);
    },
    showSubmissionDetails() {
      return this.readonly || this.submitted;
    },
    allowedAttempts() {
      if (!this.assignmentData) {
        return ' - '
      }
      const { numOfSubmissions } = this.assignmentData;
      return numOfSubmissions === null ? 1 : numOfSubmissions === 0 ? 'Unlimited' : numOfSubmissions;
    },
    cantTryAgainMessage() {
      return this.assignmentData?.retakeDetails?.retakeNotAllowedReason;
    },
    selectedSubmissionDateSubmitted() {
      return moment(this.selectedSubmission?.dateSubmitted).format('MMMM Do YYYY hh:mm');
    },
    timeBeforeSubmission() {
      const time = this.selectedSubmission?.dateSubmitted - this.selectedSubmission?.dateCreated;
      return isNaN(time) ? '' : moment.duration(time, "milliseconds").humanize();
    },
    currentScore() {
      let grade;
      if (!this.selectedSubmission) {
        grade = this.assignmentData?.retakeDetails.lastAttemptScore;
      } else {
        const { totalAlteredGrade, alteredCalculatedGrade } = this.selectedSubmission;
        grade = totalAlteredGrade !== null ? totalAlteredGrade : alteredCalculatedGrade;
      }
      return `${this.round(grade)} / ${this.assignmentData?.maxPoints}`;
    },
    keptScore() {
      const kept = this.assignmentData?.retakeDetails.keptScore;

      return `${kept ? this.round(kept) : 0} / ${this.assignmentData?.maxPoints}`;
    },
    muted() {
      if (!this.assignmentData) { return true; }
      const { allowStudentViewResponses, studentViewResponsesAfter, studentViewResponsesBefore } = this.assignmentData;
      if (allowStudentViewResponses) {
        const now = Date.now();
        const isAfter = studentViewResponsesAfter ? moment(now).isAfter(studentViewResponsesAfter) : true;
        const isBefore = studentViewResponsesBefore ? moment(now).isBefore(studentViewResponsesBefore) : true;
        return isAfter && isBefore ? false : true;
      }
      return true;
    },
    showAnswers() {
      if (!this.assignmentData) { return false; }
      const { allowStudentViewCorrectAnswers, studentViewCorrectAnswersAfter, studentViewCorrectAnswersBefore } = this.assignmentData;
      if (allowStudentViewCorrectAnswers) {
        const now = Date.now();
        const isAfter = studentViewCorrectAnswersAfter ? moment(now).isAfter(studentViewCorrectAnswersAfter) : true;
        const isBefore = studentViewCorrectAnswersBefore ? moment(now).isBefore(studentViewCorrectAnswersBefore) : true;
        return isAfter && isBefore;
      }
      return false;
    },
    showResponses() {
      return this.assignmentData?.allowStudentViewCorrectAnswers;
    },
    selectedSubmission() {
      return this.assignmentData?.submissions.find(s => s.submissionId === this.selectedSubmissionId);
    },
    selectedSubmissionConditionId() {
      return this.selectedSubmission?.conditionId;
    },
    selectedDownloadId() {
      return this.downloadId;
    },
    isIntegration() {
      return this.assessment.integration;
    },
    questionTypes() {
      return {
        essay: "ESSAY",
        file: "FILE",
        integration: "INTEGRATION",
        mc: "MC"
      }
    }
  },
  methods: {
    ...mapActions({
      reportStep: "api/reportStep",
      fetchAssessmentForSubmission: "assessment/fetchAssessmentForSubmission",
      fetchQuestionSubmissions: "submissions/fetchQuestionSubmissions",
      createQuestionSubmissions: "submissions/createQuestionSubmissions",
      createAnswerSubmissions: "submissions/createAnswerSubmissions",
      updateAnswerSubmission: "submissions/updateAnswerSubmission",
      downloadAnswerFileSubmission: "submissions/downloadAnswerFileSubmission",
      previewTreatment: "preview/treatment"
    }),
    ...mapMutations({
      setAssessment: "assessment/setAssessment",
      clearFiles: "submissions/clearFiles"
    }),
    async handleTryAgainIntegration() {
      this.attempt(true);
    },
    async handleTryAgain() {
      this.attempt();
    },
    async handleSubmit() {
      await this.$swal({
        target: "#app",
        icon: "question",
        text: "Are you ready to submit your answers?",
        showCancelButton: true,
        confirmButtonText: "Yes, submit",
        cancelButtonText: "No, cancel",
        showLoaderOnConfirm: true,
        preConfirm: async () => {
          try {
            this.$swal.update({
              text: "Please don't refresh or close your browser window until assignment submission is confirmed.",
              showConfirmButton: false,
            });
            return await this.submitQuiz();
          } catch (error) {
            this.$swal({
              // add popup to #app so we can use vuetify styling
              target: "#app",
              text: "Could not submit: " + error.message,
              icon: "error",
              footer: this.errorFooter(),
            });
          }
        },
        allowOutsideClick: () => !this.$swal.isLoading(),
      });
    },
    async submitQuiz() {
      try {
        this.selectedSubmissionId = this.submissionId;
        const experimentId = this.experimentId;
        const step = "student_submission";
        const parameters = { submissionIds: this.submissionId };
        // Reload question submissions so we know whether to create/update question/answer submissions
        if (!this.submissions) {
          await this.fetchQuestionSubmissions([
            experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
            this.submissionId,
          ]);
          this.submissions = this.questionSubmissions;
        }
        await this.saveAnswers();

        // submit step
        const { data, status } = await this.reportStep({
          experimentId,
          step,
          parameters,
        });
        if (!status || ![200, 201].includes(status)) {
          throw Error("Error submitting quiz: " + data);
        }

        const view = await this.viewAssignment();

        if (view?.status === 200) {
          const { data } = view;
          this.assignmentData = data;
          this.submitted = true;
        }
      } catch (e) {
        // Clear question submissions for this attempt. Will need to reload
        // these to figure out what QuestionSubmissions and/or AnswerSubmissions
        // need to be updated
        this.submissions = null;
        console.error({ e });
        throw e; // rethrow
      }
    },
    async saveAnswers() {
      const allQuestionSubmissions = this.questionValues.map((q) => {
        const existingQuestionSubmission = this.submissions.find(
          (qs) => qs.questionId === q.questionId
        );
        const questionSubmissionId = existingQuestionSubmission?.questionSubmissionId;
        // find existing answer submission id if it exists
        const answerSubmissionId = existingQuestionSubmission?.answerSubmissionDtoList?.[0]?.answerSubmissionId;
        const questionSubmission = {
          questionSubmissionId,
          questionId: q.questionId,
          answerSubmissionDtoList: [
            {
              answerSubmissionId,
              questionSubmissionId,
              answerId: q.answerId,
              response: q.response,
            },
          ],
        };
        return questionSubmission;
      });

      // separate question submissions into existing and new by whether they have id
      const existingQuestionSubmissions = allQuestionSubmissions.filter(
        (qs) => !!qs.questionSubmissionId
      );
      const newQuestionSubmissions = allQuestionSubmissions.filter(
        (qs) => !qs.questionSubmissionId
      );

      // call createAnswerSubmission for all existing question submissions
      const answerSubmissions = existingQuestionSubmissions.map(
        (qs) => qs.answerSubmissionDtoList[0]
      );
      if (answerSubmissions.length > 0) {
        // separate into existing and new answer submissions and call the appropriate end point
        const existingAnswerSubmissions = answerSubmissions.filter(
          (ans) => !!ans.answerSubmissionId
        );
        const newAnswerSubmissions = answerSubmissions.filter(
          (ans) => !ans.answerSubmissionId
        );
        if (newAnswerSubmissions.length > 0) {
          const { data, status } = await this.createAnswerSubmissions([
            this.experimentId,
            this.conditionId,
            this.treatmentId,
            this.assessmentId,
            this.submissionId,
            newAnswerSubmissions,
          ]);
          if (!status || ![200, 201].includes(status)) {
            throw Error("Error submitting quiz: " + data);
          }
        }
        if (existingAnswerSubmissions.length > 0) {
          for (const answerSubmission of existingAnswerSubmissions) {
            const { data, status } = await this.updateAnswerSubmission([
              this.experimentId,
              this.conditionId,
              this.treatmentId,
              this.assessmentId,
              this.submissionId,
              answerSubmission.questionSubmissionId,
              answerSubmission.answerSubmissionId,
              answerSubmission,
            ]);
            if (!status || ![200, 201].includes(status)) {
              throw Error("Error submitting quiz: " + data);
            }
          }
        }
      }

      // call createQuestionSubmissions for all new question submissions
      if (newQuestionSubmissions.length > 0) {
        const { data, status } = await this.createQuestionSubmissions([
          this.experimentId,
          this.conditionId,
          this.treatmentId,
          this.assessmentId,
          this.submissionId,
          newQuestionSubmissions,
        ]);
        if (!status || ![200, 201].includes(status)) {
          throw Error("Error submitting quiz: " + data);
        }
      }
    },
    async getAnswers(experimentId, conditionId, assessmentId, treatmentId, submissionId) {
      await this.fetchQuestionSubmissions([
        experimentId,
        conditionId,
        treatmentId,
        assessmentId,
        submissionId,
      ]);
    },
    async downloadFileResponse({ conditionId, treatmentId, assessmentId, submissionId, questionSubmissionId, answerSubmissionId, mimeType, fileName }) {
      this.downloadId = answerSubmissionId;

      try {
        await this.downloadAnswerFileSubmission([
          this.experimentId,
          conditionId,
          treatmentId,
          assessmentId,
          submissionId,
          questionSubmissionId,
          answerSubmissionId,
          mimeType,
          fileName
        ]);

        this.downloadId = null;
      } catch (error) {
          console.log("downloadFileResponse | catch", error);
          this.downloadId = null;
      }
    },
    async getQuestions(experimentId, conditionId, assessmentId, treatmentId, submissionId) {
      this.questionValues = [];

      await this.fetchAssessmentForSubmission([
          experimentId,
          conditionId,
          treatmentId,
          assessmentId,
          submissionId,
      ]);
    },
    getQuestionSubmissionValue(question) {
      const value = this.questionSubmissions?.find(({ questionId }) => questionId === question.questionId);
      const score = value?.alteredGrade != null ? value?.alteredGrade : value?.calculatedPoints;
      return score != null ? score : 0;
    },
    getQuestionAnswers(question) {
      if (!this.readonly) {
        return question.answers;
      }

      const questionSubmissionDto = this.questionSubmissions?.find(s => s.questionId === question.questionId);

      if (!questionSubmissionDto) {
        return [];
      }

      const answers = questionSubmissionDto.answerDtoList;
      const responses = questionSubmissionDto.answerSubmissionDtoList;

      return answers.map(a => {
        const resp = responses.find(r => r.answerId === a.answerId);
        return {
          ...a,
          studentResponse: resp ? resp.answerId : false
        };
      });
    },
    getEssayResponse(question) {
      if (!this.readonly) {
        return null;
      }

      const questionSubmissionDto = this.questionSubmissions?.find(s => s.questionId === question.questionId);

      if (!questionSubmissionDto) {
        return null;
      }

      return questionSubmissionDto.answerSubmissionDtoList.find(a => a.questionSubmissionId === questionSubmissionDto.questionSubmissionId);
    },
    getFileResponses(question) {
      if (!this.readonly) {
        return null;
      }

      const questionSubmissionDto = this.questionSubmissions?.find(s => s.questionId === question.questionId);

      if (!questionSubmissionDto) {
        return null;
      }

      return questionSubmissionDto.answerSubmissionDtoList;
    },
    errorFooter() {
      return `<div class="text--secondary body-2">
                <div>Timestamp: ${new Date().toString()}</div>
                <div>Experiment: ${this.experimentId}</div>
              </div>`;
    },
    areAllQuestionsAnswered(answerableQuestions) {
      if (this.readonly) {
        return true;
      }

      for (const question of answerableQuestions) {
        if (question.questionType === "MC") {
          const answer = this.questionValues.find(
            ({ questionId }) => questionId === question.questionId
          ).answerId;
          if (answer === null) {
            return false;
          }
        } else if (question.questionType === "ESSAY") {
          const answer = this.questionValues.find(
            ({ questionId }) => questionId === question.questionId
          ).response;
          if (answer === null || answer.trim() === "") {
            return false;
          }
        } else if (question.questionType === "FILE") {
          const answer = this.questionValues.find(
              ({ questionId }) => questionId === question.questionId
          ).response;
          if (answer === null) {
            return false;
          }
        } else {
          console.log(
            "Unexpected question type",
            question.questionType,
            question
          );
        }
      }
      return true;
    },
    nextPage() {
      this.questionPageIndex++;
      this.$nextTick(() => {
        this.$refs.form.scrollIntoView({ behavior: "smooth" });
      });
    },
    backPage() {
      this.questionPageIndex--;
      this.$nextTick(() => {
        this.$refs.form.scrollIntoView({ behavior: "smooth" });
      });
    },
    async viewAssignment() {
      const experimentId = this.experimentId;
      const step = "view_assignment";
      return this.reportStep({ experimentId, step });
    },
    async attempt(preferLmsChecks = false) {
      this.questionPageIndex = 0;
      const experimentId = this.experimentId;
      const step = "launch_assignment";
      this.readonly = false;
      this.loading = true;
      try {
        const stepResponse = await this.reportStep({ experimentId, step, undefined, preferLmsChecks });

        if (stepResponse?.status === 200) {
          const data = stepResponse?.data;
          this.conditionId = data.conditionId;
          this.treatmentId = data.treatmentId;
          this.assessmentId = data.assessmentId;
          this.submissionId = data.submissionId;
          this.integrationLaunchUrl = data.integrationLaunchUrl;

          const { experimentId, conditionId, assessmentId, treatmentId, submissionId, questionSubmissionDtoList } = data;

          this.submissions = questionSubmissionDtoList;

          this.getQuestions(experimentId, conditionId, assessmentId, treatmentId, submissionId);

        } else if(stepResponse?.status == 401) {
          if (stepResponse?.data.toString().includes("Error 150:")) {
            this.$swal({
              target: "#app",
              text: "You have no more attempts available",
              icon: "error",
              footer: this.errorFooter(),
            });
          }
        }
        this.loading = false;
      } catch (e) {
        console.error({ e });
      }
    },
    round(n) {
      return n % 1 ? n.toFixed(2) : n;
    },
    async handleIntegrationsResize(event) {
      if (event.data && event.data.height) {
        this.hasResizeMessage = true;
        const iframe = document.getElementById("integration-iframe");

        if (!iframe || iframe.height === event.data.height) {
          // skip resize if no iframe or height is the same as previous
          console.log("iframe not found or height unchanged, skipping resize");
          return;
        }

        const heightPadded = event.data.height;
        iframe.height = `${heightPadded}px`;

        // postMessage to LMS to resize iframe there as well
        window.parent.postMessage(
          {
            subject: "lti.frameResize",
            height: heightPadded
          },
          "*"
        )
      }
    },
    async handleIntegrationsScore() {
      const view = await this.viewAssignment();

      if (view?.status === 200) {
        const { data } = view;
        this.assignmentData = data;
        this.submitted = true;
        // auto-select the last submission
        this.selectedSubmissionId = this.assignmentData.submissions[this.assignmentData.submissions.length - 1];
      }
    }
  },
  async created() {
    this.clearFiles();
    if (this.preview) {
      const treatmentPreview = await this.previewTreatment([
        this.experimentId,
        this.previewConditionId,
        this.previewTreatmentId,
        this.previewId,
        this.ownerId
      ]);

      this.treatment = treatmentPreview.treatment;
      this.setAssessment(this.treatment.assessmentDto);
      this.assessmentId = this.treatment.assessmentDto.assessmentId;
      this.treatmentId = this.treatment.treatmentId;
      this.conditionId = this.treatment.conditionId;
      this.submissions = [treatmentPreview.submission];
      this.submissionId = treatmentPreview.submission.submissionId;
      this.integrationLaunchUrl = null;
      this.readonly = false;
      this.pageLoaded = true;
      this.$emit("loaded");
      return;
    }

    this.loading = true;

    try {
      const stepResponse = await this.viewAssignment();

      if (stepResponse?.status === 200) {
        const { data } = stepResponse;
        this.assignmentData = data;

        const { retakeDetails } = data;
        const { retakeAllowed, submissionAttemptsCount } = retakeDetails;
        if (retakeAllowed && submissionAttemptsCount === 0) {
          this.attempt();
        } else {
          this.readonly = true;
        }

      } else if (stepResponse?.status == 401) {
         if (stepResponse?.data.toString().includes("Error 150:")) {
           this.$swal({
             target: "#app",
             text: "You have no more attempts available",
             icon: "error",
             footer: this.errorFooter(),
           });
         }
      }
    } catch (e) {
      console.error({ e });
    }
    this.loading = false;
    this.pageLoaded = true;
    this.$emit('loaded');
  },
  mounted() {
    // handle integration iframe resizing
    window.addEventListener(
      "message",
      (event) => {
        if (!event || !event.origin) {
          // no event or origin, ignore
          return;
        }

        const messageOrigin = new URL(event.origin).hostname;
        const expectedOrigin = new URL(this.integrationLaunchUrl).hostname;

        if (messageOrigin !== expectedOrigin) {
          // origin does not match integration launch url, ignore
          console.log(`ignoring message from origin ${messageOrigin}, expected from ${expectedOrigin}`);
          return;
        }

        if (!event.data || !event.data.subject) {
          // not an expected postMessage event
          console.log(`ignoring message from origin ${messageOrigin}, unexpected subject ${event.data.subject || 'null'}`);
          return;
        }

        switch (event.data.subject) {
          case "terracotta_iframe_resize":
            this.handleIntegrationsResize(event);
            break;
          default:
            break;
        }
      },
      false
    );
    // handle integration iframe score return event
    window.document.addEventListener("integrations_score", this.handleIntegrationsScore);
    // handle integration iframe reattempt event
    window.document.addEventListener("integrations_reattempt", this.handleTryAgainIntegration);
  },
  beforeDestroy () {
    window.removeEventListener("message", this.handleIntegrationsResize);
    window.removeEventListener("integrations_score", this.handleIntegrationsScore);
    window.removeEventListener("integrations_reattempt", this.handleTryAgainIntegration);
  }
};
</script>

<style lang="scss" scoped>
.v-application .v-sheet--outlined.yellow.lighten-5 {
  border-color: #FFE0B2 !important;
}
.total-points {
  line-height: 24px;
  font-size: 16px;
  font-weight: 400;
}
.individualScore {
  margin-left: 1px;
}
.cardDetails {
  min-width: 100%;
}
.integration {
  min-height: 100%;
  min-width: 100%;
  & > .col {
    min-height: 100%;
    min-width: 100%;
    & > iframe {
      min-width: 100%;
      border: none;
    }
  }
  & .no-resize {
    height: 100%;
  }
}
.preview {
  background-color: rgba(253, 245, 242, 1) !important;
  & .preview-treatment {
    margin: 0 auto;
    max-width: 75%;
  }
}
</style>
