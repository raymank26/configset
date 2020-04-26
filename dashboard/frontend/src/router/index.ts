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
        // при совпадении пути с шаблоном /user/:id/profile
        // в <router-view> компонента User будет показан UserProfile
        path: 'search',
        component: Search
      },
      {
        // при совпадении пути с шаблоном /user/:id/profile
        // в <router-view> компонента User будет показан UserProfile
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
