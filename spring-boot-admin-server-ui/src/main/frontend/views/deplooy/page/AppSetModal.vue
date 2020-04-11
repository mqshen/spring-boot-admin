<template>
  <a-modal
    :width="600"
    title="应用实例设置"
    v-model="visible"
    @ok="handleSubmit"
    :afterClose="handleClose">
    <a-form :form="form">
      <a-card :bordered="false">
        <a-form-item v-bind="formItemLay" label="" style="display:none">
          <a-input v-decorator="['id']"/>
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="" style="display:none">
          <a-input v-decorator="['serviceId']"/>
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="应用ID">
          <span class="ant-form-text">{{ mdl.serviceName }}</span>
        </a-form-item>
        <template v-if="isAdd">
          <a-form-item v-bind="formItemLay" label="服务器" >
            <a-select v-decorator="['serverId']" placeholder="请选择" class="ifp-selector">
              <a-select-option v-for="server in servers" :key="server.id" :value="server.id">
                {{ server.name }} ({{ server.ip }})
              </a-select-option>
            </a-select>
          </a-form-item>
        </template>
        <template v-else>
          <a-form-item v-bind="formItemLay" label="" style="display:none" >
            <a-input v-decorator="['serverId']"/>
          </a-form-item>
          <a-form-item v-bind="formItemLay" label="服务器" >
            <span class="ant-form-text">{{ mdl.serverName }}</span>
          </a-form-item>
        </template>
        
        <a-form-item v-bind="formItemLay" label="分组">
          <a-radio-group v-decorator="['group']">
            <a-radio v-for="group in groups" :key="group.id" :value="group.id">{{ group.name }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="部署分支">
          <a-input v-decorator="['branch']" />
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="回滚分支">
          <a-input v-decorator="['rollbackBranch']" />
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="PROFILE_ACITVE">
          <a-input v-decorator="['profile',{rules:[{required:true}]}]" />
        </a-form-item>
      </a-card>
    </a-form>
  </a-modal>
</template>

<script>
import pick from 'lodash.pick'
import Deploy from '@/services/deploy'

export default {
  name: 'AppSetModal',
  props: {
    deploy: {
      type: Deploy,
      required: true
    },
    groups: {
      type: Array,
      default: () => [],
    },
    servers: {
      type: Array,
      default: () => [],
    }
  },
  data () {
    return {
      formItemLay: {
        labelCol: {
          xs: {
            span: 24
          },
          sm: {
            span: 8
          }
        },
        wrapperCol: {
          xs: {
            span: 24
          },
          sm: {
            span: 16
          }
        }
      },
      form: this.$form.createForm(this),
      visible: false,
      isAdd: false,
      mdl: {}
    }
  },
  methods: {
    handleSubmit () {
      this.form.validateFields((err, values) => {
        if (!err) {
          this.deploy.doAddDeployInstance(values).then(() => {
            this.visible = false;
          });
          window.console.log('Received values of form: ', values)
        }
      })
    },
    handleClose () {
      this.visible = false
      this.mdl = {}
      this.form.resetFields()
    },
    add (record) {
      this.mdl = Object.assign({}, record)
      this.visible = true
      this.isAdd = true
      const _this = this
      _this.mdl = Object.assign({}, record)
      _this.visible = true
      _this.$nextTick(() => {
        setTimeout(() => {
          _this.form.setFieldsValue(pick(_this.mdl, 'serviceId', 'serviceName'))
        })
      })
    },
    edit(record) {
      const _this = this
      _this.mdl = Object.assign({}, record)
      _this.visible = true
      _this.$nextTick(() => {
        setTimeout(() => {
          _this.form.setFieldsValue(pick(_this.mdl, 'id', 'serviceId', 'serviceName', 'serverId', 'serverName', 
            'group', 'branch', 'rollbackBranch', 'profile'))
        })
      })
    }
  }
}
</script>

<style scoped>
  .ant-card-grid {
    box-shadow:none;
  }
  .ant-card-grid .ant-form-item {
    margin-bottom:0px;
  }
</style>
