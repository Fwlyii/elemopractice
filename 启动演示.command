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
docker compose -f docker-compose.demo.yml up -d
open "http://localhost:18081"

print "演示已启动：http://localhost:18081"
print "普通用户：demo_user / Demo1234!"
print "商家账号：demo_merchant / Demo1234!"
print "管理员：demo_admin / Demo1234!"
read "?按回车键关闭窗口……"
