<template>
  <div>
    <h1 class="my-3">
      Which students can participate in the study?
    </h1>

    <!-- Groups Section -->
    <p>Groups</p>
    <v-expansion-panels class="v-expansion-panels--icon" flat>
      <v-expansion-panel>
        <v-expansion-panel-header>
          Participating (0)
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            Participating Students list goes here
          </p>
        </v-expansion-panel-content>
      </v-expansion-panel>

      <v-expansion-panel>
        <v-expansion-panel-header>
          Not Participating (0)
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            Not Participating Students list goes here
          </p>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <p class="mt-3">Students (0)</p>
    <v-row class="mx-3">
      <v-row align="center" class="mx-3">
        <v-checkbox on-icon="$checkboxIndeterminate" color="primary" />
        0 Selected
      </v-row>
      <!-- Dropdown Menu -->
      <v-menu offset-y>
        <template v-slot:activator="{ on, attrs }">
          <v-btn color="primary" v-bind="attrs" v-on="on">MOVE TO</v-btn>
        </template>
        <v-list>
          <v-list-item-group>
            <template v-for="item in ['Participating', 'Not Participating']">
              <v-list-item :key="item">
                <v-list-item-content>
                  <v-list-item-title> {{ item }}</v-list-item-title>
                </v-list-item-content>
              </v-list-item>
            </template>
          </v-list-item-group>
        </v-list>
      </v-menu>
    </v-row>

    <!-- List of available participants -->
    <v-list class="mt-5" outlined rounded>
      <v-list-item-group v-model="settings" multiple>
        <template v-for="(item, index) in ['1', '2', '3', '4']">
          <v-list-item :key="item">
            <v-list-item-action>
              <v-checkbox :input-value="active" color="primary"></v-checkbox>
            </v-list-item-action>

            <v-list-item-content>
              <v-list-item-title>{{ item }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
          <v-divider v-if="index !== 3" class="mx-4" :key="item" />
        </template>
      </v-list-item-group>
    </v-list>

    <div class="mt-5">
      <v-btn
        elevation="0"
        color="primary"
        :to="{ name: 'ParticipationSummary' }"
        >Continue
      </v-btn>
    </div>
  </div>
</template>

<script>
export default {
  name: "ParticipationTypeManualSelection",
  props: ["experiment"],
};
</script>
