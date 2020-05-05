import {UpdateResult} from "@/service/PropertyService";
<template>
  <div>
    <div class="row">
      <div class="col">
        <h4>Create/Update property</h4>
      </div>
    </div>
    <div v-if="!loading">
      <div class="row">
        <div class="col-4">
          <form @submit="submitApp" novalidate ref="appForm">
            <div class="form-group">
              <label for="appName">Application name</label>
              <select class="custom-select" id="appName" required v-model="appName">
                <option value="">Select application</option>
                <option v-for="app in applications">{{ app }}</option>
              </select>
              <div class="invalid-feedback">
                Application is not selected
              </div>
            </div>
            <div class="form-group">
              <label for="hostName">Host name</label>
              <input class="form-control" id="hostName" pattern="[A-Za-z-0-9.]+" required type="text"
                     v-model="hostName">
              <div class="invalid-feedback">
                Invalid application name, pattern = [A-Za-z-]+
              </div>
            </div>
            <div class="form-group">
              <label for="propertyName">Property name</label>
              <input class="form-control" id="propertyName" pattern="[A-Za-z-0-9.]+" required type="text"
                     v-model="propertyName">
              <div class="invalid-feedback">
                Invalid application name, pattern = [A-Za-z-]+
              </div>
            </div>
            <div class="form-group">
              <label for="propertyValue">Property value</label>
              <textarea class="form-control" id="propertyValue" type="text" v-model="propertyValue"/>
              <div class="invalid-feedback">
                Invalid application name, pattern = [A-Za-z-]+
              </div>
            </div>
            <div class="form-group">
              <input class="btn btn-primary" type="submit" value="Update"/>
            </div>
          </form>
        </div>
      </div>
      <div class="row">
        <div class="col-6">
          <div class="alert alert-danger" v-if="hasConflict">
            The property value has conflict with already saved one. Please update the page to see the update.
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col-6">
          <div class="alert alert-danger" v-if="appNotFound">
            The selected application is not found.
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col-6">
          <div class="alert alert-danger" v-if="propertyNotFound">
            The property is not found.
          </div>
        </div>
      </div>
    </div>
    <div v-else>
      Loading...
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Vue} from 'vue-property-decorator'
  import {applicationService, propertyService, searchService} from "@/service/services";
  import {UpdateResult} from "@/service/PropertyService";

  @Component
  export default class UpdateProperty extends Vue {
    applications: string[] = [];

    appName: string = "";
    propertyName: string = "";
    hostName: string = "";
    propertyValue: string = "";
    _version: number | null = null;

    loading: boolean = true;
    hasConflict: boolean = false;
    appNotFound: boolean = false;
    propertyNotFound: boolean = false;

    created() {
      if (Object.keys(this.$router.currentRoute.query).length !== 0) {
        let query = this.$router.currentRoute.query as any;
        this.appName = query.applicationName;
        this.propertyName = query.propertyName;
        this.hostName = query.hostName;
        this.applications = [this.appName];

        if (this.appName && this.hostName && this.propertyName) {
          propertyService.readProperty(this.appName, this.hostName, this.propertyName).then(result => {
            if (result.property) {
              this.propertyValue = result.property.propertyValue;
              this._version = result.property.version;
            } else {
              this.propertyNotFound = true;
            }
            this.loading = false;
          });
        } else {
          this.loading = false;
        }
      } else {
        applicationService.listApplications().then(apps => {
          this.applications = apps;
          this.loading = false;
        });
      }
    }

    submitApp() {
      if (this.hasConflict) {
        return;
      }
      propertyService.updateProperty(this.appName, this.hostName, this.propertyName, this.propertyValue, this._version)
        .then(updateResult => {
          switch (updateResult) {
            case UpdateResult.OK:
              searchService.searchPush({
                searchApplicationName: this.appName,
                searchHost: this.hostName,
                searchPropertyName: this.propertyName,
                searchPropertyValue: null
              });
              break;
            case UpdateResult.CONFLICT:
              this.hasConflict = true;
              break;
            case UpdateResult.APPLICATION_NOT_FOUND:
              this.appNotFound = true;
              break;
          }
        });
    }
  }
</script>

<style scoped>

</style>
