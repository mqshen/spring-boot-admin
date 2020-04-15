import axios from 'axios'
// import notification from 'ant-design-vue/es/notification'
import { Modal } from 'ant-design-vue'

// 创建 axios 实例
const service = axios.create({
  // baseURL: process.env.VUE_APP_API_BASE_URL, // api base_url
  // baseURL: '/api',
  timeout: 6000 // 请求超时时间
})

const err = (error) => {
  if (error.response) {
    const data = error.response.data
    if (error.response.status === 403) {
      /* notification.error({
        message: 'Forbidden',
        description: data.message
      }) */
    }
    if (error.response.status === 401 && !(data.result && data.result.isLogin)) {
      /* notification.error({
        message: 'Unauthorized',
        description: 'Authorization verification failed'
      }) */
    }
  }
  return Promise.reject(error)
}

// response interceptor
service.interceptors.response.use((response) => {
  const data = response.data
  if (data.returnCode && data.returnCode !== '000000') {
    Modal.error({ content: data.returnMessage })
  }
  return data.result
}, err)

export {
  service as axios
}
