<template>
  <div>
    <div>First Party Interaction!</div>
    <v-btn color="primary" elevation="0" :href="platformRedirectUrl"
      >Return to Canvas</v-btn
    >
  </div>
</template>

<script>
export default {
  props: ["platformRedirectUrl"],
};
</script>
