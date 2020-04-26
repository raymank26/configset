<template>
  <div>
    <div class="row mb-2">
      <div class="col">
        <router-link class="btn btn-primary" to="applications/add">Add new application</router-link>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <div class="row">
          <div class="col">
            <h3>Applications</h3>
          </div>
        </div>
        <div class="row">
          <div class="col-4">
            <div v-if="applications.length === 0">
              No applications created yet
            </div>
            <div v-else>
              <table class="table table-striped">
                <thead>
                <tr>
                  <th scope="col">Name</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="app in applications">
                  <td>{{ app }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Vue} from 'vue-property-decorator'
  import AddNewApplication from "@/components/AddNewApplication.vue";
  import {applicationService} from "@/service/services";

  @Component({
    components: {AddNewApplication}
  })
  export default class Applications extends Vue {

    private applications: string[] = [];

    created() {
      applicationService.listApplication().then(apps => {
        this.applications = apps
      });
    }
  }
</script>

<style scoped>

</style>
