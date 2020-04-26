import Vue from 'vue'
import VueRouter, {RouteConfig} from 'vue-router'
import Dashboard from '@/components/Dashboard.vue'
import Search from "@/components/Search.vue";
import Applications from "@/components/Applications.vue";


Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
  {
    path: '/',
    redirect: '/search'
  },
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard,
    children: [
      {
        path: 'search',
        component: Search,
        props: (route => ({
          propApplicationName: route.query['applicationName'] as string,
          propSearchHost: route.query["host"] as string,
          propSearchPropertyName: route.query["propertyName"] as string,
          propSearchPropertyValue: route.query["propertyValue"] as string
        }))
      },
      {
        path: 'applications',
        component: Applications
      },
    ]
  },
];

const router = new VueRouter({
  routes,
  linkActiveClass: "active"
});

export default router
