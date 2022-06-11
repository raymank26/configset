import Vue from 'vue'
import App from './App.vue'
import router from './router'
import {BootstrapVue} from 'bootstrap-vue'
import 'bootstrap'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import Axios from "axios";
import {ClientConfig, getConfig, setConfig} from "@/config";
import Keycloak from "keycloak-js";
import {axiosApiInstance} from "@/Axios";
import Toasts from "@/plugins/Toasts";

Vue.config.productionTip = false;
Vue.use(BootstrapVue)
Vue.use(Toasts)

let loadPromise = Axios.get("/api/config").then(clientConfig => {
  setConfig(clientConfig.data as ClientConfig)
});

loadPromise = loadPromise.then(() => {
  let config = getConfig()
  let initOptions = {
    url: config.keycloackUrl + '/auth',
    realm: config.keycloackRealm,
    clientId: config.keycloackClientId,
    onLoad: 'login-required' as const
  }
  let keycloak = Keycloak(initOptions);

  return keycloak
    .init({onLoad: initOptions.onLoad})
    .then((authenticated: boolean) => {
      if (!authenticated) {
        window.location.reload();
        return;
      }
      localStorage.setItem("vue-token", keycloak.token!!);
      localStorage.setItem("vue-refresh-token", keycloak.refreshToken!!);
      Vue.prototype.$roles = keycloak.tokenParsed?.realm_access?.roles || [];

      setInterval(() => {
        keycloak.updateToken(70)
      }, 60000);

      axiosApiInstance.interceptors.request.use(
        async config => {
          let token = keycloak.token;
          config.headers = {
            Authorization: `Bearer ${token}`,
          }
          return config
        },
        error => {
          Promise.reject(error)
        },
      )
    }).catch(() => {
      alert('failed to initialize');
    });
})


loadPromise.then(() => {
  let app = new Vue({
    router,
    render: h => h(App)
  }).$mount('#app');

  axiosApiInstance.interceptors.response.use((response: any) => {
    return response
  }, (error: any) => {
    app.$showError!!(error.toString())
    return Promise.reject(error)
  })
})
