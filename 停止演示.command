#!/bin/zsh

set -e

SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR"

docker compose -f docker-compose.demo.yml down

print "演示服务已停止。"
read "?按回车键关闭窗口……"
