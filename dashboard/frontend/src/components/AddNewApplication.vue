<template>
  <div>
    <div class="row">
      <div class="col">
        <h4>Create application</h4>
      </div>
    </div>
    <div class="row">
      <div class="col-4">
        <form @submit="submitApp" novalidate ref="appForm">
          <div class="form-group">
            <label for="appName">Application name</label>
            <input class="form-control" id="appName" pattern="[A-Za-z-]+" required type="text" v-model="appName">
            <div class="invalid-feedback">
              Invalid application name, patterm = [A-Za-z-]+
            </div>
          </div>
          <div class="form-group">
            <input class="btn btn-primary" type="submit" value="Create"/>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Vue} from 'vue-property-decorator'
  import {applicationService} from "@/service/services";

  @Component
  export default class AddNewApplication extends Vue {

    appName: string | null = null;

    submitApp(e: any) {
      let form = this.$refs['appForm'] as HTMLFormElement;
      let isValid = form.checkValidity();
      form.classList.add('was-validated');

      if (isValid) {
        applicationService.createApplication(this.appName!)
          .then(() => {
            this.$router.push("/applications");
          });
      } else {
        e.preventDefault();
      }
    }
  }
</script>

<style scoped>

</style>
