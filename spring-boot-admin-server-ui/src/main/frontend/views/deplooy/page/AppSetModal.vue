<template>
  <a-modal
    :width="600"
    title="应用实例设置"
    v-model="visible"
    @ok="handleSubmit"
    :afterClose="handleClose">
    <a-form :form="form">
      <a-card :bordered="false">
        <a-form-item v-bind="formItemLay" label="应用ID">
          <span class="ant-form-text">{{ mdl.appId }}</span>
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="分组">
          <a-radio-group v-decorator="['group']">
            <a-radio :value="1">默认</a-radio>
            <a-radio :value="2">灰度</a-radio>
            <a-radio :value="3">灰度2</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="部署分支">
          <a-input v-decorator="['deployDev']" />
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="回滚分支">
          <a-input v-decorator="['taskType']" />
        </a-form-item>
        <a-form-item v-bind="formItemLay" label="PROFILE_ACITVE">
          <a-input v-decorator="['destination',{rules:[{required:true}]}]" />
        </a-form-item>
      </a-card>
    </a-form>
  </a-modal>
</template>

<script>
import pick from 'lodash.pick'

export default {
  name: 'AppSetModal',
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
        }
      })
    },
    handleClose () {
      this.visible = false
      this.mdl = {}
      this.form.resetFields()
    },
    edit (record) {
      const _this = this
      _this.mdl = Object.assign({}, record)
      _this.visible = true
      _this.$nextTick(() => {
        setTimeout(() => {
          _this.form.setFieldsValue(pick(_this.mdl, 'group', 'deployDev', 'taskType',
            'destination'))
        })
      })
    }
  }
}
</script>

<style scoped>
  .ant-card-grid{
    box-shadow:none;
    .ant-form-item{
      margin-bottom:0px;
    }
  }
</style>
