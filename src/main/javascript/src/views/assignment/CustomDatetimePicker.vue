<template>
  <date-time-picker
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
    class="custom-datetime-picker"
  >
    <template
      #dateIcon
    >
      <v-icon>mdi-calendar</v-icon>
    </template>
    <template
      #timeIcon
    >
      <v-icon>mdi-clock-outline</v-icon>
    </template>
    <template
      #actions="{ parent }"
    >
      <v-btn
        @click.native="parent.clearHandler"
        color="grey lighten-1"
        text
      >
        {{ parent.clearText }}
      </v-btn>
      <v-btn
        @click="parent.okHandler"
        color="primary"
        text
      >
        {{ parent.okText }}
      </v-btn>
    </template>
  </date-time-picker>
</template>

<script>
import DateTimePicker from "@/components/picker/DateTimePicker.vue";

export default {
  components: {
    DateTimePicker,
  },
  props: {
    value: {
      type: [Date, String],
      default: null
    }
  }
};
</script>

<style lang="scss" scoped>
.custom-datetime-picker::v-deep + .v-text-field {
  flex: 0 1 190px;
  margin-left: 0.5rem !important;
  margin-right: 0.5rem !important;
}
</style>
