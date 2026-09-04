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
            :src="terracottaLogo"
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
              ref="formRef"
              id="deep-link-response-form"
              :action="deepLinkReturnUrl"
              method="POST"
            >
              <v-btn
                :disabled="!sendToLmsEnabled || isLoading"
                :loading="isLoading"
                @click.prevent="sendToLms"
                class="experiment-btn"
                color="primary"
                elevation="0"
              >
                ADD TERRACOTTA TO YOUR COURSE
              </v-btn>

              <div class="form-group">
                <input
                  ref="jwtInput"
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

<script setup>
import { computed, onMounted, ref, toRef } from "vue";
import { api as useApiStore } from "@/store/api.module";
import terracottaLogo from "@/assets/terracotta_logo.svg";

const props = defineProps({
  id: {
    type: String,
    required: true
  }
});

const id = toRef(props, "id");
const formRef = ref(null);
const jwtInput = ref(null);
const ltiDeepLink = ref(null);
const isLoading = ref(false);
const error = ref(null);

const deepLinkReturnUrl = computed(() => ltiDeepLink.value?.returnUrl || null);
const sendToLmsEnabled = computed(() => !!deepLinkReturnUrl.value && !!ltiDeepLink.value?.jwt);

const getJwt = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const data = await useApiStore().deepLinkJwt(id.value);
    if (jwtInput.value) {
      jwtInput.value.value = data.jwt;
    }
    ltiDeepLink.value = data;
  } catch (err) {
    console.error("Error:", err);
    error.value = err;
  } finally {
    isLoading.value = false;
  }
};

const sendToLms = () => {
  formRef.value?.submit();
};

onMounted(() => {
  getJwt();
});
</script>

<style lang="scss">
.deep-link {
  & .terracotta-appbg {
    background: url("@/assets/terracotta_appbg.jpg") no-repeat center center;
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
