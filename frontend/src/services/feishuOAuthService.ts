import { api } from "@/services/api";

// ==================== 类型定义 ====================

export interface FeishuUser {
  openId: string;
  userId?: string;
  name?: string;
  avatar?: string;
  bound?: boolean;
  username?: string;
}

export interface FeishuOAuthConfig {
  enabled: boolean;
  appId?: string;
}

// ==================== 飞书 OAuth API ====================

/**
 * 获取飞书 OAuth 授权 URL
 */
export async function getFeishuAuthorizeUrl(redirectUri: string, state?: string): Promise<string> {
  const params = new URLSearchParams({ redirectUri });
  if (state) {
    params.append("state", state);
  }
  const response = await api.get<string, string>(`/auth/feishu/authorize-url?${params.toString()}`);
  return response;
}

/**
 * 飞书 OAuth 回调登录
 */
export async function feishuCallback(code: string, state?: string): Promise<import("./authService").LoginResult> {
  return api.post("/auth/feishu/callback", { code, state });
}

/**
 * 绑定飞书账号
 */
export async function bindFeishuAccount(code: string, state?: string): Promise<void> {
  return api.post("/auth/feishu/bind", { code, state });
}

/**
 * 解绑飞书账号
 */
export async function unbindFeishuAccount(): Promise<void> {
  return api.delete("/auth/feishu/bind");
}

/**
 * 获取当前用户的飞书绑定信息
 */
export async function getFeishuBinding(): Promise<FeishuUser | null> {
  return api.get<FeishuUser | null, FeishuUser | null>("/auth/feishu/binding");
}
