#!/usr/bin/env bash
# 下载 Plasmo Voice 官方 jar 到 libs/（编译期可选依赖，不提交到 git）
# 用法: ./scripts/fetch-plasmovoice.sh
set -e
cd "$(dirname "$0")/.."
mkdir -p libs
URL="https://cdn.modrinth.com/data/1bZhdhsH/versions/sQEBy4Ef/plasmovoice-forge-1.20.1-2.1.13.jar"
if [ ! -f "libs/plasmovoice-forge-1.20.1-2.1.13.jar" ]; then
  echo "Downloading Plasmo Voice 2.1.13 (Forge 1.20.1)..."
  curl -fL --retry 3 -o "libs/plasmovoice-forge-1.20.1-2.1.13.jar" "$URL"
fi
echo "OK: libs/plasmovoice-forge-1.20.1-2.1.13.jar"
