import Vue from 'vue'
import App from './App.vue'
import router from './router'
import 'bootstrap'
import 'bootstrap/dist/css/bootstrap.min.css'
import Axios from "axios";
import {ClientConfig, getConfig, setConfig} from "@/config";
import Keycloak from "keycloak-js";

Vue.config.productionTip = false;

let loadPromise = Axios.get("/api/config").then(clientConfig => {
  setConfig(clientConfig.data as ClientConfig)
});

loadPromise = loadPromise.then(() => {
  let config = getConfig()
  let initOptions = {
    url: config.keycloackUrl + '/auth', realm: config.keycloackRealm, clientId: config.keycloackClientId, onLoad:'login-required'
  }
  let keycloak = Keycloak(initOptions);
  return keycloak.init({onLoad: initOptions.onLoad}).then((authenticated: boolean) => {
    if (!authenticated) {
      window.location.reload();
      return;
    }
    localStorage.setItem("vue-token", keycloak.token!!);
    localStorage.setItem("vue-refresh-token", keycloak.refreshToken!!);

    setInterval(() => {
      keycloak.updateToken(70)
    }, 60000);
  }).catch(() => {
    alert('failed to initialize');
  });
})


loadPromise.then(() => {
  new Vue({
    router,
    render: h => h(App)
  }).$mount('#app');
})
