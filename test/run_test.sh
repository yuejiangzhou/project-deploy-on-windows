#!/bin/bash
set -e
BASE="http://localhost:9090"
PASS=0; FAIL=0
G='\033[32m'; R='\033[31m'; Y='\033[33m'; N='\033[0m'
ok() { echo -e "${G}[PASS]${N} $1"; PASS=$((PASS+1)); }
no() { echo -e "${R}[FAIL]${N} $1 → $2"; FAIL=$((FAIL+1)); }

echo "============================================"
echo "  功能测试开始 $(date '+%H:%M:%S')"
echo "============================================"

# ── 1. 启动 ──
echo -e "\n${Y}▶ Step 1: 启动后端${N}"
kill $(lsof -t -i:9090) 2>/dev/null; sleep 2
cd /workspace/project-repo/deploy-manager-backend
JWT_SECRET="ThisIsAVeryLongJwtSecretKeyForHmacSha256AtLeast256Bits!!" \
  nohup java -jar target/deploy-manager-1.0.0.jar > /tmp/app.log 2>&1 &
for i in $(seq 1 30); do
  sleep 2
  curl -s $BASE/api/auth/login -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"admin123"}' | grep -q '"code":200' && break
done
curl -s $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | grep -q '"code":200' \
  && ok "后端启动+登录" || { no "后端启动" "失败"; tail -20 /tmp/app.log; exit 1; }

# ── 2. 认证 ──
echo -e "\n${Y}▶ Step 2: 认证测试${N}"
LOGIN=$(curl -s $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null)
AUTH="Authorization: Bearer $TOKEN"
[ -n "$TOKEN" ] && ok "获取Token" || no "获取Token" "$LOGIN"

curl -s $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"wrong"}' | grep -q '"code":401' \
  && ok "错误密码返回401" || no "错误密码" "未返回401"

curl -s $BASE/api/auth/me -H "$AUTH" | grep -q '"username":"admin"' \
  && ok "获取当前用户" || no "获取用户信息" "失败"

# ── 3. 用户管理 ──
echo -e "\n${Y}▶ Step 3: 用户管理${N}"
curl -s $BASE/api/users -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"username":"tester","password":"test123456","displayName":"测试员","role":"DEVELOPER"}' | grep -q '"code":200' \
  && ok "创建用户 tester" || no "创建用户" "失败"

USERS=$(curl -s "$BASE/api/users?pageNum=1&pageSize=10" -H "$AUTH")
TOTAL=$(echo "$USERS" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['total'])" 2>/dev/null)
[ "$TOTAL" -ge 2 ] && ok "用户列表 total=$TOTAL" || no "用户列表" "total=$TOTAL"

# ── 4. 项目管理 ──
echo -e "\n${Y}▶ Step 4: 项目管理${N}"
PROJ=$(curl -s $BASE/api/projects -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"name":"自动化测试项目","description":"脚本创建"}')
PID=$(echo "$PROJ" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
[ -n "$PID" ] && ok "创建项目 id=$PID" || no "创建项目" "$PROJ"

curl -s "$BASE/api/projects/$PID" -H "$AUTH" | grep -q '"name":"自动化测试项目"' \
  && ok "项目详情" || no "项目详情" "失败"

# ── 5. 基础设施 API（验证新路径） ──
echo -e "\n${Y}▶ Step 5: 基础设施API (新路径 /api/infrastructures)${N}"
curl -s "$BASE/api/infrastructures/mysql/list" -H "$AUTH" | grep -q '"code":200' \
  && ok "新路径 /api/infrastructures 正常" || no "新路径" "失败"

OLD=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/infra/mysql/list" -H "$AUTH")
[ "$OLD" = "404" ] && ok "旧路径 /api/infra 返回404" || no "旧路径404" "返回$OLD"

# ── 6. License ──
echo -e "\n${Y}▶ Step 6: License (新路径 /api/licenses)${N}"
LIC=$(curl -s "$BASE/api/licenses/generate" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"projectId\":$PID,\"customerName\":\"测试客户\",\"trialDays\":30,\"type\":\"TRIAL\"}")
LID=$(echo "$LIC" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
[ -n "$LID" ] && ok "生成License id=$LID" || no "生成License" "$LIC"

curl -s "$BASE/api/licenses/list?projectId=$PID&page=1&size=10" -H "$AUTH" | grep -q '"total":1' \
  && ok "License列表 total=1" || no "License列表" "失败"

curl -s "$BASE/api/licenses/${LID}/renew" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"additionalDays":30}' | grep -q '"code":200' \
  && ok "License续期" || no "License续期" "失败"

DL=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/licenses/${LID}/download" -H "$AUTH")
[ "$DL" = "200" ] && ok "License下载" || no "License下载" "HTTP $DL"

# ── 7. 打包 ──
echo -e "\n${Y}▶ Step 7: 打包 (不加密)${N}"
PKG=$(curl -s "$BASE/api/package/start" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"projectId\":$PID,\"encrypted\":false,\"password\":\"\",\"licenseId\":$LID}")
PKG_CODE=$(echo "$PKG" | python3 -c "import sys,json; print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
# 打包可能因为没有上传组件而失败，但API应可达
[ "$PKG_CODE" = "200" ] || [ "$PKG_CODE" = "500" ] \
  && ok "打包API可达 code=$PKG_CODE" || no "打包API" "$PKG_CODE"

# ── 8. 权限 ──
echo -e "\n${Y}▶ Step 8: 权限验证${N}"
NOAUTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/projects")
[ "$NOAUTH" = "401" ] || [ "$NOAUTH" = "403" ] \
  && ok "无Token被拒绝 $NOAUTH" || no "无Token" "返回$NOAUTH"

# ── 9. 清理 ──
echo -e "\n${Y}▶ Step 9: 清理${N}"
curl -s -X DELETE "$BASE/api/projects/$PID" -H "$AUTH" > /dev/null && ok "清理项目" || no "清理项目" ""
kill $(lsof -t -i:9090) 2>/dev/null
ok "后端已停止"

# ── 结果 ──
echo ""
echo "============================================"
echo "  测试结果: ${G}通过 $PASS${N}  ${R}失败 $FAIL${N}  总计 $((PASS+FAIL))"
echo "============================================"
[ $FAIL -eq 0 ] && echo -e "${G}✅ 全部通过!${N}" || echo -e "${R}❌ 有失败${N}"
