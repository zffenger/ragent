import { api } from "@/services/api";

// ==================== 类型定义 ====================

export interface KnowledgeBrief {
  id: string;
  name: string;
}

export interface RetrievalDomain {
  id?: string;
  name: string;
  description?: string | null;
  enabled?: boolean;
  knowledgeIds?: string[];
  knowledges?: KnowledgeBrief[];
  botCount?: number;
}

export interface ChatBot {
  id?: string;
  name: string;
  platform: "FEISHU" | "WEWORK";
  description?: string | null;
  // 飞书配置
  appId?: string | null;
  appSecret?: string | null;
  encryptKey?: string | null;
  verificationToken?: string | null;
  // 企微配置
  corpId?: string | null;
  agentId?: string | null;
  token?: string | null;
  encodingAesKey?: string | null;
  // 通用配置
  botName?: string | null;
  // 检索域绑定
  domainId?: string | null;
  domainName?: string | null;
  // 检测配置
  detectionMode?: "KEYWORD" | "LLM" | "COMPOSITE";
  detectionKeywords?: string[];
  atTriggerEnabled?: boolean;
  llmThreshold?: number;
  // 回答配置
  answerMode?: "LLM" | "RAG";
  systemPrompt?: string | null;
  maxTokens?: number;
  // 状态
  enabled?: boolean;
}

// ==================== 检索域 API ====================

export async function listRetrievalDomains(): Promise<RetrievalDomain[]> {
  return api.get<RetrievalDomain[], RetrievalDomain[]>("/admin/settings/retrieval-domains");
}

export async function getRetrievalDomain(id: string): Promise<RetrievalDomain> {
  return api.get<RetrievalDomain, RetrievalDomain>(`/admin/settings/retrieval-domains/${id}`);
}

export async function createRetrievalDomain(domain: RetrievalDomain): Promise<RetrievalDomain> {
  return api.post<RetrievalDomain, RetrievalDomain>("/admin/settings/retrieval-domains", domain);
}

export async function updateRetrievalDomain(id: string, domain: RetrievalDomain): Promise<RetrievalDomain> {
  return api.put<RetrievalDomain, RetrievalDomain>(`/admin/settings/retrieval-domains/${id}`, domain);
}

export async function deleteRetrievalDomain(id: string): Promise<void> {
  return api.delete<void, void>(`/admin/settings/retrieval-domains/${id}`);
}

export async function bindKnowledgesToDomain(domainId: string, knowledgeIds: string[]): Promise<void> {
  return api.post<void, void>(`/admin/settings/retrieval-domains/${domainId}/knowledges`, { knowledgeIds });
}

export async function unbindKnowledgeFromDomain(domainId: string, knowledgeId: string): Promise<void> {
  return api.delete<void, void>(`/admin/settings/retrieval-domains/${domainId}/knowledges/${knowledgeId}`);
}

// ==================== 聊天机器人 API ====================

export async function listChatBots(platform?: string): Promise<ChatBot[]> {
  const url = platform
    ? `/admin/settings/chat-bots?platform=${platform}`
    : "/admin/settings/chat-bots";
  return api.get<ChatBot[], ChatBot[]>(url);
}

export async function getChatBot(id: string): Promise<ChatBot> {
  return api.get<ChatBot, ChatBot>(`/admin/settings/chat-bots/${id}`);
}

export async function createChatBot(bot: ChatBot): Promise<ChatBot> {
  return api.post<ChatBot, ChatBot>("/admin/settings/chat-bots", bot);
}

export async function updateChatBot(id: string, bot: ChatBot): Promise<ChatBot> {
  return api.put<ChatBot, ChatBot>(`/admin/settings/chat-bots/${id}`, bot);
}

export async function deleteChatBot(id: string): Promise<void> {
  return api.delete<void, void>(`/admin/settings/chat-bots/${id}`);
}

export async function setChatBotEnabled(id: string, enabled: boolean): Promise<void> {
  return api.put<void, void>(`/admin/settings/chat-bots/${id}/enabled`, { enabled });
}

export async function bindDomainToBot(botId: string, domainId: string | null): Promise<void> {
  return api.put<void, void>(`/admin/settings/chat-bots/${botId}/domain`, { domainId });
}
