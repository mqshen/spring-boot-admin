<!--
  - Copyright 2014-2019 the original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div>
    <span>{{ jenkins.projectName }}</span>
    <div v-if="isBuilding">构建中... {{Math.floor(duration / estimatedDuration * 100)}}%</div> 
    <div v-else-if="queued">等待构建</div> 
    <div v-else>
      <button @click="startBuild">构建</button>
    </div> 
  </div> 
</template>

<script>
  import axios from '@/utils/axios';
  import Application from '@/services/application';
  import Instance from '@/services/instance';
  import {VIEW_GROUP} from '../../index';

  export default {
    props: {
      instance: {
        type: Instance,
        required: true
      },
      application: {
        type: Application,
        required: true
      }
    },
    data: function () {
      return {
        jenkins: {projectName: ''},
        isBuilding: false,
        queued: false,
        duration: 0,
        estimatedDuration: 1,
        axios: axios.create({ headers: {'Accept': 'application/json'} }),
      };
    },
    computed: {
      service() {
        return this.scope === 'instance'
          ? new InstanceLoggers(this.instance)
          : new ApplicationLoggers(this.application);
      }
    },
    methods: {
      async fetchJenkins() {
        this.error = null;
        this.isLoading = true;
        try {
          const res = await this.instance.fetchJenkins();
          this.jenkins.projectName = res.data.projectName;
          this.jenkins.host = res.data.host;
          this.jenkins.user = res.data.user;
          this.jenkins.password = res.data.password;
          this.jenkins.args = res.data.args;
          this.getBuildStatus();
        } catch (error) {
          console.warn('Fetching caches failed:', error);
          this.error = error;
        }
        this.isLoading = false;
      },
      getBuildStatus() {
        this.axios.post('/jenkins/detail', this.jenkins).then(res => {
          if(res.data) {
            this.isBuilding = res.data.building;
            this.queued = res.data.queued;
            if(res.data.estimatedDuration > 0) {
              this.duration = res.data.duration;
              this.estimatedDuration = res.data.estimatedDuration;
            }
            if(this.isBuilding || this.queued)
              setTimeout(() => this.getBuildStatus(), 3000);
          }
        });
      },
      startBuild() {
        this.queued = true;
        this.axios.post('/jenkins/build', this.jenkins);
        setTimeout(() => this.getBuildStatus(), 3000);
      }
    },
    created() {
      this.fetchJenkins();
    },
    install({viewRegistry}) {
      viewRegistry.addView({
        name: 'instances/jenkins',
        parent: 'instances',
        path: 'jenkins',
        label: 'instances.jenkins.label',
        component: this,
        group: VIEW_GROUP.JENKINS,
        order: 300,
        isEnabled: ({instance}) => instance.hasEndpoint('jenkins')
      });
    }
  }
</script>
