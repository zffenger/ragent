import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  getFeishuBotConfig,
  getWeWorkBotConfig,
  getDetectionConfig,
  getAnswerConfig,
  updateFeishuBotConfig,
  updateWeWorkBotConfig,
  updateDetectionConfig,
  updateAnswerConfig,
  refreshConfigCache,
  type FeishuBotConfig,
  type WeWorkBotConfig,
  type DetectionConfig,
  type AnswerConfig,
} from "@/services/modelConfigService";
import { getErrorMessage } from "@/utils/error";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { RefreshCw } from "lucide-react";

// 飞书配置表单 Schema
const feishuFormSchema = z.object({
  enabled: z.boolean(),
  appId: z.string().max(100).optional().or(z.literal("")),
  appSecret: z.string().max(200).optional().or(z.literal("")),
  encryptKey: z.string().max(200).optional().or(z.literal("")),
  verificationToken: z.string().max(200).optional().or(z.literal("")),
  botName: z.string().max(50).optional().or(z.literal("")),
});

// 企微配置表单 Schema
const weworkFormSchema = z.object({
  enabled: z.boolean(),
  corpId: z.string().max(100).optional().or(z.literal("")),
  agentId: z.string().max(100).optional().or(z.literal("")),
  secret: z.string().max(200).optional().or(z.literal("")),
  token: z.string().max(200).optional().or(z.literal("")),
  encodingAesKey: z.string().max(100).optional().or(z.literal("")),
  botName: z.string().max(50).optional().or(z.literal("")),
});

// 问题检测配置表单 Schema
const detectionFormSchema = z.object({
  mode: z.enum(["KEYWORD", "LLM", "COMPOSITE"]),
  keywords: z.string().optional(),
  atTriggerEnabled: z.boolean(),
  llmThreshold: z.number().min(0).max(1),
});

// 回答生成配置表单 Schema
const answerFormSchema = z.object({
  mode: z.enum(["LLM", "RAG"]),
  defaultSystemPrompt: z.string().max(2000).optional().or(z.literal("")),
  maxTokens: z.number().int().min(100).max(10000),
});

type FeishuFormValues = z.infer<typeof feishuFormSchema>;
type WeWorkFormValues = z.infer<typeof weworkFormSchema>;
type DetectionFormValues = z.infer<typeof detectionFormSchema>;
type AnswerFormValues = z.infer<typeof answerFormSchema>;

