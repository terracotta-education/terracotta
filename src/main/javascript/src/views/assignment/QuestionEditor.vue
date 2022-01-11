<template>
  <div>
    <tiptap-vuetify
      v-model="question.html"
      placeholder="Question"
      class="mb-6 outlined"
      :extensions="extensions"
      :native-extensions="nativeExtensions"
      :card-props="{ flat: true }"
      :rules="rules"
      required
      @input="emitValueChanged"
    />
    <v-text-field
      v-model="question.points"
      :rules="numberRule"
      label="Points"
      type="number"
      step="any"
      outlined
      required
      @input="emitValueChanged"
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

/*
 * Events:
 * - input: question has been updated
 *   - args: question
 * - delete: user has confirmed deletion of a question
 *   - args: question
 */
export default {
  props: ["value"],
  data() {
    return {
      question: this.cloneValue(),
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
  components: {
    TiptapVuetify,
  },
  methods: {
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
    emitValueChanged() {
      this.$emit("input", this.question);
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
