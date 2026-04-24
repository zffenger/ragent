import { useEffect, useState } from "react";
import { toast } from "sonner";
import {
  getChatModelConfig,
  getEmbeddingModelConfig,
  getRerankModelConfig,
  listModelProviders,
  setDefaultModel,
  setDeepThinkingModel,
  getSupportedProviders,
  type ModelGroupConfig,
  type ModelProvider,
  type SupportedProvider,
} from "@/services/modelConfigService";
import { getErrorMessage } from "@/utils/error";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ModelProviderDialog } from "./ModelProviderDialog";
import { ModelCandidateDialog } from "./ModelCandidateDialog";

export function ModelSettingsPage() {
  const [chatConfig, setChatConfig] = useState<ModelGroupConfig | null>(null);
  const [embeddingConfig, setEmbeddingConfig] = useState<ModelGroupConfig | null>(null);
  const [rerankConfig, setRerankConfig] = useState<ModelGroupConfig | null>(null);
  const [providers, setProviders] = useState<ModelProvider[]>([]);
  const [supportedProviders, setSupportedProviders] = useState<SupportedProvider[]>([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    try {
      setLoading(true);
      const [chat, embedding, rerank, provs, supported] = await Promise.all([
        getChatModelConfig(),
        getEmbeddingModelConfig(),
        getRerankModelConfig(),
        listModelProviders(),
        getSupportedProviders(),
      ]);
      setChatConfig(chat);
      setEmbeddingConfig(embedding);
      setRerankConfig(rerank);
      setProviders(provs);
      setSupportedProviders(supported);
    } catch (error) {
      toast.error(getErrorMessage(error, "加载模型配置失败"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSetDefault = async (candidateId: string, modelType: "CHAT" | "EMBEDDING" | "RERANK") => {
    try {
      await setDefaultModel(candidateId, modelType);
      toast.success("已设置为默认模型");
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "设置默认模型失败"));
    }
  };

  const handleSetDeepThinking = async (candidateId: string) => {
    try {
      await setDeepThinkingModel(candidateId);
      toast.success("已设置为深度思考模型");
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "设置深度思考模型失败"));
    }
  };

  if (loading) {
    return (
      <div className="admin-page">
        <div className="text-sm text-muted-foreground">加载中...</div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">模型配置</h1>
          <p className="admin-page-subtitle">管理 Chat、Embedding、Rerank 模型配置</p>
        </div>
        <div className="flex gap-2">
          <ModelProviderDialog
            providers={providers}
            onRefresh={loadData}
          />
        </div>
      </div>

      <Tabs defaultValue="chat" className="space-y-4">
        <TabsList>
          <TabsTrigger value="chat">Chat 模型</TabsTrigger>
          <TabsTrigger value="embedding">Embedding 模型</TabsTrigger>
          <TabsTrigger value="rerank">Rerank 模型</TabsTrigger>
        </TabsList>

        <TabsContent value="chat">
          <ModelConfigCard
            title="Chat 模型配置"
            description="用于对话生成的模型列表"
            config={chatConfig}
            modelType="CHAT"
            supportedProviders={supportedProviders}
            onSetDefault={handleSetDefault}
            onSetDeepThinking={handleSetDeepThinking}
            onRefresh={loadData}
          />
        </TabsContent>

        <TabsContent value="embedding">
          <ModelConfigCard
            title="Embedding 模型配置"
            description="用于向量嵌入的模型列表"
            config={embeddingConfig}
            modelType="EMBEDDING"
            supportedProviders={supportedProviders}
            showDimension
            onSetDefault={handleSetDefault}
            onRefresh={loadData}
          />
        </TabsContent>

        <TabsContent value="rerank">
          <ModelConfigCard
            title="Rerank 模型配置"
            description="用于重排序的模型列表"
            config={rerankConfig}
            modelType="RERANK"
            supportedProviders={supportedProviders}
            onSetDefault={handleSetDefault}
            onRefresh={loadData}
          />
        </TabsContent>
      </Tabs>

      <Card>
        <CardHeader>
          <CardTitle>模型提供商</CardTitle>
          <CardDescription>管理 AI 模型提供商的连接配置</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>名称</TableHead>
                <TableHead>URL</TableHead>
                <TableHead>API Key</TableHead>
                <TableHead>端点</TableHead>
                <TableHead>状态</TableHead>
                <TableHead className="w-[120px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {providers.map((provider) => (
                <TableRow key={provider.id}>
                  <TableCell className="font-medium">{provider.name}</TableCell>
                  <TableCell className="max-w-[200px] truncate">{provider.url || "-"}</TableCell>
                  <TableCell>{provider.apiKey ? "已配置" : "-"}</TableCell>
                  <TableCell className="max-w-[200px]">
                    {provider.endpoints && Object.keys(provider.endpoints).length > 0 ? (
                      <div className="flex flex-wrap gap-1">
                        {Object.entries(provider.endpoints).map(([key, value]) => (
                          <Badge key={key} variant="secondary" className="text-xs">
                            {key}: {value}
                          </Badge>
                        ))}
                      </div>
                    ) : "-"}
                  </TableCell>
                  <TableCell>
                    <Badge variant={provider.enabled ? "default" : "outline"}>
                      {provider.enabled ? "启用" : "禁用"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <ModelProviderDialog
                      provider={provider}
                      providers={providers}
                      onRefresh={loadData}
                    />
                  </TableCell>
                </TableRow>
              ))}
              {providers.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    暂无提供商，点击上方按钮添加
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}

// 模型配置卡片组件
function ModelConfigCard({
  title,
  description,
  config,
  modelType,
  supportedProviders,
  showDimension = false,
  showDeepThinking = true,
  onSetDefault,
  onSetDeepThinking,
  onRefresh,
}: {
  title: string;
  description: string;
  config: ModelGroupConfig | null;
  modelType: "CHAT" | "EMBEDDING" | "RERANK";
  supportedProviders: SupportedProvider[];
  showDimension?: boolean;
  showDeepThinking?: boolean;
  onSetDefault: (id: string, type: "CHAT" | "EMBEDDING" | "RERANK") => void;
  onSetDeepThinking?: (id: string) => void;
  onRefresh: () => void;
}) {
  const candidates = config?.candidates || [];

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <div>
          <CardTitle>{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </div>
        <ModelCandidateDialog
          modelType={modelType}
          supportedProviders={supportedProviders}
          onRefresh={onRefresh}
        />
      </CardHeader>
      <CardContent>
        <div className="mb-4 flex gap-4 text-sm">
          <div>
            <span className="text-muted-foreground">默认模型：</span>
            <span className="font-medium">{config?.defaultModel || "-"}</span>
          </div>
          {showDeepThinking && (
            <div>
              <span className="text-muted-foreground">深度思考：</span>
              <span className="font-medium">{config?.deepThinkingModel || "-"}</span>
            </div>
          )}
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>模型 ID</TableHead>
              <TableHead>提供商</TableHead>
              <TableHead>模型名称</TableHead>
              {showDimension && <TableHead>维度</TableHead>}
              <TableHead>优先级</TableHead>
              <TableHead>状态</TableHead>
              <TableHead className="w-[180px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {candidates.map((item) => (
              <TableRow key={item.id}>
                <TableCell className="font-medium">
                  {item.modelId}
                  {item.isDefault && (
                    <Badge variant="secondary" className="ml-2">默认</Badge>
                  )}
                  {item.isDeepThinking && (
                    <Badge variant="outline" className="ml-2">深度思考</Badge>
                  )}
                </TableCell>
                <TableCell>{item.provider}</TableCell>
                <TableCell>{item.model}</TableCell>
                {showDimension && <TableCell>{item.dimension || "-"}</TableCell>}
                <TableCell>{item.priority}</TableCell>
                <TableCell>
                  <Badge variant={item.enabled ? "default" : "outline"}>
                    {item.enabled ? "启用" : "禁用"}
                  </Badge>
                </TableCell>
                <TableCell>
                  <div className="flex gap-1">
                    {!item.isDefault && item.id && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => onSetDefault(item.id!, modelType)}
                      >
                        设为默认
                      </Button>
                    )}
                    {showDeepThinking && onSetDeepThinking && !item.isDeepThinking && item.id && modelType === "CHAT" && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => onSetDeepThinking(item.id!)}
                      >
                        深度思考
                      </Button>
                    )}
                    <ModelCandidateDialog
                      candidate={item}
                      modelType={modelType}
                      supportedProviders={supportedProviders}
                      onRefresh={onRefresh}
                    />
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {candidates.length === 0 && (
              <TableRow>
                <TableCell colSpan={showDimension ? 7 : 6} className="text-center text-muted-foreground">
                  暂无模型候选，点击上方按钮添加
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
