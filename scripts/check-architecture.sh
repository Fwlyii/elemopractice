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

check_absent "页面不再绕过统一实时连接服务" \
  'new[[:space:]]+WebSocket|getWebSocketUrl|client-\$\{Date\.now|admin-\$\{Date\.now' \
  "$frontend_dir/views"

if [[ "$failed" -ne 0 ]]; then
  echo "架构约束检查未通过。"
  exit 1
fi

echo "架构约束检查通过。"
