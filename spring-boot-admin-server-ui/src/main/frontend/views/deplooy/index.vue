<template>
  <section class="section">
    <div class="container">
        <div class="applications-list">
          <button class="button is-light" @click="showServiceModal = true">添加微服务</button>
            <div class="application-list-item card" :class="{'is-active': isExpanded}" v-for="deployApplication in deployApplications" :key="deployApplication.name">
                <header class="hero "  v-on="$listeners">
                    <table class="table is-hoverable is-selectable is-fullwidth instances-list">
                        <tbody>
                            <tr>
                                <td width="20%">
                                    <h1 class="title is-size-5">{{deployApplication.name}}</h1>
                                </td>
                                <td width="20%">{{deployApplication.status}}</td>
                                <td width="20%">
                                    <span>{{deployApplication.instances.length}}</span>
                                </td>
                                <td width="10%">
                                </td>
                                <td width="30%"><button class="button is-light" @click="startAddDeploy(deployApplication)">添加</button></td>
                            </tr>
                        </tbody>
                    </table>
                </header>
                <div class="card-content">
                    <table class="table is-hoverable is-selectable is-fullwidth instances-list">
                        <tbody>
                            <tr v-for="instance in deployApplication.instances" :key="instance.id">
                                <td width="5%"> <!--  --> </td>
                                <td width="35%">{{instance.statusInfo.status}}</td>
                                <td width="20%">
                                    <span>{{instance.server}}</span>
                                </td>
                                <td> {{instance.buildInfo.timestamp | dataTimeFormat}} </td>
                                <td width="30%">
                                  <template v-if="!(instance.buildInfo.queued || instance.buildInfo.building)">
                                    <button class="button is-light" @click="doShutdown(instance)">下线</button>
                                    <button class="button is-light" @click="doBuild(instance)">部署</button>
                                  </template>
                                  <template v-else>
                                    部署中
                                    <button class="button is-light" @click="stopBuild(instance)">停止部署</button>
                                    <button class="button is-light" @click="showBuildLog(instance)">控制台输出</button>
                                  </template>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <div class="modal" :class="{'is-active': showServiceModal}" >
      <div class="modal-background" @click="showServiceModal = false" />
      <div class="modal-content">
        <div class="modal-card">
          <input type="text" class="form-input" v-model="name" placeholder="微服务名字"/>
          <input type="text" class="form-input" v-model="jobName" placeholder="Jekins工程名"/>
          <input type="text" class="form-input" v-model="projectName" placeholder="子工程名字"/>
          <button class="button is-light" @click="doAddService()">添加</button>
        </div>
      </div>
    </div>

    <div class="modal" :class="{'is-active': showDeployModal}" >
      <div class="modal-background" @click="showDeployModal = false" />
      <div class="modal-content">
        <div class="modal-card">
          <input type="text" class="form-input" v-model="host" placeholder="要部署的服务器"/>
          <button class="button is-light" @click="doAddDeployServer()">添加</button>
        </div>
      </div>
    </div>

    <div class="modal" :class="{'is-active': showLog}" >
      <div class="modal-background" @click="hideBuildLog" />
      <div class="modal-content">
        <div class="modal-card">
        {{ buildLog }}
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.form-input {
    line-height: 22px;
    margin: 10px;
    padding: 10px;
    -webkit-appearance: none;
    border: 0;
}
</style>
<script>
import Vue from 'vue'
import Deploy from '@/services/deploy'
import moment from 'moment';

Vue.filter('dataTimeFormat', function (value) {
  return moment(value).format('YYYY-MM-DD HH:mm:ss')
})

  export default {
    props: {
      deployApplications: {
        type: Array,
        default: () => [],
      }
    },
    data: function () {
      return {
        jenkins: {projectName: ''},
        queued: false,
        duration: 0,
        showLog: false,
        buildLog: '',
        estimatedDuration: 1,
        deploy: new Deploy(),
        showServiceModal: false,
        showDeployModal: false,
        name: '',
        jobName: '',
        projectName: '',
        serverId: 0,
        host: ''
      };
    },
    methods: {
      async fetchDeployInfo() {
        this.error = null;
        this.isLoading = true;
        try {
          const res = await this.deploy.fetchDeploy();
          this.deployApplications = res.data
          this.deployApplications.map((application) => {
            application.instances.map((instance) => {
              if(instance.buildInfo.queued || instance.buildInfo.building ) 
                setTimeout(() => this.queryBuilding(instance, application.name), 3000);
            });
          })
        } catch (error) {
          console.warn('Fetching caches failed:', error);
          this.error = error;
        }
        this.isLoading = false;
      },
      async doShutdown(instance) {
          const res = await this.deploy.doShutdown(instance.id);
      },
      hideBuildLog() {
        this.showLog = false;
      },
      showBuildLog(instance) {
        this.showLog = true;
        this.deploy.queryBuildLog(instance.id).then((res) => {
          this.buildLog = res.data;
        });
      },
      stopBuild(instance) {
        this.deploy.stopBuild(instance.id).then((res) =>{
        });
      },
      queryBuilding(instance) {
        this.deploy.queryDetail(instance.id).then((res) =>{
          instance.buildInfo = res.data;
          if(instance.buildInfo.queued || instance.buildInfo.building )
            setTimeout(() => this.queryBuilding(instance), 3000);
        });
      },
      async doBuild(instance) {
          instance.buildInfo.building = true;
          const res = await this.deploy.doBuild(instance.id);
          setTimeout(() => this.queryBuilding(instance), 3000);
      },
      doAddService() {
        const deployRequest = {"name": this.name, "jobName": this.jobName, "projectName": this.projectName}
        this.deploy.doAddService(deployRequest).then((res) => {
          this.fetchDeployInfo();
          this.showServiceModal = false;
        });
      },
      startAddDeploy(deployApplication) {
        this.serviceId = deployApplication.id;
        this.showDeployModal = true;
      },
      doAddDeployServer() {
        const deployServerRequest = {"serviceId": this.serviceId, "host": this.host}
        this.deploy.doAddDeployServer(deployServerRequest).then((res) => {
          this.fetchDeployInfo();
          this.showDeployModal = false;
        });
      }
    },
    created() {
      this.fetchDeployInfo();
    },
    computed: {
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