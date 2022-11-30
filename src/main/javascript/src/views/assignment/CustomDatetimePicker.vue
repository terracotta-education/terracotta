<template>
  <v-datetime-picker
    class="custom-datetime-picker"
    v-bind="{
      dateFormat: 'M/d/yy',
      timeFormat: 'h:mm aaa',
      textFieldProps: {
        'append-icon': 'mdi-calendar',
        outlined: true,
        dense: true,
        'hide-details': true,
      },
      ...$attrs,
    }"
    v-on="$listeners"
    :datetime="value"
  >
    <template #dateIcon>
      <v-icon>mdi-calendar</v-icon>
    </template>
    <template #timeIcon>
      <v-icon>mdi-clock-outline</v-icon>
    </template>
    <template #actions="{ parent }">
      <v-btn color="grey lighten-1" text @click.native="parent.clearHandler">{{
        parent.clearText
      }}</v-btn>
      <v-btn color="primary" text @click="parent.okHandler">{{
        parent.okText
      }}</v-btn>
    </template>
  </v-datetime-picker>
</template>

<script>
export default {
  props: ["value"],
};
</script>

<style lang="scss" scoped>
.custom-datetime-picker::v-deep + .v-text-field {
  flex: 0 1 190px;
  margin-left: 0.5rem !important;
  margin-right: 0.5rem !important;
}
</style>
