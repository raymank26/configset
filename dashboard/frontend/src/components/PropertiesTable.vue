<template>
  <div>
    <div v-if="Object.keys(properties).length > 0">
      <div v-for="(byApp, appName) in properties">
        <h5>Application = {{ appName }}</h5>
        <table class="table table-bordered">
          <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">Application name</th>
            <th scope="col">Property name</th>
            <th scope="col">Property</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(byName, propertyName) in byApp.byName">
            <td>
              <button class="btn btn-info" v-on:click="enableShow(byName)">Select</button>
            </td>
            <td>
              {{ appName }}
            </td>
            <td>
              {{ propertyName }}
            </td>
            <td class="property-content">
              <table class="property-content-table" v-show="byName.showEnabled">
                <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">Host</th>
                  <th scope="col">Value</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(property, host) in byName.byHost">
                  <td>
                    <button class="btn btn-success mr-1" v-on:click="updateProperty(property.prop)">Update</button>
                    <button class="btn btn-danger" v-on:click="deleteProperty(property.prop)">Delete</button>
                  </td>
                  <td>{{ property.prop.hostName }}</td>
                  <td>{{ property.prop.propertyValue }}</td>
                </tr>
                </tbody>
              </table>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-else-if="searchRequest">
      <p>No properties found</p>
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Prop, Vue, Watch} from 'vue-property-decorator'
  import SearchPropertiesRequest from "../model/SearchPropertiesRequest";
  import {propertyService} from "@/service/services";
  import ShowPropertyItem from "@/model/ShowPropertyItem";
  import {Location} from "vue-router";
  import {DeleteResult} from "@/service/PropertyService";

  @Component
  export default class PropertiesTable extends Vue {
    @Prop()
    private searchRequest?: SearchPropertiesRequest;

    private properties: Record<string, PropByApp> = {};

    created() {
      if (this.searchRequest) {
        this.updateContent()
      }
    }

    @Watch("searchRequest")
    updateContent() {
      if (this.searchRequest) {
        propertyService.searchProperties(this.searchRequest!!).then(properties => {
          let newProps: Record<string, PropByApp> = {};
          let showEnabled = properties.length == 1;
          for (let prop of properties) {
            let propByApp = getOrCreate(newProps, prop.applicationName, () => new PropByApp(prop.applicationName));
            let propByName = getOrCreate(propByApp.byName, prop.propertyName, () => new PropByName(prop.propertyName, showEnabled));
            propByName.byHost[prop.hostName] = new PropByHost(prop.hostName, prop);
          }
          this.properties = newProps;
        });
      } else {
        this.properties = {};
      }
    }

    enableShow(propByName: PropByName) {
      propByName.showEnabled = true;
    }

    updateProperty(property: ShowPropertyItem) {
      this.$router.push({
        path: "/updateProperty",
        query: {
          applicationName: property.applicationName,
          hostName: property.hostName,
          propertyName: property.propertyName,
        }
      } as Location)
    }

    deleteProperty(property: ShowPropertyItem) {
      let toDelete = confirm("Delete property?");
      if (toDelete) {
        propertyService.deleteProperty(property.applicationName, property.hostName, property.propertyName, property.version)
          .then(response => {
            switch (response) {
              case DeleteResult.OK:
                this.$router.go(0);
                break;
              case DeleteResult.CONFLICT:
                alert("Property was changed (you tried to delete stale property). Please update the page manually to see the changes.");
                break;
            }
          })
      }
    }
  }

  function getOrCreate<T>(record: Record<string, T>, key: string, factory: () => T): T {
    if (!(key in record)) {
      record[key] = factory()
    }
    return record[key]
  }

  class PropByApp {
    applicationName: string;
    byName: Record<string, PropByName>;

    constructor(applicationName: string) {
      this.applicationName = applicationName;
      this.byName = {};
    }
  }

  class PropByName {
    propertyName: string;
    byHost: Record<string, PropByHost>;
    showEnabled: boolean;

    constructor(propertyName: string, showEnabled: boolean) {
      this.propertyName = propertyName;
      this.byHost = {};
      this.showEnabled = showEnabled;
    }
  }

  class PropByHost {
    host: string;
    prop: ShowPropertyItem;

    constructor(host: string, prop: ShowPropertyItem) {
      this.host = host;
      this.prop = prop;
    }
  }
</script>

<style scoped>
  .property-content {
    width: 700px;
  }

  .property-content-table {
    width: 100%;
  }
</style>
