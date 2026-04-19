import { useState } from "react";
import { toast } from "sonner";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  getChatModelConfig,
  getEmbeddingModelConfig,
  getRerankModelConfig,
  updateChatModelConfig,
  updateEmbeddingModelConfig,
  updateRerankModelConfig,
  type ModelCandidate,
  type ModelProvider,
} from "@/services/modelConfigService";
import { getErrorMessage } from "@/utils/error";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Pencil, Plus, Trash2 } from "lucide-react";

const formSchema = z.object({
  modelId: z.string().min(1, "请输入模型标识").max(100),
  provider: z.string().min(1, "请选择提供商"),
  model: z.string().min(1, "请输入模型名称").max(200),
  url: z.string().max(500).optional().or(z.literal("")),
  dimension: z.number().int().positive().optional().nullable(),
  priority: z.number().int().min(1).max(1000),
  enabled: z.boolean(),
  supportsThinking: z.boolean(),
});

type FormValues = z.infer<typeof formSchema>;

interface ModelCandidateDialogProps {
  candidate?: ModelCandidate;
  modelType: "CHAT" | "EMBEDDING" | "RERANK";
  providers: ModelProvider[];
  onRefresh: () => void;
}

export function ModelCandidateDialog({
  candidate,
  modelType,
  providers,
  onRefresh,
}: ModelCandidateDialogProps) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const isEdit = !!candidate?.id;

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      modelId: candidate?.modelId || "",
      provider: candidate?.provider || "",
      model: candidate?.model || "",
      url: candidate?.url || "",
      dimension: candidate?.dimension || null,
      priority: candidate?.priority || 100,
      enabled: candidate?.enabled ?? true,
      supportsThinking: candidate?.supportsThinking ?? false,
    },
  });

  const handleSubmit = async (values: FormValues) => {
    try {
      setLoading(true);

      // 获取当前配置
      const getConfig =
        modelType === "CHAT"
          ? getChatModelConfig
          : modelType === "EMBEDDING"
          ? getEmbeddingModelConfig
          : getRerankModelConfig;

      const updateConfig =
        modelType === "CHAT"
          ? updateChatModelConfig
          : modelType === "EMBEDDING"
          ? updateEmbeddingModelConfig
          : updateRerankModelConfig;

      const config = await getConfig();
      const candidates = config.candidates || [];

      // 构建新的候选对象
      const newCandidate: ModelCandidate = {
        id: isEdit ? candidate.id : null,
        modelId: values.modelId,
        provider: values.provider,
        model: values.model,
        url: values.url || null,
        dimension: values.dimension || null,
        priority: values.priority,
        enabled: values.enabled,
        supportsThinking: values.supportsThinking,
        isDefault: candidate?.isDefault || false,
        isDeepThinking: candidate?.isDeepThinking || false,
      };

      // 更新候选列表
      let updatedCandidates: ModelCandidate[];
      if (isEdit) {
        updatedCandidates = candidates.map((c) =>
          c.id === candidate.id ? newCandidate : c
        );
      } else {
        updatedCandidates = [...candidates, newCandidate];
      }

      // 保存配置
      await updateConfig({
        ...config,
        candidates: updatedCandidates,
      });

      toast.success(isEdit ? "更新成功" : "添加成功");
      setOpen(false);
      onRefresh();
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!candidate?.id) return;
    if (!confirm("确定要删除此模型吗？")) return;

    try {
      setLoading(true);

      const getConfig =
        modelType === "CHAT"
          ? getChatModelConfig
          : modelType === "EMBEDDING"
          ? getEmbeddingModelConfig
          : getRerankModelConfig;

      const updateConfig =
        modelType === "CHAT"
          ? updateChatModelConfig
          : modelType === "EMBEDDING"
          ? updateEmbeddingModelConfig
          : updateRerankModelConfig;

      const config = await getConfig();
      const updatedCandidates = (config.candidates || []).filter(
        (c) => c.id !== candidate.id
      );

      await updateConfig({
        ...config,
        candidates: updatedCandidates,
      });

      toast.success("删除成功");
      setOpen(false);
      onRefresh();
    } catch (error) {
      toast.error(getErrorMessage(error, "删除失败"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {isEdit ? (
          <Button variant="ghost" size="icon">
            <Pencil className="h-4 w-4" />
          </Button>
        ) : (
          <Button variant="outline" size="sm">
            <Plus className="mr-2 h-4 w-4" />
            添加模型
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{isEdit ? "编辑模型" : "添加模型"}</DialogTitle>
          <DialogDescription>
            配置 {modelType} 模型候选
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="modelId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>模型标识</FormLabel>
                    <FormControl>
                      <Input placeholder="例如：gpt-4" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="provider"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>提供商</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="选择提供商" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {providers.map((p) => (
                          <SelectItem key={p.name} value={p.name}>
                            {p.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="model"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>模型名称</FormLabel>
                  <FormControl>
                    <Input placeholder="例如：gpt-4-turbo-preview" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="priority"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>优先级</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        {...field}
                        onChange={(e) => field.onChange(parseInt(e.target.value) || 100)}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              {modelType === "EMBEDDING" && (
                <FormField
                  control={form.control}
                  name="dimension"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>向量维度</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="例如：1536"
                          {...field}
                          value={field.value ?? ""}
                          onChange={(e) =>
                            field.onChange(e.target.value ? parseInt(e.target.value) : null)
                          }
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
            </div>
            <FormField
              control={form.control}
              name="url"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>自定义 URL（可选）</FormLabel>
                  <FormControl>
                    <Input placeholder="覆盖提供商默认 URL" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="enabled"
                render={({ field }) => (
                  <FormItem className="flex items-center justify-between rounded-lg border p-3">
                    <FormLabel className="text-sm">启用</FormLabel>
                    <FormControl>
                      <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                      />
                    </FormControl>
                  </FormItem>
                )}
              />
              {modelType === "CHAT" && (
                <FormField
                  control={form.control}
                  name="supportsThinking"
                  render={({ field }) => (
                    <FormItem className="flex items-center justify-between rounded-lg border p-3">
                      <FormLabel className="text-sm">支持思考链</FormLabel>
                      <FormControl>
                        <Switch
                          checked={field.value}
                          onCheckedChange={field.onChange}
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />
              )}
            </div>
            <DialogFooter>
              {isEdit && (
                <Button
                  type="button"
                  variant="destructive"
                  onClick={handleDelete}
                  disabled={loading}
                  className="mr-auto"
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  删除
                </Button>
              )}
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                取消
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? "保存中..." : "保存"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
