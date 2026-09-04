<template>
  <div />
</template>

<script setup>
import { onMounted } from "vue";
import Swal from "sweetalert2";

defineOptions({
  name: "ConfirmationDialog"
});

const props = defineProps({
  title: {
    type: String,
    default: ""
  },
  body: {
    type: String,
    default: ""
  },
  showCancelButton: {
    type: Boolean,
    default: false
  },
  confirmButtonText: {
    type: String,
    default: "OK"
  },
  cancelButtonText: {
    type: String,
    default: "CANCEL"
  },
  reverseButtons: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "confirmed"
]);


const doDisplay = async () => {
  const result = await Swal.fire({
    title: props.title,
    html: props.body,
    showCancelButton: props.showCancelButton,
    confirmButtonText: props.confirmButtonText,
    cancelButtonText: props.cancelButtonText,
    reverseButtons: props.reverseButtons,
    allowOutsideClick: () => !Swal.isLoading()
  });

  emit("confirmed", result.isConfirmed);
};

onMounted(() => {
  doDisplay();
});

defineExpose({
  doDisplay
});
</script>

<style scoped>
:global(.swal2-container .swal2-popup) {
  width: 600px;
  border-radius: 10px;
}

:global(.swal2-container h2.swal2-title) {
  font-size: 28px;
  font-weight: 500;
  text-align: left;
}

:global(.swal2-container .swal2-html-container) {
  text-align: left;
}

:global(.swal2-container .swal2-actions) {
  align-items: end;
  justify-content: end;
}

:global(.swal2-container button.swal2-cancel),
:global(.swal2-container button.swal2-confirm) {
  background-color: transparent;
}

:global(.swal2-container button.swal2-cancel) {
  color: rgba(0, 0, 0, 0.66);
}

:global(.swal2-container button.swal2-confirm) {
  color: rgba(29, 157, 255, 1);
}

:global(.swal2-container .swal2-styled.swal2-confirm:focus) {
  box-shadow: none;
}
</style>
