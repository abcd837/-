<template>
  <router-view v-if="$route.path === '/login'" />

  <el-container v-else class="app-container">
    <el-aside width="220px">
      <div class="logo">
        <el-icon class="logo-icon"><Box /></el-icon>
        <div class="logo-text">
          <div class="logo-name">智能采购系统</div>
          <div class="logo-sub">一体化采购工作台</div>
        </div>
      </div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/applications">
          <el-icon><Document /></el-icon><span>采购申请</span>
        </el-menu-item>
        <el-menu-item v-if="canManageSupplier" index="/suppliers">
          <el-icon><Shop /></el-icon><span>供应商管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/departments">
          <el-icon><Connection /></el-icon><span>部门管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/roles">
          <el-icon><UserFilled /></el-icon><span>角色管理</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">v1.0 · 采购管理中心</div>
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
import { Box, Document, Shop, Connection, User, UserFilled } from '@element-plus/icons-vue'
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

/* ── 深墨绿侧边栏 ── */
.el-aside {
  background: #2f5016;
  display: flex;
  flex-direction: column;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 64px;
  padding: 0 18px;
  color: #fff;
}

.logo-icon {
  font-size: 26px;
  color: #a8d08d;
}

.logo-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 1.3;
}

.logo-sub {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
}

.el-aside .el-menu {
  border-right: none;
  background: transparent;
  padding: 8px 10px;
}

.el-aside .el-menu-item {
  color: rgba(255, 255, 255, 0.75);
  border-radius: 8px;
  margin-bottom: 6px;
  height: 46px;
  line-height: 46px;
}

.el-aside .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.el-aside .el-menu-item.is-active {
  color: #fff;
  background: #47691f;
  box-shadow: inset 3px 0 0 #e8a33d;
}

.sidebar-footer {
  margin-top: auto;
  padding: 14px 20px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

/* ── 顶部栏与内容区 ── */
.app-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e3eadd;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.el-main {
  background: #f2f6ee;
}
</style>
