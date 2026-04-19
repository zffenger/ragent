import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  listChatBots,
  createChatBot,
  updateChatBot,
  deleteChatBot,
  setChatBotEnabled,
  type ChatBot,
} from "@/services/chatbotService";
import { listRetrievalDomains, type RetrievalDomain } from "@/services/chatbotService";
import { getErrorMessage } from "@/utils/error";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import {
  Plus,
  MoreHorizontal,
  Pencil,
  Trash2,
  Bot,
  MessageSquare,
} from "lucide-react";

// 表单 Schema
const botFormSchema = z.object({
  name: z.string().min(1, "名称不能为空").max(100),
  platform: z.enum(["FEISHU", "WEWORK"]),
  description: z.string().max(500).optional().or(z.literal("")),
  // 飞书配置
  appId: z.string().max(100).optional().or(z.literal("")),
  appSecret: z.string().max(200).optional().or(z.literal("")),
  encryptKey: z.string().max(200).optional().or(z.literal("")),
  verificationToken: z.string().max(200).optional().or(z.literal("")),
  // 企微配置
  corpId: z.string().max(100).optional().or(z.literal("")),
  agentId: z.string().max(100).optional().or(z.literal("")),
  token: z.string().max(200).optional().or(z.literal("")),
  encodingAesKey: z.string().max(100).optional().or(z.literal("")),
  // 通用配置
  botName: z.string().max(50).optional().or(z.literal("")),
  // 检索域绑定
  domainId: z.string().optional().nullable(),
  // 检测配置
  detectionMode: z.enum(["KEYWORD", "LLM", "COMPOSITE"]),
  detectionKeywords: z.string().optional(),
  atTriggerEnabled: z.boolean(),
  llmThreshold: z.number().min(0).max(1),
  // 回答配置
  answerMode: z.enum(["LLM", "RAG"]),
  systemPrompt: z.string().max(2000).optional().or(z.literal("")),
  maxTokens: z.number().int().min(100).max(10000),
  // 状态
  enabled: z.boolean(),
});

type BotFormValues = z.infer<typeof botFormSchema>;

