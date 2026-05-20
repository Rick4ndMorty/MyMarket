#!/bin/bash
set -euo pipefail

NACOS_SERVER="http://127.0.0.1:8848"
NACOS_NAMESPACE=""  # public 留空，非 public 填 namespace ID

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="${SCRIPT_DIR}/${TIMESTAMP}"

mkdir -p "$OUTPUT_DIR"

echo "============================================"
echo "Nacos Config Pull     $(date)"
echo "Server: ${NACOS_SERVER}"
echo "Output: ${OUTPUT_DIR}"
echo "============================================"

# --------------------------------------------------
# Step 1: 获取配置列表
# --------------------------------------------------
echo ""
echo "[1/2] Fetching config list..."

LIST_RESP=$(curl -s -G "${NACOS_SERVER}/nacos/v1/cs/configs" \
    --data-urlencode "pageNo=1" \
    --data-urlencode "pageSize=500" \
    --data-urlencode "search=accurate" \
    --data-urlencode "dataId=" \
    --data-urlencode "group=" \
    ${NACOS_NAMESPACE:+ --data-urlencode "tenant=${NACOS_NAMESPACE}"})

# 提取 dataId 和 group，用 | 分隔
# 从单行 JSON 中匹配 "dataId":"xxx","group":"yyy" 模式
PAIRS=$(echo "$LIST_RESP" | sed 's/},/}\n/g' | grep -o '"dataId":"[^"]*","group":"[^"]*"' | sed 's/"dataId":"//;s/","group":"/|/;s/"$//' || true)

TOTAL=$(echo "$PAIRS" | grep -c '|' || echo "0")
echo "  Found ${TOTAL} config(s)"

if [ "$TOTAL" = "0" ]; then
    echo ""
    echo "ERROR: No configs found or API returned unexpected format."
    echo "Try running manually to debug:"
    echo "  curl -s \"${NACOS_SERVER}/nacos/v1/cs/configs?pageNo=1&pageSize=500&search=accurate&dataId=&group=\""
    exit 1
fi

# --------------------------------------------------
# Step 2: 逐个下载配置内容
# --------------------------------------------------
echo ""
echo "[2/2] Downloading configs..."

COUNT=0
while IFS='|' read -r dataId group; do
    [ -z "$dataId" ] && continue
    COUNT=$((COUNT + 1))

    # 文件名安全处理（dataId 中可能有冒号等特殊字符）
    safe_name=$(echo "$dataId" | sed 's/[\/:*?"<>|]/_/g')

    # 下载（GET 方式，返回原始内容）
    HTTP_CODE=0
    CONTENT=$(curl -s -w "%{http_code}" -G "${NACOS_SERVER}/nacos/v1/cs/configs" \
        --data-urlencode "dataId=${dataId}" \
        --data-urlencode "group=${group}" \
        ${NACOS_NAMESPACE:+ --data-urlencode "tenant=${NACOS_NAMESPACE}"})

    CODE="${CONTENT: -3}"
    BODY="${CONTENT:0:${#CONTENT}-3}"

    if [ "$CODE" = "200" ]; then
        echo "$BODY" > "${OUTPUT_DIR}/${safe_name}"
        echo "  [${COUNT}] ${dataId}  (${group})  ->  saved"
    else
        echo "  [${COUNT}] ${dataId}  (${group})  ->  FAILED (HTTP ${CODE})"
    fi

done <<< "$PAIRS"

echo ""
echo "============================================"
echo "Done. ${COUNT} config(s) saved to:"
echo "  ${OUTPUT_DIR}"
ls -1 "${OUTPUT_DIR}"
echo "============================================"
