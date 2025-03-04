<template>
  <v-row>
    <v-col
      cols="1"
    >
      &nbsp;
    </v-col>
    <v-col
      cols="10"
    >
      <v-card
        :class="border"
        outlined
      >
        <v-card-title
          class="py-0"
        >
          <slot />
        </v-card-title>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
export default {
  props: {
    correct: {
      default() {
        return null;
      }
    }
  },
  computed: {
    border() {
      return this.correct ? 'green--border' : this.correct === null ? '' : 'red--border';
    }
  }
};
</script>

<style>
.green--border {
  border-color: #38ADB6 !important;
  border-width: 2px !important;
}
.red--border {
  border-color: #E5153E !important;
  border-width: 2px !important;
}
</style>
