import axios, {redirectOn401} from '@/utils/axios';
import uri from '@/utils/uri';

class Deploy{
    constructor({...deploy}) {
      Object.assign(this, deploy);
      this.axios = axios.create({
        // baseURL: uri`applications/${this.name}/`,
      });
      this.axios.interceptors.response.use(response => response, redirectOn401()
      );
    }
    async fetchDeploy() {
        return this.axios.get(uri`deploy`);
    }

    async queryServers() {
        return this.axios.get(uri`deploy/server`);
    }

    async doShutdown(deployId) {
        return this.axios.post(uri`deploy/shutdown/${deployId}`);
    }

    async doBuild(deployId) {
        return this.axios.post(uri`deploy/build/${deployId}`);
    }

    async queryDetail(deployId) {
        return this.axios.get(uri`deploy/detail/${deployId}`);
    }

    async queryBuildLog(deployId) {
        return this.axios.get(uri`deploy/log/${deployId}`);
    }

    async stopBuild(deployId) {
        return this.axios.get(uri`deploy/stop/${deployId}`);
    }

    async doAddService(deployRequest) {
        return this.axios.post(uri`deploy/service`, deployRequest);
    }

    async doAddDeployServer(deployServerRequest) {
        return this.axios.post(uri`deploy/server`, deployServerRequest);
    }
}


export default Deploy;