<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">
        <h1>智能采购系统</h1>
        <p>Intelligent Procurement System</p>
      </div>

      <el-form :model="form" size="large" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入登录账号"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
          />
        </el-form-item>

        <el-button
          type="primary"
          class="login-button"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login } from '../../api/auth'

const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入登录账号和密码')
    return
  }

  loading.value = true
  try {
    const data = await login({
      username: form.username,
      password: form.password
    })

    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('displayName', data.displayName)
    localStorage.setItem('roleCodes', JSON.stringify(data.roleCodes))

    ElMessage.success('登录成功')
    window.location.href = '/applications'
  } catch {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1f2d3d 0%, #304156 55%, #409eff 100%);
}

.login-card {
  width: 420px;
  padding: 40px 36px 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
}

.login-title {
  margin-bottom: 28px;
  text-align: center;
}

.login-title h1 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #1f2d3d;
}

.login-title p {
  margin: 0;
  color: #8a94a6;
  font-size: 13px;
}

.login-button {
  width: 100%;
  margin-top: 8px;
}
</style>
