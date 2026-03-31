<template>
<v-app
  class="app"
>
  <v-main>
    <v-container>
      <v-row>
        <v-col>
          <v-img
            class="mx-auto mb-5"
            src="@/assets/terracotta_logo.svg"
            alt="Terracotta Logo"
            max-width="173"
          />
          <v-card
            class="first-party-card mx-auto"
            max-width="700"
          >
            <v-card-title
              class="text-h5"
            >
              You need to establish a first-party interaction with Terracotta.
            </v-card-title>
            <v-card-text
              class="first-party-card__text"
            >
              <p>
                Click the button below to establish the connection. This will return you to your LMS {{ isAssignment ? "assignment" : "" }},
                and you'll see the same pop-up window as before. It may look like nothing has changed, but please click
                {{ isAssignment ? '"Launch Assignment"' : '"Launch Terracotta"' }} again.
              </p>
              <tool-tip
                header="First-party interaction"
                content="A first-party interaction allows the site you're visiting to create a cookie. This makes it possible for the browser
                    to remember your login information and to keep the session open. Establishing this kind of interaction will make it
                    possible for you to use Terracotta with your current browser."
                aria-label="First-party interaction explanation tooltip"
                alignment="top"
                activatorType="paragraph"
                activatorContent="What is a first party interaction and why am I being asked this?"
              />
            </v-card-text>
            <v-card-actions>
              <v-btn
                :href="platformRedirectUrl"
                color="primary"
                elevation="0"
              >
                Return to your LMS
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</v-app>
</template>

<script>
import { mapGetters } from "vuex";
import ToolTip from "@/components/ToolTip.vue";

export default {
  components: {
    ToolTip
  },
  props: {
    platformRedirectUrl: {
      type: String,
      required: true
    },
    assignmentId: {
      type: Number,
      required: false
    }
  },
  computed: {
    ...mapGetters({
      configurations: "configuration/get"
    }),
    isAssignment() {
      return !!this.assignmentId;
    }
  }
};
</script>

<style lang="scss" scoped>
@import "./styles/custom";

.app {
  background-color: map-get($grey, "lighten-4") !important;
  & .first-party-card {
    padding: 32px;
    & .first-party-card__text {
      color: rgba(0, 0, 0, 0.87) !important;
    }
  }
}
</style>
