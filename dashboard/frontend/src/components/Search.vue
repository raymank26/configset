<template>
  <div>
    <div class="row mb-4">
      <div class="col-12">
        <form onsubmit="return false;">
          <div class="form-row align-items-center">
            <div class="col-auto">
              <label class="mr-sm-2 sr-only" for="appSelect">Application</label>
              <application-select class="form-control mb-2" :always-show-options="true" id="appSelect"
                                  v-model="searchApplicationName"/>
            </div>
            <div class="col-auto">
              <input class="form-control mb-2" placeholder="Host name" type="text" v-model="searchHost">
            </div>
            <div class="col-auto">
              <input class="form-control mb-2" placeholder="Property name" type="text" v-model="searchPropertyName">
            </div>
            <div class="col-auto">
              <input class="form-control mb-2" placeholder="Property value" type="text" v-model="searchPropertyValue">
            </div>
            <div class="col-auto">
              <button class="form-control mb-2 btn btn-primary" v-on:click="showProperties">Search properties</button>
            </div>
            <div class="col-auto">
              <router-link class="form-control mb-2 btn btn-success"
                           v-bind:to="{path: '/updateProperty', query: {applicationName: searchApplicationName}}">Add
                new property
              </router-link>
            </div>
            <div class="col-auto">
              <router-link class="form-control mb-2 btn btn-success"
                           v-bind:to="{path: '/importProperties', query: {applicationName: searchApplicationName}}">
                Import properties
              </router-link>
            </div>
          </div>
        </form>
      </div>
    </div>
    <div class="row">
      <div class="col-10">
        <properties-table :search-request="searchPropertiesRequest"></properties-table>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Prop, Vue} from 'vue-property-decorator'
  import SearchPropertiesRequest from "@/model/SearchPropertiesRequest";
  import PropertiesTable from "@/components/PropertiesTable.vue";
  import {searchService} from "@/service/services";
  import ApplicationSelect from "@/components/ApplicationSelect.vue";

  @Component({
      components: {ApplicationSelect, PropertiesTable}
    }
  )
  export default class Search extends Vue {
    @Prop() private propApplicationName!: string | null;
    @Prop() private propSearchHost!: string | null;
    @Prop() private propSearchPropertyName!: string | null;
    @Prop() private propSearchPropertyValue!: string | null;

    private searchApplicationName: string | null = "";
    private searchHost: string | null = null;
    private searchPropertyName: string | null = null;
    private searchPropertyValue: string | null = null;

    private searchPropertiesRequest: SearchPropertiesRequest | null = null;

    created() {
      this.searchApplicationName = this.propApplicationName || "";
      this.searchHost = this.propSearchHost;
      this.searchPropertyName = this.propSearchPropertyName;
      this.searchPropertyValue = this.propSearchPropertyValue;

      this.showProperties()
    }

    showProperties() {
      if (!!this.searchApplicationName || !!this.searchHost || !!this.searchPropertyName || !!this.searchPropertyValue) {
        this.searchPropertiesRequest = new SearchPropertiesRequest(this.searchApplicationName, this.searchHost,
          this.searchPropertyName, this.searchPropertyValue);
      } else {
        this.searchPropertiesRequest = null;
      }

      searchService.searchReplace({
        searchApplicationName: this.searchApplicationName,
        searchHost: this.searchHost,
        searchPropertyName: this.searchPropertyName,
        searchPropertyValue: this.searchPropertyValue
      });
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped>
  h3 {
    margin: 40px 0 0;
  }

  ul {
    list-style-type: none;
    padding: 0;
  }

  li {
    display: inline-block;
    margin: 0 10px;
  }
</style>
