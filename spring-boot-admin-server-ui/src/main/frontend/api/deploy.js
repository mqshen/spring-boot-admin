import { axios } from './request'
import uri from './uri'

export function fetchDeploy () {
  return axios.get(uri`deploy`)
}
export function listServers () {
  return axios.get(uri`deploy/list/servers`)
}

export function listEnvironments () {
  return axios.get(uri`deploy/environments`)
}

export function listGroups () {
  return axios.get(uri`deploy/groups`)
}

export function listInstacne () {
  return axios.get(uri`deploy/list/instances`)
}

export function queryServers () {
  return axios.get(uri`deploy/server`)
}

export function doShutdown (shutdownRequest) {
  return axios.post(uri`deploy/shutdown`, { instances: shutdownRequest })
}

export function doStart (deployArray) {
  return axios.post(uri`deploy/start`, { instances: deployArray })
}

export function doBuild (deployId) {
  if (deployId instanceof Array) {
    return axios.post(uri`deploy/buildAll`, { instances: deployId })
  } else {
    return axios.post(uri`deploy/build/${deployId}`)
  }
}

export function doRollback (deployId) {
  return axios.post(uri`deploy/rollback/${deployId}`)
}

// export function queryDetail(deployId) {
//     return axios.get(`deploy/detail/${deployId}`);
// }

export function queryBuildLog (deployId) {
  return axios.get(uri`deploy/log/${deployId}`)
}

export function stopBuild (deployId) {
  return axios.get(uri`deploy/stop/${deployId}`)
}

export function doAddService (deployRequest) {
  return axios.post(uri`deploy/service`, deployRequest)
}

export function doAddDeployInstance (instanceRequest) {
  return axios.post(uri`deploy/instance`, instanceRequest)
}

export function doAddDeployServer (deployServerRequest) {
  return axios.post(uri`deploy/server`, deployServerRequest)
}
