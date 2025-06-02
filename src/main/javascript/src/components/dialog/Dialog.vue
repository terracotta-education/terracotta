<template>
  <div></div>
</template>

<script>
export default {
  props: {
    title: {
      type: String
    },
    body: {
      type: String
    },
    showCancelButton: {
      type: Boolean
    },
    confirmButtonText: {
      type: String
    },
    cancelButtonText: {
      type: String
    },
    reverseButtons: {
      type: Boolean
    }
  },
  computed: {
    getTitle() {
      return this.title || "";
    },
    getBody() {
      return this.body || "";
    },
    isShowCancelButton() {
      return this.showCancelButton !== null ? this.showCancelButton : false;
    },
    getCancelButtonText() {
      return this.cancelButtonText || "CANCEL";
    },
    getConfirmButtonText() {
      return this.confirmButtonText || "OK";
    },
    isReverseButtons() {
      return this.reverseButtons !== null ? this.reverseButtons : false;
    }
  },
  methods: {
    async doDisplay() {
      const result = await this.$swal({
        title: this.getTitle,
        html: this.getBody,
        showCancelButton: this.isShowCancelButton,
        confirmButtonText: this.getConfirmButtonText,
        cancelButtonText: this.getCancelButtonText,
        reverseButtons: this.isReverseButtons,
        allowOutsideClick: () => !this.$swal.isLoading(),
      });

      this.$emit("confirmed", result.isConfirmed);
    }
  },
  mounted() {
    this.doDisplay();
  }
}
</script>

<style scoped>
.swal2-container {
  & .swal2-popup {
    width: 600px;
    border-radius: 10px;
  }
  & h2.swal2-title {
    font-size: 28px;
    font-weight: 500;
    text-align: left;
  }
  & .swal2-html-container {
    text-align: left;
  }
  & .swal2-actions {
    align-items: end;
    justify-content: end;
  }
  & button.swal2-cancel,
  & button.swal2-confirm {
    background-color: transparent;
  }
  & button.swal2-cancel {
    color: rgba(0, 0, 0, .66);
  }
  & button.swal2-confirm {
    color: rgba(29, 157, 255, 1);
  }
  & .swal2-styled.swal2-confirm:focus {
    box-shadow: none;
  }
}
</style>