export function ChatBotManagePage() {
  const [loading, setLoading] = useState(true);
  const [bots, setBots] = useState<ChatBot[]>([]);
  const [domains, setDomains] = useState<RetrievalDomain[]>([]);

  // 弹窗状态
  const [showDialog, setShowDialog] = useState(false);
  const [editingBot, setEditingBot] = useState<ChatBot | null>(null);
  const [saving, setSaving] = useState(false);

  const form = useForm<BotFormValues>({
    resolver: zodResolver(botFormSchema),
    defaultValues: {
      name: "",
      platform: "FEISHU",
      description: "",
      appId: "",
      appSecret: "",
      encryptKey: "",
      verificationToken: "",
      corpId: "",
      agentId: "",
      token: "",
      encodingAesKey: "",
      botName: "智能助手",
      domainId: null,
      detectionMode: "COMPOSITE",
      detectionKeywords: "?,？,请,怎么,如何,什么,为什么,能不能,可以吗,吗",
      atTriggerEnabled: true,
      llmThreshold: 0.7,
      answerMode: "RAG",
      systemPrompt: "",
      maxTokens: 2000,
      enabled: true,
    },
  });

  const platform = form.watch("platform");

  const loadData = async () => {
    try {
      setLoading(true);
      const [botsData, domainsData] = await Promise.all([
        listChatBots(),
        listRetrievalDomains(),
      ]);
      setBots(botsData);
      setDomains(domainsData);
    } catch (error) {
      toast.error(getErrorMessage(error, "加载数据失败"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // 打开创建/编辑弹窗
  const openDialog = (bot?: ChatBot) => {
    if (bot) {
      setEditingBot(bot);
      form.reset({
        name: bot.name,
        platform: bot.platform,
        description: bot.description || "",
        appId: bot.appId || "",
        appSecret: bot.appSecret || "",
        encryptKey: bot.encryptKey || "",
        verificationToken: bot.verificationToken || "",
        corpId: bot.corpId || "",
        agentId: bot.agentId || "",
        token: bot.token || "",
        encodingAesKey: bot.encodingAesKey || "",
        botName: bot.botName || "智能助手",
        domainId: bot.domainId || null,
        detectionMode: bot.detectionMode || "COMPOSITE",
        detectionKeywords: bot.detectionKeywords?.join(",") || "?,？,请,怎么,如何,什么,为什么,能不能,可以吗,吗",
        atTriggerEnabled: bot.atTriggerEnabled ?? true,
        llmThreshold: bot.llmThreshold ?? 0.7,
        answerMode: bot.answerMode || "RAG",
        systemPrompt: bot.systemPrompt || "",
        maxTokens: bot.maxTokens ?? 2000,
        enabled: bot.enabled ?? true,
      });
    } else {
      setEditingBot(null);
      form.reset({
        name: "",
        platform: "FEISHU",
        description: "",
        appId: "",
        appSecret: "",
        encryptKey: "",
        verificationToken: "",
        corpId: "",
        agentId: "",
        token: "",
        encodingAesKey: "",
        botName: "智能助手",
        domainId: null,
        detectionMode: "COMPOSITE",
        detectionKeywords: "?,？,请,怎么,如何,什么,为什么,能不能,可以吗,吗",
        atTriggerEnabled: true,
        llmThreshold: 0.7,
        answerMode: "RAG",
        systemPrompt: "",
        maxTokens: 2000,
        enabled: true,
      });
    }
    setShowDialog(true);
  };

  // 保存机器人
  const handleSave = async (values: BotFormValues) => {
    try {
      setSaving(true);
      const payload: ChatBot = {
        ...values,
        detectionKeywords: values.detectionKeywords
          ? values.detectionKeywords.split(",").map((k) => k.trim())
          : [],
        domainId: values.domainId || null,
      };

      if (editingBot) {
        await updateChatBot(editingBot.id!, payload);
        toast.success("更新成功");
      } else {
        await createChatBot(payload);
        toast.success("创建成功");
      }
      setShowDialog(false);
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setSaving(false);
    }
  };

  // 删除机器人
  const handleDelete = async (id: string) => {
    if (!confirm("确定要删除此机器人吗？")) return;
    try {
      await deleteChatBot(id);
      toast.success("删除成功");
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "删除失败"));
    }
  };

  // 切换启用状态
  const handleToggleEnabled = async (bot: ChatBot) => {
    try {
      await setChatBotEnabled(bot.id!, !bot.enabled);
      toast.success(bot.enabled ? "已禁用" : "已启用");
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "操作失败"));
    }
  };

  const getPlatformBadge = (platform: string) => {
    switch (platform) {
      case "FEISHU":
        return <Badge variant="default">飞书</Badge>;
      case "WEWORK":
        return <Badge variant="secondary">企微</Badge>;
      default:
        return <Badge variant="outline">{platform}</Badge>;
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
          <h1 className="admin-page-title">聊天机器人管理</h1>
          <p className="admin-page-subtitle">管理飞书和企微机器人实例</p>
        </div>
        <Button onClick={() => openDialog()}>
          <Plus className="mr-2 h-4 w-4" />
          新建机器人
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>名称</TableHead>
                <TableHead>平台</TableHead>
                <TableHead>检索域</TableHead>
                <TableHead>回答模式</TableHead>
                <TableHead>状态</TableHead>
                <TableHead className="w-[100px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {bots.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                    暂无机器人，点击上方按钮创建
                  </TableCell>
                </TableRow>
              ) : (
                bots.map((bot) => (
                  <TableRow key={bot.id}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Bot className="h-4 w-4 text-muted-foreground" />
                        <div>
                          <div className="font-medium">{bot.name}</div>
                          <div className="text-xs text-muted-foreground">
                            {bot.botName || "智能助手"}
                          </div>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>{getPlatformBadge(bot.platform)}</TableCell>
                    <TableCell>
                      {bot.domainName ? (
                        <Badge variant="outline">{bot.domainName}</Badge>
                      ) : (
                        <span className="text-muted-foreground text-sm">未绑定</span>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={bot.answerMode === "RAG" ? "default" : "secondary"}>
                        {bot.answerMode === "RAG" ? "RAG检索" : "LLM直答"}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Switch
                        checked={bot.enabled}
                        onCheckedChange={() => handleToggleEnabled(bot)}
                      />
                    </TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => openDialog(bot)}>
                            <Pencil className="mr-2 h-4 w-4" />
                            编辑
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            onClick={() => handleDelete(bot.id!)}
                            className="text-destructive"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            删除
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* 创建/编辑机器人弹窗 */}
      <Dialog open={showDialog} onOpenChange={setShowDialog}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingBot ? "编辑机器人" : "新建机器人"}</DialogTitle>
            <DialogDescription>
              配置机器人的平台连接参数和行为设置
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(handleSave)} className="space-y-6">
              {/* 基本信息 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium">基本信息</h4>
                <div className="grid grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>机器人名称</FormLabel>
                        <FormControl>
                          <Input placeholder="用于管理标识" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="platform"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>平台</FormLabel>
                        <Select
                          onValueChange={field.onChange}
                          value={field.value}
                          disabled={!!editingBot}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="FEISHU">飞书</SelectItem>
                            <SelectItem value="WEWORK">企业微信</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
                <FormField
                  control={form.control}
                  name="description"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>描述</FormLabel>
                      <FormControl>
                        <Input placeholder="简要描述此机器人的用途" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="botName"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>机器人昵称</FormLabel>
                      <FormControl>
                        <Input placeholder="智能助手" {...field} />
                      </FormControl>
                      <FormDescription>用于识别群聊中的 @ 事件</FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* 平台配置 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium">
                  {platform === "FEISHU" ? "飞书配置" : "企微配置"}
                </h4>
                {platform === "FEISHU" ? (
                  <>
                    <div className="grid grid-cols-2 gap-4">
                      <FormField
                        control={form.control}
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
                        control={form.control}
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
                        control={form.control}
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
                        control={form.control}
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
                  </>
                ) : (
                  <>
                    <div className="grid grid-cols-2 gap-4">
                      <FormField
                        control={form.control}
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
                        control={form.control}
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
                        control={form.control}
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
                      <FormField
                        control={form.control}
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
                    </div>
                  </>
                )}
              </div>

              {/* 检索域绑定 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium">检索域绑定</h4>
                <FormField
                  control={form.control}
                  name="domainId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>检索域</FormLabel>
                      <Select
                        onValueChange={(v) => field.onChange(v === "__none__" ? null : v)}
                        value={field.value?.toString() || "__none__"}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="选择检索域（可选）" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="__none__">不绑定检索域</SelectItem>
                          {domains.map((domain) => (
                            <SelectItem key={domain.id} value={domain.id!.toString()}>
                              {domain.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormDescription>
                        绑定后机器人只能从检索域关联的知识库中检索信息
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* 检测配置 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium">问题检测</h4>
                <div className="grid grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="detectionMode"
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
                            <SelectItem value="KEYWORD">仅关键词</SelectItem>
                            <SelectItem value="LLM">仅 LLM</SelectItem>
                            <SelectItem value="COMPOSITE">组合模式</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
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
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
                <FormField
                  control={form.control}
                  name="detectionKeywords"
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
                <FormField
                  control={form.control}
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
              </div>

              {/* 回答配置 */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium">回答生成</h4>
                <div className="grid grid-cols-2 gap-4">
                  <FormField
                    control={form.control}
                    name="answerMode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>回答模式</FormLabel>
                        <Select onValueChange={field.onChange} value={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="LLM">LLM 直答</SelectItem>
                            <SelectItem value="RAG">RAG 检索增强</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormDescription>
                          RAG 模式需要绑定检索域
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
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
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>
                <FormField
                  control={form.control}
                  name="systemPrompt"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>系统提示词（LLM 模式）</FormLabel>
                      <FormControl>
                        <Textarea
                          placeholder="你是一个智能客服助手，请简洁准确地回答用户问题。"
                          rows={3}
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* 启用状态 */}
              <FormField
                control={form.control}
                name="enabled"
                render={({ field }) => (
                  <FormItem className="flex items-center justify-between rounded-lg border p-4">
                    <div>
                      <FormLabel>启用状态</FormLabel>
                      <FormDescription>禁用后机器人将不会接收和处理消息</FormDescription>
                    </div>
                    <FormControl>
                      <Switch checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                  </FormItem>
                )}
              />

              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setShowDialog(false)}>
                  取消
                </Button>
                <Button type="submit" disabled={saving}>
                  {saving ? "保存中..." : "保存"}
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
