<template>
  <div class="file-drop-zone">
    <template v-if="!file">
      <div
        :class="['drop-zone', dragging ? 'drop-zone--over' : '']"
        @dragenter="dragging = true"
        @dragleave="dragging = false"
      >
        <div
          class="drop-zone__info"
          @drag="onChange"
        >
          <v-btn
            class="mb-3"
            elevation="0"
            color="primary"
          >
            Upload PDF
          </v-btn>
          <p>or drag and drop here</p>
        </div>
        <input
          type="file"
          accept=".pdf,application/pdf"
          @change="onChange"
        >
      </div>
    </template>
    <div
      v-else
      class="drop-zone__uploaded pa-3"
    >
      <div class="drop-zone__uploaded-info">
        <h4 class="drop-zone__title">Selected file:</h4>
        <v-card outlined>
          <v-card-text class="py-1 px-2">
            <strong>Informed Consent File</strong>
            <div>
              <v-btn
                class="icon-file-view"
                elevation="0"
                icon
                tile
                @click="doDisplayFile"
              >
                <v-icon>
                  mdi-file-eye-outline
                </v-icon>
              </v-btn>
              <v-btn
                class="icon-file-remove"
                elevation="0"
                icon
                tile
                @click="removeFile"
              >
                <v-icon dark>
                  mdi-close
                </v-icon>
              </v-btn>
            </div>
          </v-card-text>
        </v-card>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: ['existingFile'],
  data() {
    return {
      file: null,
      dragging: false
    }
  },
  watch: {
    existingFile(newFile) {
      this.file = newFile;
    }
  },
  methods: {
    onChange(e) {
      var files = e.target.files || e.dataTransfer.files;

      if (!files.length) {
        this.dragging = false;
        return;
      }

      this.createFile(files[0]);
    },
    createFile(file) {
      if (!file.type.match('application/pdf')) {
        this.$swal('Please select a pdf file.');
        this.dragging = false;
        return;
      }

      if (file.size > 10*1024*1024) {
        this.$swal('Please check file size is not over 10 MB.')
        this.dragging = false;
        return;
      }

      this.file = file;
      this.dragging = false;
      this.$emit('update', file)
      this.$emit('newUpload', true);
    },
    removeFile() {
      this.file = '';
      this.$emit('update', null);
      this.$emit('newUpload', true);
      this.$emit('displayFile', false);
    },
    doDisplayFile() {
      this.$emit('displayFile', true);
    }
  },
  computed: {
    extension() {
      return (this.file) ? this.file.name.split('.').pop() : '';
    }
  },
}
</script>

<style lang="scss">
@import '~@/styles/variables';

.drop-zone {
  height: 153px;
  position: relative;
  border: 2px dashed map-get($grey, 'lighten-2');
  border-radius: 9px;

  &:hover {
    border: 2px dashed map-get($blue, 'lighten-2');
  }

  &--over {
    background: map-get($grey, 'lighten-2');
    border: 2px solid map-get($blue, 'lighten-2');
    opacity: 0.8;
  }

  &__info {
    position: absolute;
    top: 50%;
    width: 100%;
    transform: translate(0, -50%);
    text-align: center;

    p {
      margin: 0;
      padding: 0;
    }
  }

  &__uploaded {
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: center;
    height: 153px;
    border: 2px dashed map-get($grey, 'lighten-2');
    border-radius: 9px;

    * {
      color: black;
    }

    .v-card__text {
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: space-between;
    }

    .icon-file-remove,
    .icon-file-view {
      height: 30px;
      width: 30px;
      border: 1px solid map-get($grey, 'lighten-2');
      border-radius: 4px;

      i {
        font-size: 16px;
      }
    }

    .icon-file-remove {
      &:hover,
      &:focus {
        background: map-get($red, 'base');
        color: white;
      }
    }

    .icon-file-view {
      &:hover,
      &:focus {
        background: map-get($grey, 'lighten-2');
        color: white;
      }
    }
  }

  input {
    position: absolute;
    cursor: pointer;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
  }
}
</style>
