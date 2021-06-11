<template>
  <div>
    <h1 class="my-3">
      <span class="green--text font-weight-bold"
        >You've completed section 2.</span
      ><br />
      Here's a summary of your experiment participation.
    </h1>
  </div>
</template>

<script>
export default {
  name: "ParticipationSummary",
  props: ["experiment"],

  beforeRouteEnter(to, from, next) {
    // Updating selection type for custom steps
    to.meta.selectionType = from.meta.selectionType;
    console.log(to.meta);
    console.log(from.meta);
    next();
  },
};
</script>
