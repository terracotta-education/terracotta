<template>
<v-container>
  <v-row>
    <v-col>
      <v-img
        class="mx-auto mt-7 mb-7"
        src="@/assets/terracotta_logo.svg"
        alt="Terracotta Logo"
        max-width="173"
      />
      <v-card
        class="oauth-permissions-card mx-auto"
        max-width="700"
      >
        <v-card-title
          class="text-h5"
        >
          Terracotta wants to access your {{ lmsTitle }} account
        </v-card-title>
        <v-card-text
          class="oauth-permissions-card__text"
        >
          <p>This will allow Terracotta to:</p>
          <ul
            class="permission-list"
          >
            <li>
              <div
                class="d-flex"
              >
                <div>List assignment submissions</div>
                <tool-tip
                  :content="`Terracotta will pull information from your ${lmsTitle} site which allows it to list students' names and assignments in order and match them up with the outcomes you choose.`"
                  header="What does this mean?"
                  aria-label="Assignment submission listing explanation tooltip"
                  alignment="top"
                  activatorType="icon"
                  icon="mdi-information-outline"
                  ref="ref-list-submissions-tooltip"
                />
              </div>
            </li>
            <li>
              <div
                class="d-flex"
              >
                <div>Create, list, edit and delete assignments</div>
                <tool-tip
                  :content="`When you create an assignment in Terracotta, it automatically creates that same assignment in ${lmsTitle}. Terracotta assignments are listed with other ${lmsTitle}
                    assignments, and when you edit or delete them in Terracotta, Terracotta communicates with ${lmsTitle} to keep the assignments up to date.`"
                  header="What does this involve?"
                  aria-label="Assignment management explanation tooltip"
                  alignment="top"
                  activatorType="icon"
                  icon="mdi-information-outline"
                  ref="ref-manage-assignments-tooltip"
                />
              </div>
            </li>
          </ul>
        </v-card-text>
        <v-card-actions>
          <v-btn
            :href="lmsApiOAuthURL"
            class="redirect-button"
            elevation="0"
          >
            Go to the {{ lmsTitle }} Authorization Page
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-col>
  </v-row>
</v-container>
</template>

<script>
import { mapGetters } from "vuex";
import ToolTip from "@/components/ToolTip.vue";

export default {
  components: {
    ToolTip
  },
  data: () => ({
    tooltipRefs: [
      "ref-list-submissions-tooltip",
      "ref-manage-assignments-tooltip"
    ]
  }),
  computed: {
    ...mapGetters({
      lmsApiOAuthURL: "api/lmsApiOAuthURL",
      configurations: "configuration/get"
    }),
    lmsTitle() {
      return this.configurations.lmsTitle || "LMS";
    }
  }
}
</script>

<style lang="scss" scoped>
@import "../styles/custom";

.oauth-permissions-card {
  padding: 32px;
}
.oauth-permissions-card__text {
  color: rgba(0, 0, 0, 0.87) !important;
}
.v-tooltip__content {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  a {
    color: #afdcff;
  }
}
.v-btn {
  &.redirect-button {
    background: #ebdcd2;
    border-radius: 48px;
    height: 42px;
    font-style: normal;
    font-weight: 400;
    font-size: 16px;
    line-height: 24px;
    /* identical to box height, or 150% */
    text-align: center;
    letter-spacing: 0.15px;
    text-transform: none;
    color: rgba(35, 48, 80, 0.87);
  }
}
.permission-list {
  li {
    font-size: 16px;
    line-height: 24px;
    letter-spacing: 0.15px;
    height: 24px;
    padding-bottom: 36px;
    margin-bottom: 18px;
    border-bottom: 1px solid #dddcd5;
  }
}
</style>
