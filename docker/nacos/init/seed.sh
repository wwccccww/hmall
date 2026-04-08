#!/bin/sh
set -eu

addr="${NACOS_ADDR:-http://nacos:8848}"
ns="${NACOS_NS:-public}"
group="${NACOS_GROUP:-DEFAULT_GROUP}"

put_config() {
  data_id="$1"
  file="$2"
  echo "[nacos-seed] put ${data_id} from ${file}"
  curl -fsS -X POST "${addr}/nacos/v1/cs/configs" \
    -d "dataId=${data_id}" \
    -d "group=${group}" \
    -d "tenant=${ns}" \
    --data-urlencode "content@${file}" >/dev/null
}

put_config "shared-jdbc.yaml" /init/shared-jdbc.yaml
put_config "shared-log.yaml" /init/shared-log.yaml
put_config "shared-swagger.yaml" /init/shared-swagger.yaml
put_config "shared-observability.yaml" /init/shared-observability.yaml
put_config "shared-seata.yaml" /init/shared-seata.yaml
put_config "gateway-routes.json" /init/gateway-routes.json

echo "[nacos-seed] done"

