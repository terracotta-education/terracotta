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
                max-width="700"
              >
                <v-card-text
                  class="first-party-card__text"
                >
                  <div
                    class="icon-circle icon-circle-invalid"
                  >
                    <v-icon>
                      mdi-exclamation
                    </v-icon>
                  </div>
                  <div>
                      This assignment was copied when you copied your Canvas course, but is linked to an inactive Terracotta experiment.
                      Please click the Terracotta link from within <i>this</i> Canvas course site and recreate your experiment within Terracotta.
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </v-container>
      </v-main>
    </v-app>
  </template>

  <style lang="scss" scoped>
    @import "../../styles/custom";

    .app {
      background-color: rgba(253, 245, 242, 1) !important;
      padding-top: 80px;
    }
    .first-party-card {
      padding: 32px;
      display: flex;
      justify-content: space-between;
      border-radius: 10px;
      & .first-party-card__text {
        color: rgba(0, 0, 0, 0.87) !important;
      }
      & .icon-circle {
        min-width: 54px;
        min-height: 54px;
        margin: auto 20px auto 0;
        border-radius: 50%;
        text-align: center;
        font-size: 24px;
        font-weight: bold;
        align-content: center;
        opacity: 1;
        &.icon-circle-invalid {
          border: 2px solid rgba(229, 21, 62, 1);
          background-color: rgba(229, 21, 62, .2);
          color:rgba(229, 21, 62, 1);
          > .v-icon {
            color:rgba(229, 21, 62, 1);
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
        display: flex;
        flex-direction: row;
      }
    }
  </style>
