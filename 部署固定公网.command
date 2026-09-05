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

./scripts/deploy-cloudflare-pages.sh main
open "https://elm-demo.whliugong.xyz"

print "部署完成。固定网址不会改变，但演示期间本机与 Docker Desktop 必须保持运行。"
read "?按回车键关闭窗口……"
