<template>
  <section class="section">
    <div class="container">
        <div class="applications-list">
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
                                <td width="40%" class="instance-list-item__actions"><!----></td>
                            </tr>
                        </tbody>
                    </table>
                </header>
                <div class="card-content">
                    <table class="table is-hoverable is-selectable is-fullwidth instances-list">
                        <tbody>
                            <tr v-for="(instance, index) in deployApplication.instances" :key="index">
                                <td width="5%"> <!--  --> </td>
                                <td width="35%">{{instance.statusInfo.status}}</td>
                                <td width="20%">
                                    <span>{{instance.server}}</span>
                                </td>
                                <td> {{instance.buildInfo.timestamp}} </td>
                                <td width="30%">
                                  <template v-if="!(instance.buildInfo.queued || instance.buildInfo.building)">
                                    <button class="button is-light" @click="doStop(deployApplication, index)">下线</button>
                                    <button class="button is-light" @click="doBuild(deployApplication, index)">部署</button>
                                  </template>
                                  <template v-else>
                                    部署中
                                  </template>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
  </section>
</template>

<script>
  import Deploy from '@/services/deploy'
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
        isBuilding: false,
        queued: false,
        duration: 0,
        estimatedDuration: 1,
        deploy: new Deploy(),
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
              if(instance.buildInfo.queued || instance.buildInfo.isBuilding ) 
                setTimeout(() => this.queryBuilding(instance, application.name), 3000);
            });
          })
        } catch (error) {
          console.warn('Fetching caches failed:', error);
          this.error = error;
        }
        this.isLoading = false;
      },
      async doStop(deployApplication, index) {
          const name = deployApplication.name;
          const server = deployApplication.instances[index].server
          const res = await this.deploy.doStop(name, server);
      },
      queryBuilding(instance, name) {
        this.deploy.queryDetail(name, instance.server).then((res) =>{
          instance.buildInfo = res.data;
          if(instance.buildInfo.queued || instance.buildInfo.isBuilding )
            setTimeout(() => this.queryBuilding(instance, name), 3000);
        });
      },
      async doBuild(deployApplication, index) {
          const name = deployApplication.name;
          const instance = deployApplication.instances[index];
          const server = instance.server
          instance.buildInfo.isBuilding = true;
          const res = await this.deploy.doBuild(name, server);
          setTimeout(() => this.queryBuilding(instance, name), 3000);
      },
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