<template>
  <div>
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <a-col :md="7" :sm="24">
            <a-form-item label="环境">
              <a-select v-model="queryParam.eve" placeholder="请选择" default-value="0" class="ifp-selector">
                <a-select-option :value="-1"> 全部 </a-select-option>
                <a-select-option v-for="environment in environments" :key="environment.id" :value="environment.id">{{ environment.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="7" :sm="24">
            <a-form-item label="数据中心">
              <a-select v-model="queryParam.group" placeholder="请选择" default-value="0">
                <a-select-option :value="-1"> 全部 </a-select-option>
                <a-select-option value="userCenter"> userCenter </a-select-option>
                <a-select-option value="customCenter"> customCenter </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :md="3" :sm="12">
            <span class="table-page-search-submitButtons">
              <a-button type="primary" @click="filterServer">
                查询
              </a-button>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <a-table :columns="columns" :dataSource="serverInstances" :childrenColumnName="childName" :rowKey="(record) => record.url || record.id">
      <span slot="status" slot-scope="text, record">
        <template v-if="record.buildInfo && !(record.buildInfo.queued || record.buildInfo.building)">
          {{ record.statusInfo.status }}
        </template>
        <template v-else>
        </template>
      </span>
      <span slot="name" slot-scope="text, record">
        <template v-if="record.instances">
          {{ text }}
        </template>
      </span>
      <span slot="instance" slot-scope="text, record">
        <template v-if="record.instances">
          {{ record.instances.length }}
        </template>
        <template v-else>
          {{ text }}
        </template>
      </span>
      <span slot="ip" slot-scope="text, record">
        <template v-if="text">
          {{ text }}
        </template>
        <template v-else>
          {{ record.url }}
        </template>
      </span>
      <span slot="recordInfo" slot-scope="text">
        <template v-if="text">
          {{ showDisplay(text, environments) }}
        </template>
      </span>
      <span slot="action" slot-scope="text, record">
        <template v-if="record.instances">
          <a @click="doTest(record)">检测</a>
          <a-divider type="vertical" />
          <a @click="modifyServer(record)">设置</a>
        </template>
      </span>
    </a-table>
    <server-modal ref="modal" :deploy="deploy"/>
  </div>
</template>
<script>
  import ServerModal from './page/ServerModal.vue'
  import Deploy from '@/services/deploy'
  const columns = [
    {
      title: '服务器',
      dataIndex: 'name',
      scopedSlots: {customRender: 'name'}
    },
    {
      title: '应用',
      dataIndex: 'name',
      scopedSlots: {customRender: 'instance'}
    },
    {
      title: 'IP/URL',
      dataIndex: 'ip',
      scopedSlots: {customRender: 'ip'},
      width: '20%',
    },
    {
      title: '运行状态',
      dataIndex: 'status',
      width: '15%',
      scopedSlots: {customRender: 'status'}
    },
    {
      title: '环境',
      dataIndex: 'environmentId',
      width: '10%',
      scopedSlots: {customRender: 'recordInfo'}
    },
    {
      title: '操作',
      dataIndex: 'action',
      width: '10%',
      scopedSlots: { customRender: 'action' }
    }
  ];

  export default {
    components: {
      ServerModal
    },
    props: {
      deploy: {
        type: Deploy,
        required: true
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
    data() {
      return {
        childName: 'instances',
        serverInstances: [],
        allServers: [],
        columns,
        queryParam:{
          eve: -1,
          server: '',
          group: -1
        }
      }
    },
    mounted() {
      this.deploy.queryServers().then((res) =>{
        const servers = res.data.map((server) => {
          const instances = server.children.map((id) => {
            return this.instances.find((instance) => {return instance.id == id});
          })
          server.instances = instances;
          return server;
        })
        this.allServers = servers;
        this.filterServer();
      });
    },
    methods: {
      handleOk() {

      },
      addEvent () {
        this.$refs.modal.add()
      },
      doTest(record) {
        window.console.log(record);
      },
      modifyServer(record) {
        this.$refs.modal.edit(record);
      },
      showDisplay(id, list) {
        const item = list.find((item) => {return item.id == id});
        if (item) 
          return item.name;
        return ''
      },
      filterServer() {
        const servers = this.allServers.filter((server) => {
          if(this.queryParam.eve && this.queryParam.eve > 0) {
            return server.environmentId == this.queryParam.eve;
          }
          return true;
        });
        this.serverInstances = servers;
      },
    }
  }
</script>
