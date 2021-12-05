<template>
  <v-row>
    <v-col cols="1">
      &nbsp;
    </v-col>
    <v-col cols="10">
      <v-card outlined>
        <v-card-title class="py-0">
          <slot />
        </v-card-title>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
export default {};
</script>

<style></style>
