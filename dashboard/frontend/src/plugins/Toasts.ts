import {Vue as _Vue} from "vue/types/vue";


export default {
  install(Vue: typeof _Vue, options?: any): void {
    Vue.prototype.$showError = function (this: any, msg: string) {
      this.$bvToast.toast(msg, {
        title: "Warning",
        variant: "danger",
        toaster: "b-toaster-bottom-right"
      })
    }
  }
}

