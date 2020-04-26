<template>
  <div>
    <div class="row">
      <div class="col">
        <h4>Create/Update property</h4>
      </div>
    </div>
    <div class="row">
      <div class="col-4">
        <form @submit="submitApp" novalidate ref="appForm">
          <div class="form-group">
            <label for="appName">Application name</label>
            <select class="custom-select" id="appName" required v-model="appName">
              <option selected value="">Select application</option>
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
  </div>
</template>

<script lang="ts">
  import {Component, Vue} from 'vue-property-decorator'
  import {applicationService, propertyService} from "@/service/services";

  @Component
  export default class UpdateProperty extends Vue {
    applications: string[] = [];

    appName: string = "";
    propertyName: string = "";
    hostName: string = "";
    propertyValue: string = "";

    created() {
      applicationService.listApplications().then(apps => {
        this.applications = apps;
      });
    }

    submitApp(e: any) {
      let form = this.$refs['appForm'] as HTMLFormElement;
      let isValid = form.checkValidity();
      form.classList.add('was-validated');

      if (isValid) {
        propertyService.updateProperty(this.appName, this.hostName, this.propertyName, this.propertyValue, null)
          .then(() =>
            this.$router.push("/search")
          )
      }
    }
  }
</script>

<style scoped>

</style>
