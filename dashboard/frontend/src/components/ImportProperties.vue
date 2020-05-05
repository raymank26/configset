<template>
  <div>
    <div class="row">
      <div class="col">
        <h4>Import properties</h4>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <p>To import properties provide them in following format for each line:</p>
        <pre>
&lt;properties&gt;
  &lt;property&gt;
      &lt;host&gt;hostname&lt;/host&gt;
      &lt;name&gt;propertyName&lt;/name&gt;
      &lt;value&gt;propertyValue&lt;/value&gt;
  &lt;/property&gt;
&lt;/properties&gt;
        </pre>
        <p><b>hostName</b>.<b>propertyName</b>.<b>propertyValue</b></p>
      </div>
    </div>
    <div class="row">
      <div class="col-auto">
        <form v-on:submit="submit">
          <div class="form-group">
            <label for="appName">Application name</label>
            <application-select id="appName" v-bind:invalid="appNotFound" v-model="appName"/>
          </div>
          <div class="form-group">
            <label for="content">Configuration XML</label>
            <textarea class="form-control" id="content" v-bind:class="{'is-invalid': contentNotFound || contentIllegalFormat}" v-model="content"></textarea>
          </div>
          <div class="alert alert-danger" v-if="contentIllegalFormat">
            Content has illegal format.
          </div>
          <div class="form-group">
            <input class="form-control btn btn-success" type="submit" value="Import"/>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import {Component, Vue} from 'vue-property-decorator'
  import ApplicationSelect from "@/components/ApplicationSelect.vue";
  import {propertyService, searchService} from "@/service/services";
  import {ImportResult} from "@/service/PropertyService";

  @Component({
    components: {ApplicationSelect}
  })
  export default class ImportProperties extends Vue {

    appName: string | null = null;
    content: string = "";

    appNotFound: boolean = false;
    contentNotFound: boolean = false;
    contentIllegalFormat: boolean = false;

    created() {
      let applicationName = this.$route.query['applicationName'] as string;
      if (applicationName) {
        this.appName = applicationName;
      }
    }

    submit() {
      if (this.appName) {
        this.appNotFound = false;
      } else {
        this.appNotFound = true;
        return;
      }
      if (this.content) {
        this.contentNotFound = false;
      } else {
        this.contentNotFound = true;
        return;
      }
      this.contentIllegalFormat = false;
      propertyService.importConfiguration(this.appName, this.content)
        .then(code => {
          switch (code) {
            case ImportResult.OK:
              searchService.searchPush({
                searchApplicationName: this.appName,
                searchHost: null,
                searchPropertyName: null,
                searchPropertyValue: null
              });
              break;
            case ImportResult.INVALID_FORMAT:
              this.contentIllegalFormat = true;
              break;
          }
        })
    }
  }
</script>

<style scoped>
  #content {
    width: 500px;
    height: 500px;
  }

</style>
