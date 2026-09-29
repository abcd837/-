# 智能采购系统

当前阶段已完成：

- 采购申请模块数据库设计
- PostgreSQL / MySQL 建表脚本
- Spring Boot 后端骨架
- Vue 3 前端骨架
- Python AI Agent 骨架

## 目录结构

```text
.
├── backend/        # Spring Boot 3 后端
├── frontend/       # Vue 3 + Vite 前端
├── ai-agent/       # FastAPI 智能体服务
├── docs/           # 设计文档
├── sql/            # 数据库脚本
└── README.md
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 14+
- Python 3.10+

## 数据库初始化

PostgreSQL：

```powershell
psql -U postgres -f sql/postgresql/001_procurement_application.sql
```

MySQL：

```powershell
mysql -u root -p < sql/mysql/001_procurement_application.sql
```

## 后端启动

修改 `backend/src/main/resources/application.yml` 中的数据库账号和密码。

```powershell
cd backend
mvn -s settings.xml spring-boot:run
```

## AI Agent 启动

```powershell
cd ai-agent
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

前端开发地址默认：

```text
http://localhost:5173
```

后端地址：

```text
http://localhost:8080
```

AI Agent 地址：

```text
http://localhost:8000
```

## 当前接口

### 采购申请

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/applications` | 创建采购申请草稿 |
| GET | `/api/applications` | 分页查询采购申请 |
| GET | `/api/applications/{id}` | 查询采购申请详情 |
| POST | `/api/applications/{id}/submit` | 提交申请 |
| POST | `/api/applications/{id}/approve` | 审批通过 |
| POST | `/api/applications/{id}/reject` | 审批驳回 |
| POST | `/api/files/upload` | 上传采购申请附件 |
| GET | `/api/departments` | 查询可用部门列表 |
| POST | `/api/departments` | 新增部门 |
| PUT | `/api/departments/{id}` | 修改部门 |
| DELETE | `/api/departments/{id}` | 删除部门 |
| GET | `/api/users` | 查询用户列表 |
| POST | `/api/users` | 新增用户 |
| PUT | `/api/users/{id}` | 修改用户 |
| DELETE | `/api/users/{id}` | 删除用户 |
| GET | `/api/roles` | 查询角色列表 |
| POST | `/api/roles` | 新增角色 |
| PUT | `/api/roles/{id}` | 修改角色 |
| DELETE | `/api/roles/{id}` | 删除角色 |
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/auth/me` | 获取当前登录用户 |
| POST | `/api/auth/logout` | 退出登录 |

当前申请人和审批人通过 `applicantId`、`operatorId` 参数临时传入，后续接入登录认证后应改为从当前登录用户获取。

## AI Agent 当前能力

采购申请校验智能体已实现第一版规则校验：

- 完整性校验
- 预算充足性校验
- 金额等级校验
- 紧急采购校验
- 物料合规校验

接口：

```text
POST /api/v1/agents/application-validation
```

当前为规则版，后续可接入大模型、OCR、历史数据去重和 RAG 知识库。

## 下一阶段建议

1. 接入 Spring Security + JWT，替换临时用户参数。
2. 实现采购申请智能体调用，将校验结果写入数据库。
3. 完善前端审批页面。
4. 开始采购寻源与比价模块。
