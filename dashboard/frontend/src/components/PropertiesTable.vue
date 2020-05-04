<template>
  <div>
    <div v-if="properties">
      <div class="row" v-for="(appProperties, appName) in properties">
        <table class="table table-striped">
          <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">Property name</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="propertyName in appProperties">
            <td>
              <button class="btn btn-info">Select</button>
            </td>
            <td>{{ propertyName }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Prop, Vue, Watch} from 'vue-property-decorator'
  import SearchPropertiesRequest from "../model/SearchPropertiesRequest";
  import {propertyService} from "@/service/services";
  import SearchPropertiesResult from "@/model/SearchPropertiesResult";

  @Component
  export default class PropertiesTable extends Vue {
    @Prop()
    private searchRequest?: SearchPropertiesRequest;

    private properties: SearchPropertiesResult | null = null;

    created() {
      if (this.searchRequest) {
        this.updateContent()
      } else {
        this.properties = null;
      }
    }

    @Watch("searchRequest")
    updateContent() {
      if (this.searchRequest) {
        propertyService.searchProperties(this.searchRequest!!).then(properties => {
          this.properties = properties;
        });
      } else {
        this.properties = null;
      }
    }
  }
</script>

<style scoped>

</style>
