## 目标

- CI 构建镜像并推送到阿里云 ACR
- 虚拟机通过 `docker compose pull` 拉取镜像完成本地部署（不在 VM 上 build）

## 1. 准备阿里云 ACR 信息

你需要 4 个参数（放到 GitHub 仓库 Secrets 和虚拟机 `.env`）：

- **ACR_REGISTRY**：镜像仓库域名（不带协议）
  - 新版个人版示例：`crpi-xxxx.cn-hangzhou.personal.cr.aliyuncs.com`
  - 旧版个人版示例：`registry.cn-hangzhou.aliyuncs.com`
  - 企业版示例：`<实例名>-registry.cn-hangzhou.cr.aliyuncs.com`
- **ACR_NAMESPACE**：命名空间（例如 `hmall`）
- **ACR_USERNAME**：镜像仓库登录用户名（通常是你的阿里云账号/子账号名或镜像仓库用户名）
- **ACR_PASSWORD**：镜像仓库密码（建议用“访问凭证/密码”，不要用主账号密码）

## 2. GitHub Actions（CI）推送到 ACR

工作流文件：`.github/workflows/docker-publish.yml`

在 GitHub 仓库配置 Secrets：

- `ACR_REGISTRY`
- `ACR_NAMESPACE`
- `ACR_USERNAME`
- `ACR_PASSWORD`

触发方式：

- **手动触发**：Actions → Publish Docker images → Run workflow，输入 `tag`（如 `latest` 或 `v1.0.0`）
- **自动触发**：当 CI 工作流成功后自动触发（main/master）

镜像命名规则：

- `${ACR_REGISTRY}/${ACR_NAMESPACE}/hmall-<service>:<tag>`
- 例如：`${ACR_REGISTRY}/${ACR_NAMESPACE}/hmall-item-service:latest`

## 3. 虚拟机部署（拉取镜像）

### 3.1 在虚拟机准备代码与 `.env`

把仓库拷到虚拟机（或仅拷 `docker-compose.yml` + `docker/` 目录也行）。

在 `docker-compose.yml` 同目录创建 `.env`（示例）：

```bash
ACR_REGISTRY=crpi-xxxx.cn-hangzhou.personal.cr.aliyuncs.com
ACR_NAMESPACE=hmall
TAG=latest

# 下面是 compose 里会用到的（按需改）
MYSQL_ROOT_PASSWORD=123
REDIS_PASSWORD=123456
RABBITMQ_USER=sail
RABBITMQ_PASSWORD=123
RABBITMQ_VHOST=/hmall
```

### 3.2 登录 ACR 并拉取/启动

先登录（只需要一次；token/凭证过期再登录）：

```bash
docker login "${ACR_REGISTRY}" -u "<ACR_USERNAME>" -p "<ACR_PASSWORD>"
```

拉取并启动（重点是 **不 build**）：

```bash
docker compose pull
docker compose up -d --no-build
```

更新到新版本（改 `.env` 的 `TAG` 或继续用 `latest`）：

```bash
docker compose pull
docker compose up -d --no-build
```

## 4. CD：GitHub Actions 通过 SSH 部署到虚拟机

仓库已提供工作流：`.github/workflows/deploy-vm.yml`

### 4.1 需要配置的 GitHub Secrets

- **ACR_REGISTRY / ACR_NAMESPACE / ACR_USERNAME / ACR_PASSWORD**：同上
- **VM_HOST**：虚拟机公网 IP 或域名
- **VM_USER**：SSH 用户名（例如 `root`）
- **VM_SSH_KEY**：用于登录 VM 的私钥（建议新建专用部署 key）
- **VM_SSH_PORT**：可选，默认 22
- **VM_APP_DIR**：虚拟机上项目目录（例如 `/opt/hmall`，里面应包含 `docker-compose.yml`）
- **VM_GIT_REF**：可选，默认 `main`（你也可以填 `master`）

### 4.2 虚拟机侧一次性准备

```bash
# 1) 安装 docker / docker compose（略）
# 2) 准备目录并 clone 仓库（确保 docker-compose.yml 在该目录）
mkdir -p /opt/hmall
cd /opt/hmall
git clone <你的仓库地址> .
```

### 4.3 触发方式

- **自动触发**：当 “Publish Docker images” 工作流成功后，会自动触发部署（默认部署 `head_sha` 标签）
- **手动触发**：Actions → Deploy to VM (pull from ACR) → Run workflow，可指定 `tag=latest` 或某个 SHA tag

### 3.3 常用排查

```bash
docker compose ps
docker compose logs -f hm-gateway
```

