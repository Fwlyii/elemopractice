#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend_dir="$repo_dir/elmclient/src"
failed=0

check_absent() {
  local description="$1"
  local pattern="$2"
  shift 2
  if rg -n "$pattern" "$@"; then
    echo "[FAIL] $description"
    failed=1
  else
    echo "[ OK ] $description"
  fi
}

check_absent "活跃页面不再调用旧购物车变更接口" \
  '/api/carts/(add|quantity|remove)' \
  "$frontend_dir/views/BusinessInfo.vue" "$frontend_dir/views/Cart.vue" "$frontend_dir/views/AiChat.vue"

check_absent "AI 客服不使用硬编码演示用户" \
  'return[[:space:]]+33|userId:[[:space:]]*this\.getCurrentUserId' \
  "$frontend_dir/services/aiChatService.js"

check_absent "订单页不直接比较魔法状态码" \
  'orderState[[:space:]]*={2,3}[[:space:]]*[0-9]|orderState:[[:space:]]*[0-9]' \
  "$frontend_dir/views/OrderList.vue" "$frontend_dir/views/ListDetail.vue" \
  "$frontend_dir/views/Payment.vue" "$frontend_dir/views/MerchantOrders.vue"

check_absent "商品接口失败时不伪造模拟商品" \
  '模拟食品|模拟商家' \
  "$frontend_dir/views/BusinessInfo.vue"

check_absent "活跃地址页不使用携带用户 ID 的旧查询接口" \
  '/api/addresses/listDeliveryAddressByUserId' \
  "$frontend_dir/components/AddressManager.vue" "$frontend_dir/views/UserAddress.vue"

check_absent "收藏与点赞由登录态确定用户" \
  'merchant/interaction/(collections/\$\{|status[^m]|update[^\n]*userId)' \
  "$frontend_dir/views/Favorites.vue" "$frontend_dir/views/BusinessInfo.vue"

check_absent "活跃搜索页不调用上一版 Controller 接口" \
  'SearchController|sessionStorage\.getItem\(['"'"']user['"'"']\)' \
  "$frontend_dir/views/Search.vue"

check_absent "普通用户页面不再给通知接口上传 userId" \
  '/api/notifications\?userId|/api/notifications[^\n]*params' \
  "$frontend_dir/views/MyInformation.vue" "$frontend_dir/views/Notifications.vue"

check_absent "用户个人页不再承担多身份切换" \
  '身份中心|goToRole\(|identity-(section|grid|item)' \
  "$frontend_dir/views/MyInformation.vue"

check_absent "用户端登录不再被无条件放行" \
  '!role\.authority' \
  "$frontend_dir/utils/roles.js"

if ! rg -q "role:[[:space:]]*selectedRole\.value" "$frontend_dir/views/Login.vue"; then
  echo "[FAIL] 登录请求必须把所选登录端交给后端校验"
  failed=1
else
  echo "[ OK ] 登录请求会把所选登录端交给后端校验"
fi

check_absent "商家页面不再提供跨端切换入口" \
  '切换为顾客|switchToCustomer' \
  "$frontend_dir/views/MerchantProfile.vue"

check_absent "商家登录失效时不再误跳用户端" \
  "path:[[:space:]]*['\"]\/login['\"][[:space:]]*}" \
  "$frontend_dir/views/MerchantBusiness.vue" "$frontend_dir/views/MerchantApply.vue"

check_absent "页面退出登录不再清空无关本地偏好" \
  '(localStorage|sessionStorage)\.clear\(' \
  "$frontend_dir/views"

check_absent "页面不再绕过统一实时连接服务" \
  'new[[:space:]]+WebSocket|getWebSocketUrl|client-\$\{Date\.now|admin-\$\{Date\.now' \
  "$frontend_dir/views"

check_absent "生产前端不保留调试输出" \
  'console\.log\(' \
  "$frontend_dir"

check_absent "前端启动入口不挂载未管理的全局工具" \
  'globalProperties|\$axios|\$qs' \
  "$frontend_dir/main.js"

check_absent "管理端接口失败时不伪造本地成功数据" \
  '本地模拟|Date\.now\(\).*id' \
  "$frontend_dir/views/AdminShop.vue"

if [[ "$failed" -ne 0 ]]; then
  echo "架构约束检查未通过。"
  exit 1
fi

echo "架构约束检查通过。"
