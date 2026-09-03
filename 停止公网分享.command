#!/bin/zsh

set -e

SCRIPT_DIR="${0:A:h}"
cd "$SCRIPT_DIR"

docker compose -f docker-compose.demo.yml --profile share stop tunnel

print "公网分享已停止，本地演示仍然保持运行。"
read "?按回车键关闭窗口……"
