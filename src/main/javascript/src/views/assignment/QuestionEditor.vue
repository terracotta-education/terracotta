<template>
  <div>
    <tiptap-vuetify
      v-model="html"
      placeholder="Question"
      class="mb-6 outlined"
      :extensions="extensions"
      :native-extensions="nativeExtensions"
      :card-props="{ flat: true }"
      :rules="rules"
      required
    />
    <v-text-field
      v-model="points"
      :rules="numberRule"
      label="Points"
      type="number"
      step="any"
      outlined
      required
    ></v-text-field>

    <!-- default slot for answer options or other custom content -->
    <slot></slot>

    <v-row>
      <v-col>
        <slot name="actions"> </slot>
      </v-col>
      <v-col class="text-right">
        <v-menu>
          <template v-slot:activator="{ on, attrs }">
            <v-icon color="black" v-bind="attrs" v-on="on">
              mdi-dots-horizontal
            </v-icon>
          </template>
          <v-list class="text-left">
            <v-list-item
              v-if="isPageBreakAfter"
              @click="removePageBreakAfter(question)"
            >
              <v-list-item-title
                >Remove page break after question</v-list-item-title
              >
            </v-list-item>
            <v-list-item v-else @click="addPageBreakAfter(question)">
              <v-list-item-title
                >Add page break after question</v-list-item-title
              >
            </v-list-item>
            <v-list-item @click="handleDeleteQuestion(question)">
              <v-list-item-title>Delete Question</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </v-col>
    </v-row>
  </div>
</template>

<script>
import {
  TiptapVuetify,
  Heading,
  Bold,
  Italic,
  Strike,
  Underline,
  Code,
  Paragraph,
  BulletList,
  OrderedList,
  ListItem,
  Link,
  Blockquote,
  HardBreak,
  HorizontalRule,
  History,
} from "tiptap-vuetify";
import { clone } from "@/helpers";
import YoutubeEmbed from "./tiptap/YoutubeEmbed";
import YoutubeEmbedExtension from "./tiptap/YoutubeEmbedExtension";
import { mapGetters, mapMutations } from "vuex";

/*
 * Events:
 * - delete: user has confirmed deletion of a question
 *   - args: question
 */
export default {
  props: ["question"],
  data() {
    return {
      rules: [
        (v) => (v && !!v.trim()) || "required",
        (v) =>
          (v || "").length <= 255 || "A maximum of 255 characters is allowed",
      ],
      numberRule: [
        (v) => (v && !isNaN(v)) || "required",
        (v) =>
          (!isNaN(parseFloat(v)) && v >= 0) ||
          "The point value cannot be negative",
      ],
      extensions: [
        History,
        Blockquote,
        Link,
        Underline,
        Strike,
        Italic,
        ListItem,
        BulletList,
        OrderedList,
        [
          Heading,
          {
            options: {
              levels: [1, 2, 3],
            },
          },
        ],
        Bold,
        Code,
        HorizontalRule,
        Paragraph,
        HardBreak,
        YoutubeEmbedExtension,
      ],
      nativeExtensions: [new YoutubeEmbed()],
    };
  },
  computed: {
    ...mapGetters({
      questions: "assessment/questions",
    }),
    isPageBreakAfter() {
      const questionIndex = this.questions.findIndex(
        (que) => que.questionId === this.question.questionId
      );
      if (questionIndex + 1 < this.questions.length) {
        return this.questions[questionIndex + 1].questionType === "PAGE_BREAK";
      } else {
        return false;
      }
    },
    html: {
      // two-way computed property
      get() {
        return this.question.html;
      },
      set(value) {
        this.updateQuestions({ ...this.question, html: value });
      },
    },
    points: {
      // two-way computed property
      get() {
        return this.question.points;
      },
      set(value) {
        this.updateQuestions({ ...this.question, points: value });
      },
    },
  },
  components: {
    TiptapVuetify,
  },
  methods: {
    ...mapMutations({
      updateQuestions: "assessment/updateQuestions",
    }),
    cloneValue() {
      return clone(this.value);
    },
    async handleDeleteQuestion(question) {
      // DELETE QUESTION
      const reallyDelete = await this.$swal({
        icon: "question",
        text: `Are you sure you want to delete the question?`,
        showCancelButton: true,
        confirmButtonText: "Yes, delete it",
        cancelButtonText: "No, cancel",
      });
      if (reallyDelete?.isConfirmed) {
        this.$emit("delete", question);
      }
    },
    async addPageBreakAfter(question) {
      this.$emit("page-break-after", question);
    },
    async removePageBreakAfter(question) {
      this.$emit("page-break-after-remove", question);
    },
  },
  watch: {
    value() {
      this.question = this.cloneValue();
    },
  },
};
</script>

<style lang="scss" scoped>
.tiptap-vuetify-editor::v-deep {
  box-shadow: none;
  border-radius: 4px;
  border: 1px solid map-get($grey, "base");
  overflow: hidden;

  .ProseMirror {
    margin: 20px 5px !important;

    .is-editor-empty::before {
      color: map-get($grey, "darken-1");
      font-style: normal;
    }
  }
  .tiptap-vuetify-editor__toolbar {
    border-top: 1px solid map-get($grey, "base");
    border-radius: 0 !important;
  }
  .v-card {
    display: flex;
    flex-direction: column-reverse;
  }
}
</style>
