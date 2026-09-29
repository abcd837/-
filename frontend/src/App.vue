<template>
  <router-view v-if="$route.path === '/login'" />

  <el-container v-else class="app-container">
    <el-aside width="220px">
      <div class="logo">智能采购系统</div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/applications">采购申请</el-menu-item>
        <el-menu-item v-if="canManageSupplier" index="/suppliers">供应商管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/departments">部门管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">用户管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="/roles">角色管理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-user">
          <span>{{ displayName }}</span>
          <el-button link type="primary" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { logout } from './api/auth'
import { getRoleCodes, hasAnyRole } from './utils/auth'

const router = useRouter()
const displayName = localStorage.getItem('displayName') || '未登录用户'
const roleCodes = getRoleCodes()
const isAdmin = roleCodes.includes('ADMIN')
const canManageSupplier = hasAnyRole(['BUYER', 'PURCHASE_MANAGER', 'ADMIN'])

async function handleLogout() {
  try {
    await logout()
  } finally {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('displayName')
    localStorage.removeItem('roleCodes')
    router.push('/login')
  }
}
</script>

<style>
body {
  margin: 0;
  font-family: "Microsoft YaHei", Arial, sans-serif;
}

.app-container {
  height: 100vh;
}

.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  background: #1f2d3d;
  font-weight: 600;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-aside {
  background: #304156;
}

.el-aside .el-menu {
  border-right: none;
  background: #304156;
}

.el-aside .el-menu-item {
  color: #bfcbd9;
}

.el-aside .el-menu-item.is-active {
  color: #fff;
  background: #409eff;
}
</style>
