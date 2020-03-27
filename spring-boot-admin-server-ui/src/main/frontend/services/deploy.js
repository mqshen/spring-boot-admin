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

    async doStop(name, server) {
        return this.axios.post(uri`deploy/stop`,
            {name: name, server: server },
        );
    }

    async doBuild(name, server) {
        return this.axios.post(uri`deploy/build`,
            {name: name, server: server },
        );
    }

    async queryDetail(name, server) {
        return this.axios.post(uri`deploy/detail`,
            {name: name, server: server },
        );
    }
}


export default Deploy;