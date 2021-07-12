<template>
  <v-row>
    <v-col cols="2">
      <v-btn
        icon
        tile
        class="correct"
        @click="handleToggleCorrect(i)"
      >
        <v-icon>mdi-check</v-icon>
      </v-btn>
    </v-col>
    <v-col cols="8">
      <v-text-field
        v-model="option.option"
        :label="`Option ${i}`"
        outlined
      ></v-text-field>
    </v-col>
    <v-col class="py-0" cols="2">
      <v-btn
        icon
        tile
        class="delete_option"
        @click="handleDeleteOption(i)"
      >
        <v-icon>mdi-delete</v-icon>
      </v-btn>
    </v-col>
  </v-row>
</template>
<script>

export default {
  name: 'TerracottaBuilder',
  props: ['option'],

}
</script>