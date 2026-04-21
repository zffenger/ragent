#!/bin/bash
#
# Ragent 后端服务部署脚本
# 用法: ./deploy.sh [start|stop|restart|status|logs]
#

set -e

# ============ 配置区域 ============
APP_NAME="ragent"
APP_PORT=${APP_PORT:-8080}
JAVA_OPTS=${JAVA_OPTS:-"-Xms512m -Xmx1024m"}

# 部署目录（脚本所在目录）
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$(dirname "$DEPLOY_DIR")"

# 路径配置
JAR_FILE="${APP_HOME}/bootstrap/target/bootstrap-0.0.1-SNAPSHOT.jar"
LOG_DIR="${DEPLOY_DIR}/logs"
PID_FILE="${DEPLOY_DIR}/${APP_NAME}.pid"
CONFIG_FILE="${DEPLOY_DIR}/application-prod.yaml"

# ============ 函数定义 ============

check_java() {
    if [ -z "$JAVA_HOME" ]; then
        echo "错误: JAVA_HOME 未设置"
        exit 1
    fi
    if [ ! -x "$JAVA_HOME/bin/java" ]; then
        echo "错误: 找不到 java 可执行文件"
        exit 1
    fi
}

check_jar() {
    if [ ! -f "$JAR_FILE" ]; then
        echo "错误: 找不到 JAR 文件: $JAR_FILE"
        echo "请先执行: mvn clean package -DskipTests"
        exit 1
    fi
}

get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    fi
}

is_running() {
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        if ps -p "$pid" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

start() {
    if is_running; then
        echo "${APP_NAME} 已在运行中，PID: $(get_pid)"
        return 0
    fi

    check_java
    check_jar

    # 创建日志目录
    mkdir -p "$LOG_DIR"

    # 构建启动命令
    local spring_opts=""
    if [ -f "$CONFIG_FILE" ]; then
        spring_opts="--spring.config.location=classpath:/application.yaml,file:${CONFIG_FILE}"
        echo "使用外部配置文件: $CONFIG_FILE"
    fi

    echo "启动 ${APP_NAME}..."
    echo "端口: ${APP_PORT}"
    echo "Java 选项: ${JAVA_OPTS}"

    nohup "$JAVA_HOME/bin/java" $JAVA_OPTS \
        -jar "$JAR_FILE" \
        --server.port="$APP_PORT" \
        $spring_opts \
        > "${LOG_DIR}/console.log" 2>&1 &

    local pid=$!
    echo $pid > "$PID_FILE"

    sleep 3
    if is_running; then
        echo "${APP_NAME} 启动成功，PID: $pid"
        echo "日志目录: $LOG_DIR"
    else
        echo "${APP_NAME} 启动失败，请查看日志: ${LOG_DIR}/console.log"
        exit 1
    fi
}

stop() {
    if ! is_running; then
        echo "${APP_NAME} 未运行"
        [ -f "$PID_FILE" ] && rm -f "$PID_FILE"
        return 0
    fi

    local pid=$(get_pid)
    echo "停止 ${APP_NAME}，PID: $pid"

    kill "$pid" 2>/dev/null

    # 等待进程结束
    local count=0
    while is_running && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
    done

    if is_running; then
        echo "强制终止 ${APP_NAME}"
        kill -9 "$pid" 2>/dev/null
    fi

    rm -f "$PID_FILE"
    echo "${APP_NAME} 已停止"
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if is_running; then
        echo "${APP_NAME} 运行中，PID: $(get_pid)"
        # 检查端口
        if command -v lsof &> /dev/null; then
            lsof -i :"$APP_PORT" 2>/dev/null | head -5
        fi
    else
        echo "${APP_NAME} 未运行"
    fi
}

logs() {
    local log_file="${LOG_DIR}/console.log"
    if [ -f "$log_file" ]; then
        tail -f "$log_file"
    else
        echo "日志文件不存在: $log_file"
    fi
}

usage() {
    echo "用法: $0 {start|stop|restart|status|logs}"
    echo ""
    echo "环境变量:"
    echo "  JAVA_HOME    - Java 安装目录（必需）"
    echo "  APP_PORT     - 应用端口（默认: 8080）"
    echo "  JAVA_OPTS    - JVM 参数（默认: -Xms512m -Xmx1024m）"
    echo ""
    echo "配置文件: deploy/application-prod.yaml"
}

# ============ 主逻辑 ============

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    *)
        usage
        exit 1
        ;;
esac
