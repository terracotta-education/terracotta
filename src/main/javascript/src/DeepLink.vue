<template>
<v-app
  class="app"
>
  <v-main>
    <v-container
      class="deep-link"
    >
      <div
          class="terracotta-appbg"
      ></div>
      <v-row
        justify="center"
      >
        <v-col
          col="8"
          class="mt-15"
        >
          <v-img
            src="@/assets/terracotta_logo.svg"
            alt="Terracotta Logo"
            class="terracotta-logo mb-4 mx-auto"
            max-width="200"
          />
          <h1
            class="experimental-header mb-5"
          >
            Experimental research in the LMS
          </h1>
          <p
            class="mb-3 text-center"
          >
            Welcome to Terracotta, the platform that supports teachers' and researchers' abilities to easily run experiments in live classes.
          </p>
          <v-row>
            <form
              :action="deepLinkReturnUrl"
              id="deep-link-response-form"
              method="POST"
            >
              <v-btn
                :disabled="!sendToLmsEnabled"
                @click="sendToLms"
                class="experiment-btn"
                color="primary"
                elevation="0"
              >
                ADD TERRACOTTA TO YOUR COURSE
              </v-btn>
              <div
                class="form-group"
              >
                <input
                  id="jwt"
                  name="JWT"
                  type="hidden"
                />
              </div>
            </form>
          </v-row>
        </v-col>
      </v-row>
    </v-container>
  </v-main>
</v-app>
</template>

<script>
import { mapActions } from "vuex";

export default {
props: {
  id: {
    type: String,
    required: true
  }
},
data: () => ({
  ltiDeepLink: null,
}),
computed: {
  deepLinkReturnUrl() {
    return this.ltiDeepLink ? this.ltiDeepLink.returnUrl : null;
  },
  sendToLmsEnabled() {
    return this.deepLinkReturnUrl !== null;
  }
},
methods: {
  ...mapActions({
    deepLinkJwt: "api/deepLinkJwt"
  }),
  sendToLms() {
    document.getElementById("deep-link-response-form").submit();
  },
  getJwt() {
    this.deepLinkJwt(this.id).then(
      data => {
        // set the JWT string in the form
        document.getElementById("jwt").value = data.jwt;
        this.ltiDeepLink = data;
      }
    ).catch(
      error => {
        console.error("Error:", error);
      }
    );
  }
},
mounted() {
  this.getJwt();
}
};
</script>

<style lang="scss">
.deep-link {
  & .terracotta-appbg {
    background: url("~@/assets/terracotta_appbg.jpg") no-repeat center center;
    background-size: cover;
    height: 100%;
    width: 100%;
    position: fixed;
    top: 0;
    left: 0;
    opacity: 0.5;
  }
  & .terracotta-appbg + * {
    position: relative; /*place the content above the terracotta-appbg*/
  }
  & h1 {
    &.experimental-header {
      font-size: 48px;
      font-weight: 200;
      text-align: center;
    }
  }
  & button {
    &.experiment-btn {
      margin: 0 auto;
      max-width: fit-content;
    }
  }
  & #deep-link-response-form {
    max-width: fit-content;
    margin: 40px auto;
  }
}
</style>
