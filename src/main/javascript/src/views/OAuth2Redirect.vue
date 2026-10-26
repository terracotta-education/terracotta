<template>
<v-container>
  <v-row>
    <v-col>
      <v-img
        class="mx-auto mt-7 mb-7"
        :src="terracottaLogo"
        alt="Terracotta Logo"
        max-width="173"
      />
      <v-card
        class="oauth-permissions-card mx-auto"
        max-width="700"
      >
        <v-card-title
          class="text-headline-small text-wrap"
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
        <v-card-actions
          class="flex-column align-start"
        >
          <!-- opens in its own window: inside this frame the LMS can lose track of the request
               (third-party cookie rules) and show "Must submit new OAuth2 request". If the popup
               is blocked, the link itself opens the page in a new tab instead. -->
          <v-btn
            :href="lmsApiOAuthURL"
            target="_blank"
            class="redirect-button"
            elevation="0"
            @click="openAuthorization"
          >
            Go to the {{ lmsTitle }} Authorization Page
          </v-btn>
          <output
            v-if="waiting"
            class="authorization-waiting"
          >
            Finish authorizing in the window that opened. This page continues on its own once you're done.
          </output>
        </v-card-actions>
      </v-card>
    </v-col>
  </v-row>
</v-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import ToolTip from "@/components/ToolTip.vue";
import { api } from "@/store/api.module";
import terracottaLogo from "@/assets/terracotta_logo.svg";
import { configuration } from "@/store/configuration.module";
import { lmsAuthorizationService } from "@/services";

const AUTHORIZATION_WINDOW_NAME = "terracotta-lms-authorization";
const POLL_INTERVAL_MS = 2000;
const POLL_LIMIT_MS = 15 * 60 * 1000;

const apiStore = api();
const configurationStore = configuration();
const router = useRouter();

const waiting = ref(false);
let pollTimer = null;
let pollStartedAt = 0;

const lmsApiOAuthURL = computed(() => {
  return apiStore.lmsApiOAuthURL;
});

const configurations = computed(() => {
  return configurationStore.get;
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "LMS";
});

const stopPolling = () => {
  clearTimeout(pollTimer);
  pollTimer = null;
};

// (re)starts the fifteen minute window, so a click late in it still gets a full wait
const startPolling = () => {
  pollStartedAt = Date.now();

  if (!pollTimer) {
    pollTimer = setTimeout(checkAuthorization, POLL_INTERVAL_MS);
  }
};

// the authorization window can't reliably message this frame (the LMS may cut the opener link),
// so ask the server until the new token shows up. This runs from the moment the page opens, so it
// also catches authorizing in a tab opened by right click, which never fires the click handler.
const checkAuthorization = async () => {
  const { authorized } = await lmsAuthorizationService.getAuthorization();

  if (!pollTimer) {
    return; // left the page while the request was in flight
  }

  if (authorized) {
    stopPolling();
    waiting.value = false;
    apiStore.setLmsApiOAuthURL("");
    await router.replace({ name: "Home" });
    return;
  }

  if (Date.now() - pollStartedAt >= POLL_LIMIT_MS) {
    stopPolling();
    waiting.value = false;
    return;
  }

  pollTimer = setTimeout(checkAuthorization, POLL_INTERVAL_MS);
};

const openAuthorization = event => {
  if (!lmsApiOAuthURL.value) {
    return;
  }

  const authorizationWindow = window.open(lmsApiOAuthURL.value, AUTHORIZATION_WINDOW_NAME, "popup,width=900,height=760");

  if (authorizationWindow) {
    event.preventDefault();
  }

  waiting.value = true;
  startPolling();
};

onMounted(startPolling);
onBeforeUnmount(stopPolling);
</script>

<style lang="scss" scoped>
.oauth-permissions-card {
  padding: 32px;
}
.oauth-permissions-card__text {
  color: rgba(0, 0, 0, 0.87) !important;
}
// tooltip content is teleported outside this component's DOM subtree, so :deep() (a descendant
// selector rooted at this component's scope attribute) can't reach it - :global() is required
:global(.v-tooltip > .v-overlay__content) {
  max-width: 400px;
  opacity: 1.0 !important;
  background-color: rgba(55,61,63, 1.0) !important;
  color: #fff !important;

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
.authorization-waiting {
  display: block;
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 20px;
  color: rgba(0, 0, 0, 0.6);
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
