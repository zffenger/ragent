import { api } from "@/services/api";

// ==================== 类型定义 ====================

export interface ModelGroupConfig {
  defaultModel?: string | null;
  deepThinkingModel?: string | null;
  candidates: ModelCandidate[];
}

export interface ModelCandidate {
  id?: string | null;
  modelId: string;
  provider: string;
  model: string;
  url?: string | null;
  dimension?: number | null;
  priority?: number | null;
  enabled?: boolean | null;
  supportsThinking?: boolean | null;
  isDefault?: boolean | null;
  isDeepThinking?: boolean | null;
}

export interface ModelCandidateInput {
  id?: string | null;
  modelId: string;
  modelType: "CHAT" | "EMBEDDING" | "RERANK";
  provider: string;
  model: string;
  url?: string | null;
  dimension?: number | null;
  priority?: number | null;
  enabled?: boolean | null;
  supportsThinking?: boolean | null;
}

export interface ModelProvider {
  id?: string | null;
  name: string;
  url?: string | null;
  apiKey?: string | null;
  endpoints?: Record<string, string> | null;
  enabled?: boolean | null;
}

export interface FeishuBotConfig {
  enabled?: boolean | null;
  appId?: string | null;
  appSecret?: string | null;
  encryptKey?: string | null;
  verificationToken?: string | null;
  botName?: string | null;
}

export interface WeWorkBotConfig {
  enabled?: boolean | null;
  corpId?: string | null;
  agentId?: string | null;
  secret?: string | null;
  token?: string | null;
  encodingAesKey?: string | null;
  botName?: string | null;
}

export interface DetectionConfig {
  mode?: string | null;
  keywords?: string[] | null;
  atTriggerEnabled?: boolean | null;
  llmThreshold?: number | null;
}

export interface AnswerConfig {
  mode?: string | null;
  defaultSystemPrompt?: string | null;
  maxTokens?: number | null;
}

// ==================== 模型组配置查询 API ====================

export async function getChatModelConfig(): Promise<ModelGroupConfig> {
  return api.get<ModelGroupConfig, ModelGroupConfig>("/admin/settings/ai/chat");
}

export async function getEmbeddingModelConfig(): Promise<ModelGroupConfig> {
  return api.get<ModelGroupConfig, ModelGroupConfig>("/admin/settings/ai/embedding");
}

export async function getRerankModelConfig(): Promise<ModelGroupConfig> {
  return api.get<ModelGroupConfig, ModelGroupConfig>("/admin/settings/ai/rerank");
}

// ==================== 模型候选 CRUD API ====================

export async function createModelCandidate(candidate: ModelCandidateInput): Promise<ModelCandidate> {
  return api.post<ModelCandidate, ModelCandidate>("/admin/settings/model-candidates", candidate);
}

export async function updateModelCandidate(id: string, candidate: ModelCandidateInput): Promise<ModelCandidate> {
  return api.put<ModelCandidate, ModelCandidate>(`/admin/settings/model-candidates/${id}`, candidate);
}

export async function deleteModelCandidate(id: string): Promise<void> {
  return api.delete<void, void>(`/admin/settings/model-candidates/${id}`);
}

export async function setDefaultModel(id: string, modelType: "CHAT" | "EMBEDDING" | "RERANK"): Promise<void> {
  return api.put<void, void>(`/admin/settings/model-candidates/${id}/default`, { modelType });
}

export async function setDeepThinkingModel(id: string): Promise<void> {
  return api.put<void, void>(`/admin/settings/model-candidates/${id}/deep-thinking`);
}

// ==================== 模型提供商 API ====================

export interface SupportedProvider {
  id: string;
  name: string;
}

export async function getSupportedProviders(): Promise<SupportedProvider[]> {
  return api.get<SupportedProvider[], SupportedProvider[]>("/admin/settings/supported-providers");
}

export async function listModelProviders(): Promise<ModelProvider[]> {
  return api.get<ModelProvider[], ModelProvider[]>("/admin/settings/model-providers");
}

export async function createModelProvider(provider: ModelProvider): Promise<ModelProvider> {
  return api.post<ModelProvider, ModelProvider>("/admin/settings/model-providers", provider);
}

export async function updateModelProvider(id: string, provider: ModelProvider): Promise<ModelProvider> {
  return api.put<ModelProvider, ModelProvider>(`/admin/settings/model-providers/${id}`, provider);
}

export async function deleteModelProvider(id: string): Promise<void> {
  return api.delete<void, void>(`/admin/settings/model-providers/${id}`);
}

// ==================== 机器人配置 API ====================

export async function getFeishuBotConfig(): Promise<FeishuBotConfig> {
  return api.get<FeishuBotConfig, FeishuBotConfig>("/admin/settings/chatbot/feishu");
}

export async function updateFeishuBotConfig(config: FeishuBotConfig): Promise<void> {
  return api.put<void, void>("/admin/settings/chatbot/feishu", config);
}

export async function getWeWorkBotConfig(): Promise<WeWorkBotConfig> {
  return api.get<WeWorkBotConfig, WeWorkBotConfig>("/admin/settings/chatbot/wework");
}

export async function updateWeWorkBotConfig(config: WeWorkBotConfig): Promise<void> {
  return api.put<void, void>("/admin/settings/chatbot/wework", config);
}

export async function getDetectionConfig(): Promise<DetectionConfig> {
  return api.get<DetectionConfig, DetectionConfig>("/admin/settings/chatbot/detection");
}

export async function updateDetectionConfig(config: DetectionConfig): Promise<void> {
  return api.put<void, void>("/admin/settings/chatbot/detection", config);
}

export async function getAnswerConfig(): Promise<AnswerConfig> {
  return api.get<AnswerConfig, AnswerConfig>("/admin/settings/chatbot/answer");
}

export async function updateAnswerConfig(config: AnswerConfig): Promise<void> {
  return api.put<void, void>("/admin/settings/chatbot/answer", config);
}

export async function refreshConfigCache(): Promise<void> {
  return api.post<void, void>("/admin/settings/refresh-cache");
}
