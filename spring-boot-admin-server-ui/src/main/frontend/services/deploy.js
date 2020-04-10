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

    async listServers() {
        return this.axios.get(uri`deploy/list/servers`);
    }

    async listEnvironments() {
        return this.axios.get(uri`deploy/environments`);
    }

    async listGroups() {
        return this.axios.get(uri`deploy/groups`);
    }

    async listInstacne() {
        return this.axios.get(uri`deploy/list/instances`);
    }

    async queryServers() {
        return this.axios.get(uri`deploy/server`);
    }

    async doShutdown(shutdownRequest) {
        return this.axios.post(uri`deploy/shutdown`, shutdownRequest);
    }

    async doBuild(deployId) {
        return this.axios.post(uri`deploy/build/${deployId}`);
    }

    async doRollback(deployId) {
        return this.axios.post(uri`deploy/rollback/${deployId}`);
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

    async doAddDeployInstance(instanceRequest) {
        return this.axios.post(uri`deploy/instance`, instanceRequest);
    }

    async doAddDeployServer(deployServerRequest) {
        return this.axios.post(uri`deploy/server`, deployServerRequest);
    }
}


export default Deploy;