import { api } from "@/services/api";

export interface SystemSettings {
  upload: {
    maxFileSize: number;
    maxRequestSize: number;
  };
  rag: {
    default: {
      collectionName: string;
      dimension: number;
      metricType: string;
    };
    queryRewrite: {
      enabled: boolean;
    };
    rateLimit: {
      global: {
        enabled: boolean;
        maxConcurrent: number;
        maxWaitSeconds: number;
        leaseSeconds: number;
        pollIntervalMs: number;
      };
    };
    memory: {
      historyKeepTurns: number;
      summaryStartTurns: number;
      summaryEnabled: boolean;
      summaryMaxChars: number;
      titleMaxLength: number;
    };
  };
  ai: {
    selection: {
      failureThreshold: number;
      openDurationMs: number;
    };
    stream: {
      messageChunkSize: number;
    };
  };
}

export async function getSystemSettings(): Promise<SystemSettings> {
  return api.get<SystemSettings, SystemSettings>("/rag/settings");
}
