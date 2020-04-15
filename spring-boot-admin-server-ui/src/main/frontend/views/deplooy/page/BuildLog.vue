<template>
  <a-modal
    :width="1000"
    v-model="visible"
    @ok="handleSubmit"
    :afterClose="handleClose">
    <div> {{ buildLog }} </div>
    <a-spin/>
  </a-modal>
</template>

<script>
/* import Deploy from '@/services/deploy' */
import { queryBuildLog } from '@/api/deploy.js'

export default {
  name: 'BuildLog',
  data () {
    return {
      visible: false,
      buildLog: ''
    }
  },
  methods: {
    handleClose () {
      this.visible = false
    },
    handleSubmit () {
      this.visible = false
    },
    show (instanceId) {
      this.visible = true
      this.queryLog(instanceId)
    },
    queryLog (instanceId) {
      queryBuildLog(instanceId).then((res) => {
        this.buildLog = res.data
        if (this.visible) {
          setTimeout(() => this.queryLog(instanceId), 3000)
        }
      })
    }
  }
}
</script>
