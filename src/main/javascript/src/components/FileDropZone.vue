<template>
  <div class="file-drop-zone">
    <template v-if="!file">
      <div :class="['drop-zone', dragging ? 'drop-zone--over' : '']" @dragenter="dragging = true"
           @dragleave="dragging = false">
        <div class="drop-zone__info" @drag="onChange">
          <v-btn class="mb-3" elevation="0" color="primary">Upload PDF</v-btn>
          <p>or drag and drop here</p>
        </div>
        <input type="file" @change="onChange">
      </div>
    </template>
    <div v-else class="drop-zone__uploaded pa-3">
      <div class="drop-zone__uploaded-info">
        <h4 class="drop-zone__title">Selected file:</h4>
        <v-card outlined>
          <v-card-text class="py-1 px-2">
            <strong>Informed Consent File</strong>
            <v-btn
              class="remove-file"
              elevation="0"
              icon
              tile
              @click="removeFile"
            >
              <v-icon dark>
                mdi-close
              </v-icon>
            </v-btn>
          </v-card-text>
        </v-card>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: ['existing', 'fileName'],
  data() {
    return {
      file: null,
      dragging: false
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
        alert('please select a pdf file');
        this.dragging = false;
        return;
      }

      if (file.size > 500000000) {
        alert('please check file size no over 500 MB.')
        this.dragging = false;
        return;
      }

      this.file = file;
      this.dragging = false;
      this.$emit('update', file)
    },
    removeFile() {
      this.file = '';
      this.$emit('update', null)
    }
  },
  created: function () {
    this.file = this.existing?.length || null
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

    .remove-file {
      height: 30px;
      width: 30px;
      border: 1px solid map-get($grey, 'lighten-2');
      border-radius: 4px;

      &:hover,
      &:focus {
        background: map-get($red, 'base');
        color: white;
      }

      i {
        font-size: 16px;
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