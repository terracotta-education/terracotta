<template>
  <v-app
    class="app"
  >
    <v-main>
      <v-container>
        <v-row>
          <v-col>
            <v-img
              class="mx-auto mb-10"
              src="@/assets/terracotta_logo.svg"
              alt="Terracotta Logo"
              max-width="173"
            />
            <v-card
              class="first-party-card mx-auto"
            >
              <div
                class="pt-5"
              >
                <div
                  class="icon-circle icon-circle-success"
                >
                  <v-icon>
                    mdi-check
                  </v-icon>
                </div>
              </div>
              <div>
                <v-card-title>
                  Preview complete!
                </v-card-title>
                <v-card-text
                  class="first-party-card__text"
                >
                  <div>
                    Please close your tab to return to Terracotta.
                  </div>
                </v-card-text>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
export default { }
</script>

<style lang="scss" scoped>
  @import "../../styles/custom";

  .app {
    background-color: rgba(253, 245, 242, 1) !important;
    padding-top: 80px;
  }
  .first-party-card {
    min-width: 475px;
    max-width: 475px;
    padding: 32px;
    display: flex;
    justify-content: space-between;
    border-radius: 10px;
    & .first-party-card__text {
      color: rgba(0, 0, 0, 0.87) !important;
    }
    & .icon-circle {
      width: 54px;
      height: 54px;
      border-radius: 50%;
      text-align: center;
      font-size: 24px;
      font-weight: bold;
      align-content: center;
      opacity: 1;
      &.icon-circle-success {
        border: 2px solid rgba(56, 173, 182, 1);
        background-color: rgba(56, 173, 182, .2);
        color:rgba(56, 173, 182, 1);
        > .v-icon {
          color:rgba(56, 173, 182, 1);
        }
      }
    }
    & .v-card__title {
      flex-wrap: nowrap;
      font-size: 28px;
      font-weight: unset;
    }
    & .v-card__text {
      font-size: 16px;
    }
  }
</style>
