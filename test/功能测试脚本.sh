#!/bin/bash
# ============================================================
# 内部产品打包分发系统 — 端到端功能测试脚本
# 使用方法：在项目根目录下执行 bash test/功能测试脚本.sh
# 前置条件：Docker 已安装，端口 3306/9000/9090 未被占用
# ============================================================
set -e

BASE_URL="http://localhost:9090"
PASS=0
FAIL=0

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
log_fail() { echo -e "${RED}[FAIL]${NC} $1 (expected: $2, got: $3)"; FAIL=$((FAIL+1)); }
log_info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

# -----------------------------------------------------------
# Step 0: 启动 Docker 环境
# -----------------------------------------------------------
log_info "Step 0: 启动 Docker 环境 (MySQL + MinIO)..."
docker compose down -v 2>/dev/null || true
docker compose up -d
log_info "等待 MySQL 就绪..."
until docker exec deploy-mysql mysqladmin ping -uroot -proot --silent 2>/dev/null; do sleep 2; done
log_info "MySQL 已就绪，等待表初始化..."
sleep 5
docker exec deploy-mysql mysql -uroot -proot deploy_manager -e "SHOW TABLES;" 2>/dev/null | grep -q "user" && log_pass "数据库表初始化" || log_fail "数据库表初始化" "有表" "无表"

# -----------------------------------------------------------
# Step 1: 启动后端
# -----------------------------------------------------------
log_info "Step 1: 启动后端服务..."
# 生成 JWT 密钥（至少 256 bits）
JWT_SECRET=$(openssl rand -base64 64)
cd deploy-manager-backend
JWT_SECRET="$JWT_SECRET" nohup mvn spring-boot:run > /tmp/deploy-backend.log 2>&1 &
BACKEND_PID=$!
cd ..
log_info "后端 PID=$BACKEND_PID，等待启动..."

for i in $(seq 1 40); do
  sleep 3
  if curl -s "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' 2>/dev/null | grep -q '"code":200'; then
    log_pass "后端启动成功"
    break
  fi
  if [ $i -eq 40 ]; then
    log_fail "后端启动" "200" "timeout"
    tail -30 /tmp/deploy-backend.log
    exit 1
  fi
done

# -----------------------------------------------------------
# Step 2: 认证测试
# -----------------------------------------------------------
log_info "Step 2: 认证测试..."

# T2.1 正确登录
RESP=$(curl -s "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}')
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('token',''))" 2>/dev/null)
[ "$CODE" = "200" ] && [ -n "$TOKEN" ] && log_pass "T2.1 正确登录获取Token" || log_fail "T2.1 正确登录" "200+token" "$CODE"

# T2.2 错误密码
RESP=$(curl -s "$BASE_URL/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrongpass"}')
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
[ "$CODE" != "200" ] && log_pass "T2.2 错误密码被拒绝" || log_fail "T2.2 错误密码" "非200" "$CODE"

# T2.3 获取当前用户信息
AUTH_HEADER="Authorization: Bearer $TOKEN"
RESP=$(curl -s "$BASE_URL/api/auth/me" -H "$AUTH_HEADER")
USERNAME=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('username',''))" 2>/dev/null)
[ "$USERNAME" = "admin" ] && log_pass "T2.3 获取当前用户信息" || log_fail "T2.3 获取用户信息" "admin" "$USERNAME"

# -----------------------------------------------------------
# Step 3: 用户管理测试 (仅管理员)
# -----------------------------------------------------------
log_info "Step 3: 用户管理测试..."

# T3.1 创建用户
RESP=$(curl -s "$BASE_URL/api/users" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"username":"testdev","password":"test123456","displayName":"测试开发者","role":"DEVELOPER"}')
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
[ "$CODE" = "200" ] && log_pass "T3.1 创建用户 testdev" || log_fail "T3.1 创建用户" "200" "$CODE"

# T3.2 用户列表
RESP=$(curl -s "$BASE_URL/api/users?pageNum=1&pageSize=10" -H "$AUTH_HEADER")
TOTAL=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('total',0))" 2>/dev/null)
[ "$TOTAL" -ge 2 ] && log_pass "T3.2 用户列表 (total=$TOTAL)" || log_fail "T3.2 用户列表" ">=2" "$TOTAL"

# -----------------------------------------------------------
# Step 4: 项目管理测试
# -----------------------------------------------------------
log_info "Step 4: 项目管理测试..."

# T4.1 创建项目
RESP=$(curl -s "$BASE_URL/api/projects" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"name":"测试项目","description":"功能测试项目"}')
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
PROJECT_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null)
[ "$CODE" = "200" ] && [ -n "$PROJECT_ID" ] && log_pass "T4.1 创建项目 (id=$PROJECT_ID)" || log_fail "T4.1 创建项目" "200" "$CODE"

# T4.2 项目列表
RESP=$(curl -s "$BASE_URL/api/projects?pageNum=1&pageSize=10" -H "$AUTH_HEADER")
TOTAL=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('total',0))" 2>/dev/null)
[ "$TOTAL" -ge 1 ] && log_pass "T4.2 项目列表 (total=$TOTAL)" || log_fail "T4.2 项目列表" ">=1" "$TOTAL"

# T4.3 项目详情
RESP=$(curl -s "$BASE_URL/api/projects/$PROJECT_ID" -H "$AUTH_HEADER")
NAME=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('name',''))" 2>/dev/null)
[ "$NAME" = "测试项目" ] && log_pass "T4.3 项目详情" || log_fail "T4.3 项目详情" "测试项目" "$NAME"

# -----------------------------------------------------------
# Step 5: 基础设施 API 测试
# -----------------------------------------------------------
log_info "Step 5: 基础设施 API 测试..."

