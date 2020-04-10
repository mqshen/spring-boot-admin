<template>
  <div>
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <a-col :md="7" :sm="24">
            <a-form-item label="环境">
              <a-select v-model="queryParam.eve" placeholder="请选择" default-value="0" class="ifp-selector">
                <a-select-option value="0"> 全部 </a-select-option>
                <a-select-option v-for="environment in environments" :key="environment.id" :value="environment.id">{{ environment.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="7" :sm="24">
            <a-form-item label="应用ID">
              <a-input v-model="queryParam.service" placeholder="输入应用ID关键字" />
            </a-form-item>
          </a-col>
          <a-col :md="7" :sm="24">
            <a-form-item label="分组">
              <a-select v-model="queryParam.group" placeholder="请选择" default-value="0">
                <a-select-option value="0"> 全部 </a-select-option>
                <a-select-option v-for="group in groups" :key="group.id" :value="group.id">{{ group.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="3" :sm="12">
            <span class="table-page-search-submitButtons">
              <a-button type="primary">
                查询
              </a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <a-table :columns="columns" :dataSource="applications" :childrenColumnName="childName" :rowKey="(record) => record.name || record.id">
      <span slot="status" slot-scope="text, record">
        <template v-if="record.buildInfo && !(record.buildInfo.queued || record.buildInfo.building)">
          {{ record.statusInfo.status }}
        </template>
        <template v-else-if="record.instances">
          {{ record.instances | statusFilter }}
        </template>
        <template v-else>
          部署中 <a-spin/>
        </template>
      </span>
      <span slot="recordInfo" slot-scope="text,record">
        <template v-if="record.operationInfo">
          <span>{{ record.operationInfo.operationType | operationFilter}}</span>
          <p>{{ record.operationInfo.opTime }}</p>
        </template>
      </span>
      <span slot="name" slot-scope="text, record">
        <template v-if="record.instances">
          {{ text }}
        </template>
      </span>

      <span slot="group" slot-scope="text">
        <template v-if="text">
          {{ showDisplay(text, groups) }}
        </template>
      </span>

      <span slot="server" slot-scope="text, record">
        <template v-if="record.instances">
          {{ record.instances.length }}
        </template>
        <template v-else>
          {{ text }}
        </template>
      </span>
      <span slot="action" slot-scope="text, record">
        <template v-if="record.instances">
          <a @click="gotoApplications(record.name)">详情</a>
          <a-divider type="vertical" />
          <a @click="modifyApplication(record)">设置</a>
          <a-divider type="vertical" />
        </template>
        <template v-else>
          <a @click="doBuild(record)">部署</a>
          <a-divider type="vertical" />
        </template>
        <a-dropdown>
          <a class="ant-dropdown-link">
            更多 <a-icon type="down" />
          </a>
          <a-menu slot="overlay">
            <template v-if="record.instances">
              <a-menu-item> <a @click="addServer(record)">新添服务器</a> </a-menu-item>
            </template>
            <template v-else>
              <a-menu-item> <a>上线</a> </a-menu-item>
              <a-menu-item> <a @click="doShutdown(record)">下线</a> </a-menu-item>
              <a-menu-item> <a @click="doRollback(record)">回滚</a> </a-menu-item>
              <a-menu-item> <a @click="doSettings(record)">更新设置</a> </a-menu-item>
              <a-menu-item> <a @click="doRefesh(record)">配置刷新</a> </a-menu-item>
              <a-menu-item> <a @click="showBuildLog(record)">控制台输出</a> </a-menu-item>
            </template>
          </a-menu>
        </a-dropdown>
      </span>
    </a-table>
    <app-modal ref="modal" @ok="handleOk" :deploy="deploy"/>
    <build-log ref="logModal" @ok="handleOk" :deploy="deploy"/>
    <app-set-modal ref="setModal" @ok="handleSOk" :servers="servers" :group="grpup"/>
  </div>
</template>
<script>
import Vue from 'vue'
  import AppModal from './page/AppModal.vue'
  import AppSetModal from './page/AppSetModal.vue'
  import BuildLog from './page/BuildLog.vue'
  import Deploy from '@/services/deploy'

  Vue.filter('operationFilter', function (value) {
    if (value == 0 ) {
      return ''
    } else if (value == 1 ) {
      return '部署'
    } else if (value == 2 ) {
      return '上线'
    } else if (value == 3 ) {
      return '下线'
    } else if (value == 4 ) {
      return '回滚'
    } else if (value == 5 ) {
      return ''
    }
  });
  Vue.filter('statusFilter', function (values) {
    var hasUp = false;
    var hasDown = false;
    var hasUnknown = false;
    for(var i = 0; i < values.length; ++i) {
      const instance = values[i];
      if (instance.statusInfo.status == 'UP')
        hasUp = true;
      if (instance.statusInfo.status == 'UNKNOWN')
        hasUnknown = true;
      if (instance.statusInfo.status == 'DOWN' || instance.statusInfo.status == 'OFFLINE') {
        hasDown = true;
      }
      if (hasUp && (hasDown || hasUnknown))
        break;
    }
    if (hasUp && (hasDown || hasUnknown))
      return '部分健康';
    if (hasUp)
      return 'UP';
    if (hasDown)
      return 'DOWN';
  });
  const columns = [
    {
      title: '应用ID',
      dataIndex: 'name',
      scopedSlots: {customRender: 'name'}
    },
    {
      title: '实例（服务器）',
      dataIndex: 'server',
      scopedSlots: {customRender: 'server'}
    },
    {
      title: '分组',
      dataIndex: 'group',
      scopedSlots: {customRender: 'group'}
    },
    {
      title: '运行状态',
      dataIndex: 'status',
      width: '18%',
      scopedSlots: {customRender: 'status'}
    },
    {
      title: '操作记录',
      dataIndex: 'info',
      width: '18%',
      scopedSlots: {customRender: 'recordInfo'}
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: '18%',
      scopedSlots: { customRender: 'action' }
    }
  ];



  export default {
    props: {
      deploy: {
        type: Deploy,
        default: () => new Deploy(),
      },
      groups: {
        type: Array,
        default: () => [],
      },
      environments: {
        type: Array,
        default: () => [],
      },
      servers: {
        type: Array,
        default: () => [],
      },
      instances: {
        type: Array,
        required: true
      }
    },
    components: {
      AppModal,
      AppSetModal,
      BuildLog
    },
    data() {
      return {
        childName: 'instances',
        applications: [],
        allApplications: [],
        columns,
        queryParam:{
          eve: '',
          service: '',
          group: ''
        }
      }
    },
    mounted() {
      this.fetchDeployInfo();
    },
    methods: {
      handleOk () {

      },
      handleSOk () {

      },
      addEvent () {
        this.$refs.modal.add()
      },
      fetchDeployInfo() {
        this.deploy.fetchDeploy().then((res) => {
          const applications = res.data.map((application) => {
            const instances = application.children.map((id) => {
              return this.instances.find((instance) => {return instance.id == id});
            })
            application.instances = instances;
            return application;
          })
          this.allApplications = applications;
          this.filterApplications();
        });
      },
      filterApplications() {
        const applications = allApplications.filter((application) => {
          if(this.queryParam.service) {
            return application.name.includes(this.queryParam.service);
          }
          return true;
        }).map((application) => {
          const instances = application.instances.filter((instance) => {
            if (this.queryParam.eve) {
              if (instance.environment != this.queryParam.eve)
                return false;
            }
            if (this.queryParam.group) {
              if (instance.group != this.queryParam.group)
                return false;
            }
            return true;
          })
          const app = Object.assign({}, application)
          app.instances = instances;
          return app;
        })
        this.applications = applications;
      },
      gotoApplications(name) {
        this.$router.push({name: 'applications', query: {q: name}});
      },
      modifyApplication(application) {
        this.$refs.modal.edit(application);
      },
      queryBuilding(instance) {
        this.deploy.queryDetail(instance.id).then((res) =>{
          instance.buildInfo = res.data;
          if(instance.buildInfo.queued || instance.buildInfo.building )
            setTimeout(() => this.queryBuilding(instance), 3000);
        });
      },
      addServer(microService) {
        const app = {serviceId: microService.id, serviceName: microService.name}
        this.$refs.setModal.add(app);
      },
      showBuildLog(instance) {
        this.$refs.logModal.show(instance.id);
      },
      doRefesh() {
        alert('配置刷新成功');
      },
      doSettings(instance) {
        const app = Object.assign({id: instance.id,
          serviceName: instance.name,
          serverName: instance.server}, instance);
        this.$refs.setModal.edit(app);
      },
      doShutdown(instance) {
        const shutdownRequest = {'instanceId': instance.sbaId, 'deployInstanceId': instance.id};
        this.deploy.doShutdown(shutdownRequest).then(() => {
          setTimeout(() => this.queryBuilding(instance), 3000);
        });
      },
      doRollback(instance) {
        instance.buildInfo.building = true;
        this.deploy.doRollback(instance.id).then(() =>{
          setTimeout(() => this.queryBuilding(instance), 3000);
        });
      },
      doBuild(instance) {
        instance.buildInfo.building = true;
        this.deploy.doBuild(instance.id)
        // .then(() =>{
        //    setTimeout(() => this.queryBuilding(instance), 3000);
        // });
      }, 
      showDisplay(id, list) {
        const item = list.find((item) => {return item.id == id});
        if (item) 
          return item.name;
        return ''
      }
    }
  }
</script>
<style scoped>
  .appTabDiv >>> .ant-table-body .ant-table-thead .ant-table-row-cell-last {
    text-align: center !important;
  }
</style>
