<template>
  <a-modal
    :width="600"
    title="添加服务器"
    v-model="visible"
    @ok="handleSubmit"
    :afterClose="handleClose">
    <a-form :form="form">
      <a-form-item v-bind="formItemLay" label="ID" style="display:none">
        <a-input v-decorator="['id']"/>
      </a-form-item>
      <a-form-item v-bind="formItemLay" label="环境">
        <a-select v-decorator="['environmentId']">
          <a-select-option :value="0">DEV</a-select-option>
          <a-select-option :value="1">UAT</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item v-bind="formItemLay" label="主机名">
        <a-input v-decorator="['name']"/>
      </a-form-item>
      <a-form-item v-bind="formItemLay" label="IP地址">
        <a-input v-decorator="['ip']" />
      </a-form-item>
      <a-form-item v-bind="formItemLay" label="SSH登录">
        <a-radio-group v-decorator="['loginType']">
          <a-radio :value="0">用户名密码</a-radio>
          <a-radio :value="1">证书</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item v-bind="formItemLay" label="用户名">
        <a-input v-decorator="['user',{rules:[{required:true}]}]" />
      </a-form-item>
      <a-form-item v-bind="formItemLay" label="登录密码">
        <a-input-password v-decorator="['password',{rules:[{required:true}]}]" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script>
import pick from 'lodash.pick'
/* import Deploy from '@/services/deploy' */
import { doAddDeployServer } from '@/api/deploy.js'

export default {
  name: 'ServerModal',
  data () {
    return {
      formItemLay: {
        labelCol: {
          xs: {
            span: 24
          },
          sm: {
            span: 6
          }
        },
        wrapperCol: {
          xs: {
            span: 24
          },
          sm: {
            span: 14
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
          const requestParameter = values
          window.console.log('Received values of form: ', requestParameter)
          doAddDeployServer(requestParameter).then(() => {
            this.visible = false
          })
        } else {
          window.console.log(err)
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
      _this.$nextTick(() => {
        setTimeout(() => {
          _this.form.setFieldsValue(pick(_this.mdl, 'id', 'environmentId', 'name', 'ip',
            'loginType', 'user'))
        })
      })
    }
  }
}
</script>

<style scoped>
  .ant-form-item{
    margin-bottom:5px;
  }
</style>
