<template>
  <div>

  </div>
</template>

<script lang="ts">
  import {Component, Prop, Vue, Watch} from 'vue-property-decorator'
  import SearchPropertiesRequest from "../model/SearchPropertiesRequest";

  @Component
  export default class PropertiesTable extends Vue {
    @Prop()
    private searchRequest?: SearchPropertiesRequest;

    created() {
      if (this.searchRequest) {
        this.updateContent()
      }
    }

    @Watch("searchRequest")
    updateContent() {
      console.log(this.searchRequest)
    }

  }
</script>

<style scoped>

</style>