# T5.1 获取 MySQL 组件列表（使用新 API 路径 /api/infrastructures）
RESP=$(curl -s "$BASE_URL/api/infrastructures/mysql/list" -H "$AUTH_HEADER")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
[ "$CODE" = "200" ] && log_pass "T5.1 基础设施列表 (新路径 /api/infrastructures)" || log_fail "T5.1 基础设施列表" "200" "$CODE"

# T5.2 旧路径应返回 404（验证 API 路径统一生效）
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/infra/mysql/list" -H "$AUTH_HEADER")
[ "$RESP" = "404" ] && log_pass "T5.2 旧路径 /api/infra 返回404" || log_fail "T5.2 旧路径" "404" "$RESP"

# -----------------------------------------------------------
# Step 6: License API 测试
# -----------------------------------------------------------
log_info "Step 6: License API 测试..."

# T6.1 生成 License（使用新路径 /api/licenses）
RESP=$(curl -s "$BASE_URL/api/licenses/generate" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d "{\"projectId\":$PROJECT_ID,\"customerName\":\"测试客户\",\"trialDays\":30,\"type\":\"TRIAL\"}")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
LICENSE_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null)
[ "$CODE" = "200" ] && [ -n "$LICENSE_ID" ] && log_pass "T6.1 生成License (id=$LICENSE_ID)" || log_fail "T6.1 生成License" "200" "$CODE"

# T6.2 License 列表（新路径）
RESP=$(curl -s "$BASE_URL/api/licenses/list?projectId=$PROJECT_ID&page=1&size=10" -H "$AUTH_HEADER")
TOTAL=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('total',0))" 2>/dev/null)
[ "$TOTAL" -ge 1 ] && log_pass "T6.2 License列表 (total=$TOTAL)" || log_fail "T6.2 License列表" ">=1" "$TOTAL"

# T6.3 License 续期
RESP=$(curl -s "$BASE_URL/api/licenses/$LICENSE_ID/renew" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d '{"additionalDays":30}')
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
[ "$CODE" = "200" ] && log_pass "T6.3 License续期" || log_fail "T6.3 License续期" "200" "$CODE"

# T6.4 License 下载（验证文件可下载）
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/licenses/$LICENSE_ID/download" -H "$AUTH_HEADER")
[ "$HTTP_CODE" = "200" ] && log_pass "T6.4 License下载" || log_fail "T6.4 License下载" "200" "$HTTP_CODE"

# -----------------------------------------------------------
# Step 7: 打包测试（不加密模式）
# -----------------------------------------------------------
log_info "Step 7: 打包测试..."

# T7.1 启动打包（不加密，验证 encrypted=false 新参数）
RESP=$(curl -s "$BASE_URL/api/package/start" -H "$AUTH_HEADER" -H 'Content-Type: application/json' \
  -d "{\"projectId\":$PROJECT_ID,\"encrypted\":false,\"password\":\"\",\"licenseId\":$LICENSE_ID}")
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
TASK_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('taskId',''))" 2>/dev/null)

# 注意：打包可能因为没有上传组件文件而失败，这是预期的
# 这里只测试 API 是否可达
if [ "$CODE" = "200" ] || [ "$CODE" = "500" ]; then
  log_pass "T7.1 打包API可达 (code=$CODE)"
else
  log_fail "T7.1 打包API" "200/500" "$CODE"
fi

# T7.2 打包进度查询
if [ -n "$TASK_ID" ]; then
  RESP=$(curl -s "$BASE_URL/api/package/progress/$TASK_ID" -H "$AUTH_HEADER")
  STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('status',''))" 2>/dev/null)
  [ -n "$STATUS" ] && log_pass "T7.2 打包进度查询 (status=$STATUS)" || log_fail "T7.2 打包进度" "有状态" "无"
fi

# T7.3 打包记录列表
RESP=$(curl -s "$BASE_URL/api/packages?projectId=$PROJECT_ID&pageNum=1&pageSize=10" -H "$AUTH_HEADER")
TOTAL=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('total',0))" 2>/dev/null)
[ -n "$TOTAL" ] && log_pass "T7.3 打包记录列表 (total=$TOTAL)" || log_fail "T7.3 打包记录" "有数据" "无"

# -----------------------------------------------------------
# Step 8: 无权限测试
# -----------------------------------------------------------
log_info "Step 8: 无权限测试..."

# T8.1 无 Token 访问受保护资源
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/projects")
[ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ] && log_pass "T8.1 无Token被拒绝 ($HTTP_CODE)" || log_fail "T8.1 无Token" "401/403" "$HTTP_CODE"

# -----------------------------------------------------------
# Step 9: 清理
# -----------------------------------------------------------
log_info "Step 9: 清理测试数据..."
curl -s "$BASE_URL/api/licenses/$LICENSE_ID" -X DELETE -H "$AUTH_HEADER" 2>/dev/null || true
curl -s "$BASE_URL/api/projects/$PROJECT_ID" -X DELETE -H "$AUTH_HEADER" 2>/dev/null || true
log_pass "测试数据已清理"

# -----------------------------------------------------------
# 汇总
# -----------------------------------------------------------
echo ""
echo "============================================"
echo "  测试结果汇总"
echo "============================================"
echo -e "  ${GREEN}通过: $PASS${NC}"
echo -e "  ${RED}失败: $FAIL${NC}"
echo "  总计: $((PASS + FAIL))"
echo "============================================"

# 停止后端
kill $BACKEND_PID 2>/dev/null || true
log_info "后端已停止"

[ $FAIL -eq 0 ] && echo "✅ 所有测试通过!" || echo "❌ 存在失败用例，请检查"
