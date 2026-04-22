import { useState } from "react";
import { toast } from "sonner";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  createModelProvider,
  updateModelProvider,
  deleteModelProvider,
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
import { Switch } from "@/components/ui/switch";
import { Pencil, Plus, Trash2, X } from "lucide-react";

const formSchema = z.object({
  name: z.string().min(1, "请输入提供商名称").max(100),
  url: z.string().max(500).optional().or(z.literal("")),
  apiKey: z.string().max(500).optional().or(z.literal("")),
  enabled: z.boolean(),
  endpoints: z.array(
    z.object({
      key: z.string().min(1, "请输入端点名称"),
      value: z.string().min(1, "请输入端点路径"),
    })
  ).optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface ModelProviderDialogProps {
  provider?: ModelProvider;
  providers: ModelProvider[];
  onRefresh: () => void;
}

// 常用端点类型
const ENDPOINT_SUGGESTIONS = ["chat", "embedding", "rerank"];

export function ModelProviderDialog({ provider, providers, onRefresh }: ModelProviderDialogProps) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const isEdit = !!provider?.id;

  // 将 endpoints 对象转换为数组
  const endpointsToArray = (endpoints?: Record<string, string> | null) => {
    if (!endpoints) return [];
    return Object.entries(endpoints).map(([key, value]) => ({ key, value }));
  };

  // 将数组转换回 endpoints 对象
  const arrayToEndpoints = (arr: { key: string; value: string }[]): Record<string, string> => {
    const result: Record<string, string> = {};
    arr.forEach(item => {
      if (item.key && item.value) {
        result[item.key] = item.value;
      }
    });
    return result;
  };

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: provider?.name || "",
      url: provider?.url || "",
      apiKey: provider?.apiKey || "",
      enabled: provider?.enabled ?? true,
      endpoints: endpointsToArray(provider?.endpoints),
    },
  });

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "endpoints",
  });

  const handleSubmit = async (values: FormValues) => {
    try {
      setLoading(true);
      const payload = {
        ...values,
        endpoints: arrayToEndpoints(values.endpoints || []),
      };
      if (isEdit && provider?.id) {
        await updateModelProvider(provider.id, payload);
        toast.success("更新成功");
      } else {
        await createModelProvider(payload);
        toast.success("创建成功");
      }
      setOpen(false);
      onRefresh();
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!provider?.id) return;
    if (!confirm("确定要删除此提供商吗？")) return;

    try {
      await deleteModelProvider(provider.id);
      toast.success("删除成功");
      setOpen(false);
      onRefresh();
    } catch (error) {
      toast.error(getErrorMessage(error, "删除失败"));
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
            添加提供商
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{isEdit ? "编辑提供商" : "添加提供商"}</DialogTitle>
          <DialogDescription>
            配置 AI 模型提供商的连接信息
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>提供商名称</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="例如：openai, siliconflow, ollama"
                      {...field}
                      disabled={isEdit}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="url"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>基础 URL</FormLabel>
                  <FormControl>
                    <Input placeholder="https://api.example.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="apiKey"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>API 密钥</FormLabel>
                  <FormControl>
                    <Input type="password" placeholder="sk-..." {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <FormLabel>端点配置</FormLabel>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => append({ key: "", value: "" })}
                >
                  <Plus className="mr-1 h-3 w-3" />
                  添加端点
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                配置不同类型 API 的端点路径，如 chat、embedding、rerank
              </p>
              <div className="space-y-2">
                {fields.map((field, index) => (
                  <div key={field.id} className="flex gap-2 items-start">
                    <FormField
                      control={form.control}
                      name={`endpoints.${index}.key`}
                      render={({ field }) => (
                        <FormItem className="flex-1">
                          <FormControl>
                            <Input
                              placeholder="端点名称"
                              list="endpoint-suggestions"
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name={`endpoints.${index}.value`}
                      render={({ field }) => (
                        <FormItem className="flex-[2]">
                          <FormControl>
                            <Input placeholder="/v1/chat/completions" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => remove(index)}
                      className="mt-1"
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
                {fields.length === 0 && (
                  <div className="text-sm text-muted-foreground py-2 text-center border rounded-md">
                    暂无端点配置
                  </div>
                )}
              </div>
              <datalist id="endpoint-suggestions">
                {ENDPOINT_SUGGESTIONS.map((s) => (
                  <option key={s} value={s} />
                ))}
              </datalist>
            </div>
            <FormField
              control={form.control}
              name="enabled"
              render={({ field }) => (
                <FormItem className="flex items-center justify-between rounded-lg border p-4">
                  <div className="space-y-0.5">
                    <FormLabel className="text-base">启用状态</FormLabel>
                  </div>
                  <FormControl>
                    <Switch
                      checked={field.value}
                      onCheckedChange={field.onChange}
                    />
                  </FormControl>
                </FormItem>
              )}
            />
            <DialogFooter>
              {isEdit && (
                <Button
                  type="button"
                  variant="destructive"
                  onClick={handleDelete}
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
