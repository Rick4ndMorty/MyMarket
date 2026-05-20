#!/bin/bash
set -euo pipefail

NACOS_SERVER="http://127.0.0.1:8848"
NACOS_NAMESPACE=""  # public 留空，非 public 填 namespace ID

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PUSH_DIR="${SCRIPT_DIR}/push"

# ============================================================
# Helper: 根据文件名推断 group
# ============================================================
guess_group() {
    case "$1" in
        seataServer.properties) echo "SEATA_GROUP" ;;
        *) echo "DEFAULT_GROUP" ;;
    esac
}

# ============================================================
# Helper: 根据扩展名推断配置类型
# ============================================================
guess_type() {
    case "$1" in
        *.yaml|*.yml) echo "yaml" ;;
        *.properties) echo "text" ;;
        *.json)       echo "json" ;;
        *.xml)        echo "xml" ;;
        *)            echo "text" ;;
    esac
}

# ============================================================
# 检查 push 目录
# ============================================================
if [ ! -d "$PUSH_DIR" ]; then
    echo "ERROR: push directory not found at:"
    echo "  ${PUSH_DIR}"
    echo "Create it and place your config files there."
    exit 1
fi

# 收集文件（只处理文件，不递归子目录）
FILES=()
while IFS= read -r -d '' f; do
    FILES+=("$f")
done < <(find "$PUSH_DIR" -maxdepth 1 -type f -print0)

if [ ${#FILES[@]} -eq 0 ]; then
    echo "ERROR: No files found in ${PUSH_DIR}"
    exit 1
fi

# ============================================================
# 预览将要推送的内容
# ============================================================
echo ""
echo "============================================"
echo "Nacos Config Push     $(date)"
echo "Server: ${NACOS_SERVER}"
echo "Source: ${PUSH_DIR}"
echo "============================================"
echo ""
echo "Files to push:"

for f in "${FILES[@]}"; do
    name=$(basename "$f")
    group=$(guess_group "$name")
    type=$(guess_type "$name")
    size=$(wc -c < "$f" | tr -d ' ')
    echo "  [${type}] ${name}  ->  group: ${group}  (${size} bytes)"
done

echo ""
echo "WARNING: This will OVERWRITE existing configs in Nacos!"
echo ""

# ============================================================
# 用户确认
# ============================================================
read -r -p "Push all above configs to Nacos? (y/N): " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo "Cancelled."
    exit 0
fi

echo ""

# ============================================================
# 推送
# ============================================================
FAILED=0
SUCCESS=0

for f in "${FILES[@]}"; do
    name=$(basename "$f")
    group=$(guess_group "$name")
    type=$(guess_type "$name")

    # base64 编码内容，避免 ${} 等特殊字符被 bash/curl 解释
    encoded=$(base64 -w0 "$f")

    echo -n "Pushing ${name} ... "

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
        "${NACOS_SERVER}/nacos/v1/cs/configs" \
        --data-urlencode "dataId=${name}" \
        --data-urlencode "group=${group}" \
        --data-urlencode "type=${type}" \
        --data-urlencode "content=${encoded}" \
        --data-urlencode "encode=true" \
        ${NACOS_NAMESPACE:+ --data-urlencode "tenant=${NACOS_NAMESPACE}"})

    if [ "$HTTP_CODE" = "200" ]; then
        echo "OK"
        SUCCESS=$((SUCCESS + 1))
    else
        echo "FAILED (HTTP ${HTTP_CODE})"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "============================================"
echo "Push complete: ${SUCCESS} success, ${FAILED} failed"
echo "============================================"

exit $FAILED
