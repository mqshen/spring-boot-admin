<template>
  <a-modal
    :width="1000"
    v-model="visible"
    @ok="handleSubmit"
    :afterClose="handleClose">
    <a-form :form="form">
      <a-form-item v-bind="formItemLay" label="ID" style="display:none">
        <a-input v-decorator="['id']"/>
      </a-form-item>
      <a-card :bordered="false">
        <a-card-grid style="width:50%;padding-left:0px;padding-top:0px">
          <h3 style="text-align:center">基本配置</h3>
          <a-form-item v-bind="formItemLay" label="应用ID">
            <a-input v-decorator="['name',{rules:[{required:true}]}]" />
          </a-form-item>
          <a-form-item v-bind="formItemLay" label="Jenkins任务">
            <a-input v-decorator="['jobName',{rules:[{required:true}]}]" />
          </a-form-item>
          <a-form-item v-bind="formItemLay" label="工程名称">
            <a-input v-decorator="['projectName',{rules:[{required:true}]}]" />
          </a-form-item>
          <a-form-item v-bind="formItemLay" label="部署包类型">
            <a-radio-group v-decorator="['deployType']">
              <a-radio :value="0">Jar</a-radio>
              <a-radio :value="1">War</a-radio>
              <a-radio :value="2">Jar(spring cloud task)</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item v-bind="formItemLay" label="部署后运行">
            <a-radio-group v-decorator="['autoStart']">
              <a-radio :value="true">是</a-radio>
              <a-radio :value="false">否</a-radio>
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
          <a-form-item v-bind="formItemLay" label="应用端口号">
            <a-input v-decorator="['port',{rules:[{required:true}]}]" />
          </a-form-item>
        </a-card-grid>
        <a-card-grid style="width:50%;padding-right:0px;padding-top:0px">
          <h3 style="text-align:center">高级配置参数</h3>
          <a-form-item v-bind="formItemLay" label="部署目录">
            <a-input v-decorator="['path']" />
          </a-form-item>
          <a-form-item v-bind="formItemLay" label="环境变量">
            <a-input v-decorator="['env']" />
          </a-form-item>
          <a-form-item v-bind="formItemLay" label="命令行参数">
            <a-input v-decorator="['parameter']" />
          </a-form-item>
        </a-card-grid>
      </a-card>
    </a-form>
  </a-modal>
</template>

<script>
import pick from 'lodash.pick'
import Deploy from '@/services/deploy'

export default {
  name: 'AppModal',
  props: {
    regionMap: {
      type: Array,
      default: () => []
    },
    deploy: {
      type: Deploy,
      required: true
    },
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
      mdl: {}
    }
  },
  methods: {
    handleSubmit () {
      this.form.validateFields((err, values) => {
        if (!err) {
          window.console.log('Received values of form: ', values)
          const requestParameter = Object.assign({}, values)
          window.console.log('Received values of form: ', requestParameter, this.deploy)
          this.deploy.doAddService(requestParameter).then(() => {
            this.visible = false;
          });
        }
      })
    },
    handleClose () {
      this.visible = false
      this.mdl = {}
      this.form.resetFields()
    },
    add () {
      this.visible = true
    },
    edit (record) {
      const _this = this
      _this.mdl = Object.assign({}, record)
      _this.visible = true
      _this.modalTitle = '编辑任务'
      _this.$nextTick(() => {
        setTimeout(() => {
          _this.form.setFieldsValue(pick(_this.mdl, 'id', 'name', 'jobName', 'projectName',
            'deployType', 'autoStart', 'branch', 'rollbackBranch', 'profile', 'port',
            'path', 'env', 'parameter'))
        })
      })
    }
  }
}
</script>

<style scoped >
.ant-card-grid {
  box-shadow:none;
}
.ant-card-grid .ant-form-item {
  margin-bottom:0px;
}
</style>
