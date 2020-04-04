<template>
  <a-card
    :bordered="false"
    :tabList="tabList"
    :activeTabKey="titleKey"
    @tabChange="key => handleTabChange(key, 'titleKey')"
  >
    <app-tab v-if="titleKey === 'app'"></app-tab>
    <server-tab v-else></server-tab>
  </a-card>
</template>

<script>
import './common.js'
import 'ant-design-vue/dist/antd.css';
import AppTab from './AppTab'
import ServerTab from './ServerTab'

export default {
  name: 'deploy',
  components: {
    AppTab,
    ServerTab
  },
  data () {
    return {
      tabList: [
        {
          key: 'app',
          tab: '应用'
        },
        {
          key: 'server',
          tab: '服务器'
        }
      ],
      titleKey: 'app'
    }
  },
  methods: {
    handleTabChange (key, type) {
      this[type] = key
    }
  },
  install({viewRegistry}) {
    viewRegistry.addView({
      path: '/deploy',
      name: 'deploy',
      label: 'deploy.label',
      order: 300,
      component: this
    });
  }
}
</script>

<style >
.ifp-selector {
  width: 100px;
}
</style>
