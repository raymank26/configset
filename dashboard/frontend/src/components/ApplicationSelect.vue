<template>
  <select class="custom-select" id="appName" v-bind:class="{'is-invalid': invalid}" v-model="appName" v-on:change="$emit('input', appName)">
    <option disabled value="">Select an application</option>
    <option v-for="app in applications">{{ app }}</option>
  </select>
</template>

<script lang="ts">
  import {Component, Prop, Vue} from 'vue-property-decorator'

  import {applicationService} from "@/service/services";

  @Component
  export default class ApplicationSelect extends Vue {

    applications: string[] = [];
    appName: string | null = null;

    @Prop()
    value?: string | null;

    @Prop()
    invalid!: boolean;

    @Prop()
    alwaysShowOptions!: boolean;

    created() {
      console.log(this.alwaysShowOptions);
      if (this.value && !this.alwaysShowOptions) {
        this.appName = this.value;
        this.applications = [this.value];
      } else {
        this.appName = this.value || "";
        applicationService.listApplications().then(apps => {
          this.applications = apps;
        });
      }
    }
  }
</script>

<style scoped>

</style>
