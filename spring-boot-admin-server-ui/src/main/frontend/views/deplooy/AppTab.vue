<template>
  <div>
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <a-col :md="7" :sm="24">
            <a-form-item label="环境">
              <a-select v-model="queryParam.eve" placeholder="请选择" default-value="0" class="ifp-selector">
                <a-select-option value="0"> 全部 </a-select-option>
                <a-select-option value="1"> POC </a-select-option>
                <a-select-option value="2"> PREVIEW </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="7" :sm="24">
            <a-form-item label="应用ID">
              <a-input v-model="queryParam.server" placeholder="输入应用ID关键字" />
            </a-form-item>
          </a-col>
          <a-col :md="7" :sm="24">
            <a-form-item label="分组">
              <a-select v-model="queryParam.group" placeholder="请选择" default-value="0">
                <a-select-option value="0"> 全部 </a-select-option>
                <a-select-option value="userCenter"> userCenter </a-select-option>
                <a-select-option value="customCenter"> customCenter </a-select-option>
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
    
    <div class="table-operator">
      <a-button type="primary" icon="plus" @click="$refs.modal.add()">
        新建
      </a-button>
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
          部署中
        </template>
      </span>
      <span slot="recordInfo" slot-scope="text,record">
        <template v-if="record.operationInfo">
          <span>{{ record.operationInfo.operationType }}</span>
          <p>{{ record.operationInfo.opTime }}</p>
        </template>
      </span>
      <span slot="name" slot-scope="text, record">
        <template v-if="record.instances">
          {{ text }}
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
          <a>详情</a>
          <a-divider type="vertical" />
          <a @click="modifyApplication(record)">设置</a>
          <a-divider type="vertical" />
        </template>
        <template v-else>
          <a @click="doBuild(record)">部署</a>
          <a-divider type="vertical" />
        <template>
        <a-dropdown>
          <a class="ant-dropdown-link">
            更多 <a-icon type="down" />
          </a>
          <a-menu slot="overlay">
            <template v-if="record.instances">
              <a-menu-item> <a>新添服务器</a> </a-menu-item>
            </template>
            <template v-else>
              <a-menu-item> <a>上线</a> </a-menu-item>
              <a-menu-item> <a>下线</a> </a-menu-item>
              <a-menu-item> <a>回滚</a> </a-menu-item>
              <a-menu-item> <a>更新配置</a> </a-menu-item>
              <a-menu-item> <a>控制台输出</a> </a-menu-item>
            </template>
          </a-menu>
        </a-dropdown>
      </span>
    </a-table>
    <app-modal ref="modal" @ok="handleOk" :deploy="deploy"/>
    <app-set-modal ref="setModal" @ok="handleSOk" />
  </div>
</template>
<script>
import Vue from 'vue'
  import AppModal from './page/AppModal.vue'
  import AppSetModal from './page/AppSetModal.vue'
  import Deploy from '@/services/deploy'
  
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
    },
    components: {
      AppModal,
      AppSetModal
    },
    data() {
      return {
        childName: 'instances',
        applications: [],
        columns,
        queryParam:{
          eve: '',
          server: '',
          group: ''
        }
      }
    },
    mounted() {
      this.deploy.fetchDeploy().then((res) =>{
        this.applications = res.data;
      });
    },
    methods: {
      handleOk () {
        
      },
      handleSOk () {
        
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
      doBuild(instance) {
        instance.buildInfo.building = true;
        this.deploy.doBuild(instance.id).then(() =>{
          setTimeout(() => this.queryBuilding(instance), 3000);
        });
      },
    } 
  }
</script>
