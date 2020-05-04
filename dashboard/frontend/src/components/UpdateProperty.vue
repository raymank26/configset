<template>
  <div>
    <div class="row">
      <div class="col">
        <h4>Create/Update property</h4>
      </div>
    </div>
    <div class="row" v-if="!loading">
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
            <input class="form-control" id="hostName" pattern="[A-Za-z-0-9.]+" required type="text" v-model="hostName">
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
    <div v-else>
      Loading...
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Vue} from 'vue-property-decorator'
  import {applicationService, propertyService} from "@/service/services";
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

    created() {
      if (this.$router.currentRoute.query) {
        let query = this.$router.currentRoute.query as object;
        this.appName = query.applicationName;
        this.propertyName = query.propertyName;
        this.hostName = query.hostName;
        this.applications = [this.appName];

        propertyService.readProperty(this.appName, this.hostName, this.propertyName).then(property => {
          this.propertyValue = property.propertyValue;
          this._version = property.version;
          this.loading = false;
        });
      } else {
        applicationService.listApplications().then(apps => {
          this.applications = apps;
          this.loading = false;
        });
      }
    }

    submitApp(e: any) {
      let form = this.$refs['appForm'] as HTMLFormElement;
      let isValid = form.checkValidity();
      form.classList.add('was-validated');

      if (isValid) {
        propertyService.updateProperty(this.appName, this.hostName, this.propertyName, this.propertyValue, this._version)
          .then(updateResult => {
            switch (updateResult) {
              case UpdateResult.OK:
                this.$router.push("/search");
                break;
              case UpdateResult.CONFLICT:
                alert("conflict");
                break;
            }
          });
      }
    }
  }
</script>

<style scoped>

</style>
