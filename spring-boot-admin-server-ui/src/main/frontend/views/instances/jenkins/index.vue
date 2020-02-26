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
<div>{{projectName}}<button @click="startBuild">构建</button></div> 
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
        projectName: '',
        host: '',
        user: '',
        password: '',
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
          this.projectName = res.data.projectName;
          this.host = res.data.host;
          this.user = res.data.user;
          this.password = res.data.password;
        } catch (error) {
          console.warn('Fetching caches failed:', error);
          this.error = error;
        }
        this.isLoading = false;
      },
      startBuild() {
        const myAxios = axios.create({ 
          headers: {'Accept': 'application/json'} 
        });
        myAxios.post('/buildJenkins', { 
          projectName: this.projectName,
          host: this.host,
          user: this.user,
          password: this.password,
        });
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
