<template>
  <v-app class="app">
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
            <v-card class="storage-access-card mx-auto" max-width="700">
              <v-card-title class="text-h5"
                >You are using a browser that requires we ask your permission to
                use cookies.</v-card-title
              >
              <v-card-text class="storage-access-card__text">
                <p>
                  For the best experience, we recommend
                  <strong
                    >opening
                    {{ isAssignment ? "your assignment" : "Terracotta" }} in
                    Chrome</strong
                  >.
                </p>
                <p>
                  Continuing in your current browser will require a few extra
                  steps (but don't worry; we'll walk you through them). You may
                  be asked to:
                </p>
                <ul>
                  <li>
                    Establish a first-party interaction with Terracotta.
                    <v-tooltip
                      top
                      color="#373d3f"
                      transition="slide-y-transition"
                      max-width="360px"
                    >
                      <template v-slot:activator="{ on, attrs }">
                        <span class="has-tooltip" v-bind="attrs" v-on="on"
                          >What is Terracotta?</span
                        >
                      </template>
                      <div>
                        <h3>What is Terracotta?</h3>
                        <div>
                          Terracotta is a Canvas plug-in that allows teachers
                          and researchers to embed studies directly in their
                          learning management system course sites.
                        </div>
                      </div>
                    </v-tooltip>
                  </li>
                  <li>
                    Allow Terracotta to use cookies and website data while you
                    use Canvas.
                    <v-tooltip
                      top
                      color="#373d3f"
                      transition="slide-y-transition"
                      max-width="360px"
                    >
                      <template v-slot:activator="{ on, attrs }">
                        <span class="has-tooltip" v-bind="attrs" v-on="on"
                          >Why does Terracotta need this information and how
                          will it be used?</span
                        >
                      </template>
                      <div>
                        <h3>How Terracotta uses cookies</h3>

                        <div>
                          Cookies allow Terracotta to associate your activity
                          with your Canvas account. Some browsers restrict 3rd
                          party access because it's often used to track activity
                          across multiple sites. That's not what Terracotta is
                          doing; it's only tracking your activity on this site
                          to ensure it's associated with your account.
                        </div>
                      </div>
                    </v-tooltip>
                  </li>
                </ul>
              </v-card-text>
              <v-card-actions>
                <v-btn
                  @click="requestStorageAccess"
                  color="primary"
                  elevation="0"
                  >Launch
                  {{ isAssignment ? "assignment" : "Terracotta" }}</v-btn
                >
              </v-card-actions>
            </v-card>
          </v-col></v-row
        ></v-container
      >

      <form
        ref="loginForm"
        action="/oidc/login_initiations"
        method="post"
        hidden
      >
        <div class="form-group">
          <label for="iss">iss:</label>
          <input
            type="text"
            id="iss"
            name="iss"
            :value="iss"
            class="form-control"
          />
        </div>
        <div class="form-group">
          <label for="login_hint">login_hint:</label>
          <input
            type="text"
            id="login_hint"
            name="login_hint"
            :value="loginHint"
            class="form-control"
          />
        </div>
        <div class="form-group">
          <label for="target_link_uri">target_link_uri:</label>
          <input
            type="text"
            id="target_link_uri"
            name="target_link_uri"
            :value="targetLinkUri"
            class="form-control"
          />
        </div>
        <div class="form-group">
          <label for="lti_message_hint">lti_message_hint:</label>
          <input
            type="text"
            id="lti_message_hint"
            name="lti_message_hint"
            :value="ltiMessageHint"
            class="form-control"
          />
        </div>
        <div class="form-group">
          <label for="client_id">client_id:</label>
          <input
            type="text"
            id="client_id"
            name="client_id"
            :value="clientId"
            class="form-control"
          />
        </div>
        <div class="form-group" v-if="ltiDeploymentId">
          <label for="lti_deployment_id">lti_deployment_id:</label>
          <input
            type="text"
            id="lti_deployment_id"
            name="lti_deployment_id"
            :value="ltiDeploymentId"
            class="form-control"
          />
        </div>
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
    </v-main>
  </v-app>
</template>

<script>
export default {
  props: [
    "targetLinkUri",
    "iss",
    "loginHint",
    "clientId",
    "ltiMessageHint",
    "ltiDeploymentId",
    "assignmentId",
  ],
  computed: {
    isAssignment() {
      return !!this.assignmentId;
    },
  },
  methods: {
    restartLoginProcess() {
      this.$refs.loginForm.submit();
    },
    requestFullWindowLaunch() {
      window.parent.postMessage(
        { subject: "requestFullWindowLaunch", data: this.targetLinkUri },
        "*"
      );
    },
    requestStorageAccess() {
      document
        .requestStorageAccess()
        .then(() => {
          // Since we only now have storage access, restart the login process
          // so that terracotta server can set a cookie for the session
          this.restartLoginProcess();
        })
        .catch(() => {
          console.error("Request storage access failed!");

          // This happens on Safari when there is no first-party interaction,
          // so we'll redirect to Terracotta for a first-party interaction
          this.requestFullWindowLaunch();
        });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "./styles/custom";

.app {
  background-color: map-get($map: $grey, $key: "lighten-4") !important;
}
.has-tooltip {
  text-decoration-style: dashed;
  text-decoration-line: underline;
  color: map-get($map: $blue, $key: "base");
}
.storage-access-card {
  padding: 32px;
}
.storage-access-card__text {
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
</style>