export function BotSettingsPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);

  // 飞书配置
  const feishuForm = useForm<FeishuFormValues>({
    resolver: zodResolver(feishuFormSchema),
    defaultValues: {
      enabled: false,
      appId: "",
      appSecret: "",
      encryptKey: "",
      verificationToken: "",
      botName: "智能助手",
    },
  });

  // 企微配置
  const weworkForm = useForm<WeWorkFormValues>({
    resolver: zodResolver(weworkFormSchema),
    defaultValues: {
      enabled: false,
      corpId: "",
      agentId: "",
      secret: "",
      token: "",
      encodingAesKey: "",
      botName: "智能助手",
    },
  });

  // 问题检测配置
  const detectionForm = useForm<DetectionFormValues>({
    resolver: zodResolver(detectionFormSchema),
    defaultValues: {
      mode: "COMPOSITE",
      keywords: "?,？,请,怎么,如何,什么,为什么,能不能,可以吗,吗",
      atTriggerEnabled: true,
      llmThreshold: 0.7,
    },
  });

  // 回答生成配置
  const answerForm = useForm<AnswerFormValues>({
    resolver: zodResolver(answerFormSchema),
    defaultValues: {
      mode: "RAG",
      defaultSystemPrompt: "你是一个智能客服助手，请简洁准确地回答用户问题。",
      maxTokens: 2000,
    },
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const [feishu, wework, detection, answer] = await Promise.all([
        getFeishuBotConfig(),
        getWeWorkBotConfig(),
        getDetectionConfig(),
        getAnswerConfig(),
      ]);

      feishuForm.reset({
        enabled: feishu.enabled ?? false,
        appId: feishu.appId || "",
        appSecret: feishu.appSecret || "",
        encryptKey: feishu.encryptKey || "",
        verificationToken: feishu.verificationToken || "",
        botName: feishu.botName || "智能助手",
      });

      weworkForm.reset({
        enabled: wework.enabled ?? false,
        corpId: wework.corpId || "",
        agentId: wework.agentId || "",
        secret: wework.secret || "",
        token: wework.token || "",
        encodingAesKey: wework.encodingAesKey || "",
        botName: wework.botName || "智能助手",
      });

      detectionForm.reset({
        mode: (detection.mode as "KEYWORD" | "LLM" | "COMPOSITE") || "COMPOSITE",
        keywords: detection.keywords?.join(",") || "?,？,请,怎么,如何,什么,为什么,能不能,可以吗,吗",
        atTriggerEnabled: detection.atTriggerEnabled ?? true,
        llmThreshold: detection.llmThreshold ?? 0.7,
      });

      answerForm.reset({
        mode: (answer.mode as "LLM" | "RAG") || "RAG",
        defaultSystemPrompt: answer.defaultSystemPrompt || "",
        maxTokens: answer.maxTokens ?? 2000,
      });
    } catch (error) {
      toast.error(getErrorMessage(error, "加载配置失败"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSaveFeishu = async (values: FeishuFormValues) => {
    try {
      setSaving("feishu");
      await updateFeishuBotConfig({
        ...values,
        keywords: values.keywords ? values.keywords.split(",").map((k) => k.trim()) : [],
      });
      toast.success("飞书配置已保存");
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setSaving(null);
    }
  };

  const handleSaveWeWork = async (values: WeWorkFormValues) => {
    try {
      setSaving("wework");
      await updateWeWorkBotConfig(values);
      toast.success("企微配置已保存");
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setSaving(null);
    }
  };

  const handleSaveDetection = async (values: DetectionFormValues) => {
    try {
      setSaving("detection");
      await updateDetectionConfig({
        ...values,
        keywords: values.keywords ? values.keywords.split(",").map((k) => k.trim()) : [],
      });
      toast.success("问题检测配置已保存");
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setSaving(null);
    }
  };

  const handleSaveAnswer = async (values: AnswerFormValues) => {
    try {
      setSaving("answer");
      await updateAnswerConfig(values);
      toast.success("回答生成配置已保存");
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setSaving(null);
    }
  };

  const handleRefreshCache = async () => {
    try {
      await refreshConfigCache();
      toast.success("配置缓存已刷新");
    } catch (error) {
      toast.error(getErrorMessage(error, "刷新失败"));
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
          <h1 className="admin-page-title">机器人配置</h1>
          <p className="admin-page-subtitle">管理飞书和企微机器人配置</p>
        </div>
        <Button variant="outline" size="sm" onClick={handleRefreshCache}>
          <RefreshCw className="mr-2 h-4 w-4" />
          刷新缓存
        </Button>
      </div>

      <Tabs defaultValue="feishu" className="space-y-4">
        <TabsList>
          <TabsTrigger value="feishu">飞书机器人</TabsTrigger>
          <TabsTrigger value="wework">企微机器人</TabsTrigger>
          <TabsTrigger value="detection">问题检测</TabsTrigger>
          <TabsTrigger value="answer">回答生成</TabsTrigger>
        </TabsList>

        <TabsContent value="feishu">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>飞书机器人配置</CardTitle>
                  <CardDescription>配置飞书机器人的连接参数</CardDescription>
                </div>
                <Badge variant={feishuForm.watch("enabled") ? "default" : "outline"}>
                  {feishuForm.watch("enabled") ? "已启用" : "未启用"}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <Form {...feishuForm}>
                <form onSubmit={feishuForm.handleSubmit(handleSaveFeishu)} className="space-y-4">
                  <FormField
                    control={feishuForm.control}
                    name="enabled"
                    render={({ field }) => (
                      <FormItem className="flex items-center justify-between rounded-lg border p-4">
                        <div>
                          <FormLabel>启用飞书机器人</FormLabel>
                          <FormDescription>开启后将自动接收飞书消息</FormDescription>
                        </div>
                        <FormControl>
                          <Switch checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={feishuForm.control}
                      name="appId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>App ID</FormLabel>
                          <FormControl>
                            <Input placeholder="cli_xxx" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={feishuForm.control}
                      name="appSecret"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>App Secret</FormLabel>
                          <FormControl>
                            <Input type="password" placeholder="应用密钥" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={feishuForm.control}
                      name="verificationToken"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>验证令牌（可选）</FormLabel>
                          <FormControl>
                            <Input placeholder="用于签名验证" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={feishuForm.control}
                      name="encryptKey"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>加密密钥（可选）</FormLabel>
                          <FormControl>
                            <Input placeholder="用于消息加密" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <FormField
                    control={feishuForm.control}
                    name="botName"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>机器人名称</FormLabel>
                        <FormControl>
                          <Input placeholder="智能助手" {...field} />
                        </FormControl>
                        <FormDescription>用于识别 @ 事件</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="flex justify-end">
                    <Button type="submit" disabled={saving === "feishu"}>
                      {saving === "feishu" ? "保存中..." : "保存配置"}
                    </Button>
                  </div>
                </form>
              </Form>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="wework">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>企微机器人配置</CardTitle>
                  <CardDescription>配置企业微信机器人的连接参数</CardDescription>
                </div>
                <Badge variant={weworkForm.watch("enabled") ? "default" : "outline"}>
                  {weworkForm.watch("enabled") ? "已启用" : "未启用"}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <Form {...weworkForm}>
                <form onSubmit={weworkForm.handleSubmit(handleSaveWeWork)} className="space-y-4">
                  <FormField
                    control={weworkForm.control}
                    name="enabled"
                    render={({ field }) => (
                      <FormItem className="flex items-center justify-between rounded-lg border p-4">
                        <div>
                          <FormLabel>启用企微机器人</FormLabel>
                          <FormDescription>开启后将自动接收企微消息</FormDescription>
                        </div>
                        <FormControl>
                          <Switch checked={field.value} onCheckedChange={field.onChange} />
                        </FormControl>
                      </FormItem>
                    )}
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={weworkForm.control}
                      name="corpId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>企业 ID</FormLabel>
                          <FormControl>
                            <Input placeholder="wwxxx" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={weworkForm.control}
                      name="agentId"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Agent ID</FormLabel>
                          <FormControl>
                            <Input placeholder="应用ID" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={weworkForm.control}
                      name="secret"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>应用 Secret</FormLabel>
                          <FormControl>
                            <Input type="password" placeholder="应用密钥" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={weworkForm.control}
                      name="token"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>回调 Token</FormLabel>
                          <FormControl>
                            <Input placeholder="回调配置的Token" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={weworkForm.control}
                      name="encodingAesKey"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>EncodingAESKey</FormLabel>
                          <FormControl>
                            <Input placeholder="消息加密密钥" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={weworkForm.control}
                      name="botName"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>机器人名称</FormLabel>
                          <FormControl>
                            <Input placeholder="智能助手" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <div className="flex justify-end">
                    <Button type="submit" disabled={saving === "wework"}>
                      {saving === "wework" ? "保存中..." : "保存配置"}
                    </Button>
                  </div>
                </form>
              </Form>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="detection">
          <Card>
            <CardHeader>
              <CardTitle>问题检测配置</CardTitle>
              <CardDescription>配置群聊中如何识别用户提问</CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...detectionForm}>
                <form onSubmit={detectionForm.handleSubmit(handleSaveDetection)} className="space-y-4">
                  <FormField
                    control={detectionForm.control}
                    name="mode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>检测模式</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="KEYWORD">仅关键词检测</SelectItem>
                            <SelectItem value="LLM">仅 LLM 语义检测</SelectItem>
                            <SelectItem value="COMPOSITE">组合模式（推荐）</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormDescription>
                          KEYWORD: 快速但可能误判；LLM: 准确但响应慢；COMPOSITE: 先关键词后LLM
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={detectionForm.control}
                    name="keywords"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>触发关键词</FormLabel>
                        <FormControl>
                          <Input placeholder="?,？,请,怎么,如何" {...field} />
                        </FormControl>
                        <FormDescription>多个关键词用逗号分隔</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={detectionForm.control}
                      name="atTriggerEnabled"
                      render={({ field }) => (
                        <FormItem className="flex items-center justify-between rounded-lg border p-4">
                          <div>
                            <FormLabel>@机器人触发</FormLabel>
                            <FormDescription>@机器人时直接触发回答</FormDescription>
                          </div>
                          <FormControl>
                            <Switch checked={field.value} onCheckedChange={field.onChange} />
                          </FormControl>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={detectionForm.control}
                      name="llmThreshold"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>LLM 置信度阈值</FormLabel>
                          <FormControl>
                            <Input
                              type="number"
                              step="0.1"
                              min="0"
                              max="1"
                              {...field}
                              onChange={(e) => field.onChange(parseFloat(e.target.value) || 0.7)}
                            />
                          </FormControl>
                          <FormDescription>0-1 之间，越高越严格</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                  <div className="flex justify-end">
                    <Button type="submit" disabled={saving === "detection"}>
                      {saving === "detection" ? "保存中..." : "保存配置"}
                    </Button>
                  </div>
                </form>
              </Form>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="answer">
          <Card>
            <CardHeader>
              <CardTitle>回答生成配置</CardTitle>
              <CardDescription>配置机器人如何生成回答</CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...answerForm}>
                <form onSubmit={answerForm.handleSubmit(handleSaveAnswer)} className="space-y-4">
                  <FormField
                    control={answerForm.control}
                    name="mode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>回答生成模式</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="LLM">直接 LLM 回答</SelectItem>
                            <SelectItem value="RAG">RAG 检索增强（推荐）</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormDescription>
                          LLM: 直接调用大模型；RAG: 先检索知识库再生成答案
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={answerForm.control}
                    name="defaultSystemPrompt"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>系统提示词（LLM 模式）</FormLabel>
                        <FormControl>
                          <Input placeholder="你是一个智能客服助手..." {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={answerForm.control}
                    name="maxTokens"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>最大 Token 数</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min={100}
                            max={10000}
                            {...field}
                            onChange={(e) => field.onChange(parseInt(e.target.value) || 2000)}
                          />
                        </FormControl>
                        <FormDescription>限制回答的最大长度</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="flex justify-end">
                    <Button type="submit" disabled={saving === "answer"}>
                      {saving === "answer" ? "保存中..." : "保存配置"}
                    </Button>
                  </div>
                </form>
              </Form>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
