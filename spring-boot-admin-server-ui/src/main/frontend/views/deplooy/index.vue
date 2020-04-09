<template>
  <a-card
    :bordered="false"
    :tabList="tabList"
    :activeTabKey="titleKey"
    @tabChange="key => handleTabChange(key, 'titleKey')"
  >
    <a-button type="primary" icon="plus" slot="tabBarExtraContent" @click="handleAdd()">
      {{ titleKey |changeTitle }}
    </a-button>
    <app-tab v-if="titleKey === 'app'" :servers="servers" :deploy="deploy" ref="app"></app-tab>
    <server-tab v-else ref="ser"></server-tab>
  </a-card>
</template>

<script>
import './common.js'
import 'ant-design-vue/dist/antd.css';
import AppTab from './AppTab'
import ServerTab from './ServerTab'
import Deploy from '@/services/deploy'

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
      titleKey: 'app',
      servers: [],
      deploy: new Deploy()
    }
  },
  mounted() {
    this.deploy.listServers().then((res) => {
      this.servers = res.data;
    });
  },
  filters: {
    changeTitle (key) {
      const keyMap = {
        'app': '新建应用',
        'server': '新建服务器'
      }
      return keyMap[key]
    }
  },
  methods: {
    handleTabChange (key, type) {
      this[type] = key
    },
    handleAdd () {
      if (this.titleKey === 'app') {
        this.$refs.app.addEvent()
      } else {
        this.$refs.ser.addEvent()
      }
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
.table-page-search-wrapper .ant-form-inline .ant-form-item{
  display: flex;
  margin-bottom: 24px;
  margin-right: 0;
}
.table-page-search-wrapper .ant-form-inline .ant-form-item>.ant-form-item-label {
    line-height: 32px;
    padding-right: 8px;
    width: auto;
}
.table-page-search-wrapper .ant-form-inline .ant-form-item .ant-form-item-control-wrapper {
    -webkit-box-flex: 1;
    -ms-flex: 1 1;
    flex: .6 1;
    display: inline-block;
    vertical-align: middle;
}
</style>
