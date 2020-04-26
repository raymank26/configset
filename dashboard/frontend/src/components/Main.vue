<template>
  <div>
    <div class="row">
      <form>
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
            <input class="form-control" placeholder="Host name" type="text">
          </div>
          <div class="col">
            <input class="form-control" placeholder="Property name" type="text">
          </div>
          <div class="col">
            <input class="form-control" placeholder="Property value" type="text">
          </div>
          <div class="col">
            <button class="btn btn-info">Search properties</button>
          </div>
        </div>
      </form>
    </div>
    <div class="row">
      Here2
    </div>

  </div>
</template>

<script lang="ts">
  import {Component, Prop, Vue} from 'vue-property-decorator'

  @Component
  export default class Main extends Vue {
    @Prop() private msg!: string;

    created() {
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
