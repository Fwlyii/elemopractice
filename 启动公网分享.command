#!/bin/zsh

set -e

SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR"

if ! docker info >/dev/null 2>&1; then
  open -a Docker
  print "正在启动 Docker Desktop，请稍候……"
  for attempt in {1..45}; do
    if docker info >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
fi

if ! docker info >/dev/null 2>&1; then
  print "Docker Desktop 尚未启动完成，请稍后再次双击本文件。"
  read "?按回车键关闭窗口……"
  exit 1
fi

./scripts/ensure-demo-env.sh
# Quick Tunnel 地址是临时的；每次分享前重建 tunnel 容器，避免复用已经失效的隧道。
docker compose -f docker-compose.demo.yml --profile share rm -sf tunnel >/dev/null 2>&1 || true
docker compose -f docker-compose.demo.yml --profile share up -d --build

PUBLIC_DEMO_URL=""
for attempt in {1..45}; do
  PUBLIC_DEMO_URL=$(docker compose -f docker-compose.demo.yml --profile share logs --no-color tunnel 2>/dev/null | rg -o 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -n 1)
  if [[ -n "$PUBLIC_DEMO_URL" ]]; then
    break
  fi
  sleep 1
done

if [[ -z "$PUBLIC_DEMO_URL" ]]; then
  print "公网隧道仍在启动，请稍后查看 tunnel 容器日志。"
  read "?按回车键关闭窗口……"
  exit 1
fi

open "$PUBLIC_DEMO_URL"
print "公网分享已启动：$PUBLIC_DEMO_URL"
print "把这个地址发给组员即可；分享期间本机和 Docker Desktop 必须保持运行。"
print "演示账号密码请查看《本地演示说明.md》。"
read "?按回车键关闭窗口……"
