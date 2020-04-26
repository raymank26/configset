<template>
  <div>
    <div class="row">
      <form onsubmit="return false;">
        <div class="form-row">
          <div class="col">
            <label class="mr-sm-2 sr-only" for="appSelect">Application</label>
            <select class="custom-select" id="appSelect">
              <option selected>Select application</option>
              <option value="1">One</option>
              <option value="2">Two</option>
              <option value="3">Three</option>
            </select>
          </div>
          <div class="col">
            <input class="form-control" placeholder="Host name" type="text" v-model="searchHost">
          </div>
          <div class="col">
            <input class="form-control" placeholder="Property name" type="text" v-model="searchPropertyName">
          </div>
          <div class="col">
            <input class="form-control" placeholder="Property value" type="text" v-model="searchPropertyValue">
          </div>
          <div class="col">
            <button class="btn btn-info" v-on:click="showProperties">Search properties</button>
          </div>
          <div class="col">
            <button class="btn btn-primary">Add new property</button>
          </div>
        </div>
      </form>
    </div>
    <div class="row">
      <properties-table :search-request="searchPropertiesRequest"></properties-table>
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Prop, Vue} from 'vue-property-decorator'
  import SearchPropertiesRequest from "@/model/SearchPropertiesRequest";
  import PropertiesTable from "@/components/PropertiesTable.vue";

  @Component({
      components: {PropertiesTable}
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
      this.searchApplicationName = this.propApplicationName;
      this.searchHost = this.propSearchHost;
      this.searchPropertyName = this.propSearchPropertyName;
      this.searchPropertyValue = this.propSearchPropertyValue;

      this.showProperties()
    }

    showProperties() {
      console.log("clicked");

      this.searchPropertiesRequest = new SearchPropertiesRequest(this.searchApplicationName, this.searchHost, this.searchPropertyName, this.searchPropertyValue)

      this.$router.replace({
        query: {
          applicationName: this.searchApplicationName || undefined,
          host: this.searchHost || undefined,
          propertyName: this.searchPropertyName || undefined,
          propertyValue: this.searchPropertyValue || undefined
        }
      }).catch(err => {
      })
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

  a {
    color: #42b983;
  }
</style>
