import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  listRetrievalDomains,
  getRetrievalDomain,
  createRetrievalDomain,
  updateRetrievalDomain,
  deleteRetrievalDomain,
  bindKnowledgesToDomain,
  type RetrievalDomain,
} from "@/services/chatbotService";
import { listKnowledgeBases, type KnowledgeBase } from "@/services/knowledgeService";
import { getErrorMessage } from "@/utils/error";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
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
import { Checkbox } from "@/components/ui/checkbox";
import { Textarea } from "@/components/ui/textarea";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  Plus,
  MoreHorizontal,
  Pencil,
  Trash2,
  Link,
  Bot,
  ChevronDown,
  ChevronRight,
  Eye,
  Database,
} from "lucide-react";

// 表单 Schema
const domainFormSchema = z.object({
  name: z.string().min(1, "名称不能为空").max(100),
  description: z.string().max(500).optional().or(z.literal("")),
  enabled: z.boolean(),
});

type DomainFormValues = z.infer<typeof domainFormSchema>;

export function RetrievalDomainPage() {
  const [loading, setLoading] = useState(true);
  const [domains, setDomains] = useState<RetrievalDomain[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);

  // 弹窗状态
  const [showDomainDialog, setShowDomainDialog] = useState(false);
  const [showKnowledgeDialog, setShowKnowledgeDialog] = useState(false);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  const [editingDomain, setEditingDomain] = useState<RetrievalDomain | null>(null);
  const [bindingDomain, setBindingDomain] = useState<RetrievalDomain | null>(null);
  const [detailDomain, setDetailDomain] = useState<RetrievalDomain | null>(null);
  const [selectedKnowledgeIds, setSelectedKnowledgeIds] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const form = useForm<DomainFormValues>({
    resolver: zodResolver(domainFormSchema),
    defaultValues: {
      name: "",
      description: "",
      enabled: true,
    },
  });

  const loadData = async () => {
    try {
      setLoading(true);
      const [domainsData, kbData] = await Promise.all([
        listRetrievalDomains(),
        listKnowledgeBases(),
      ]);
      setDomains(domainsData);
      setKnowledgeBases(kbData);
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
  const openDomainDialog = (domain?: RetrievalDomain) => {
    if (domain) {
      setEditingDomain(domain);
      form.reset({
        name: domain.name,
        description: domain.description || "",
        enabled: domain.enabled ?? true,
      });
    } else {
      setEditingDomain(null);
      form.reset({
        name: "",
        description: "",
        enabled: true,
      });
    }
    setShowDomainDialog(true);
  };

  // 保存检索域
  const handleSaveDomain = async (values: DomainFormValues) => {
    try {
      setSaving(true);
      if (editingDomain) {
        await updateRetrievalDomain(editingDomain.id!, values);
        toast.success("更新成功");
      } else {
        await createRetrievalDomain(values);
        toast.success("创建成功");
      }
      setShowDomainDialog(false);
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "保存失败"));
    } finally {
      setSaving(false);
    }
  };

  // 删除检索域
  const handleDeleteDomain = async (id: string) => {
    if (!confirm("确定要删除此检索域吗？")) return;
    try {
      await deleteRetrievalDomain(id);
      toast.success("删除成功");
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "删除失败"));
    }
  };

  // 打开绑定知识库弹窗
  const openKnowledgeDialog = (domain: RetrievalDomain) => {
    setBindingDomain(domain);
    setSelectedKnowledgeIds(domain.knowledgeIds || []);
    setShowKnowledgeDialog(true);
  };

  // 切换知识库选择
  const toggleKnowledge = (knowledgeId: string) => {
    setSelectedKnowledgeIds((prev) =>
      prev.includes(knowledgeId)
        ? prev.filter((id) => id !== knowledgeId)
        : [...prev, knowledgeId]
    );
  };

  // 保存知识库绑定
  const handleSaveKnowledge = async () => {
    if (!bindingDomain) return;
    try {
      setSaving(true);
      await bindKnowledgesToDomain(bindingDomain.id!, selectedKnowledgeIds);
      toast.success("绑定成功");
      setShowKnowledgeDialog(false);
      loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "绑定失败"));
    } finally {
      setSaving(false);
    }
  };

  // 查看检索域详情
  const handleViewDetail = async (domain: RetrievalDomain) => {
    try {
      const detail = await getRetrievalDomain(domain.id!);
      setDetailDomain(detail);
      setShowDetailDialog(true);
    } catch (error) {
      toast.error(getErrorMessage(error, "获取详情失败"));
    }
  };

  // 切换行展开状态
  const toggleRowExpand = (domainId: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(domainId)) {
        next.delete(domainId);
      } else {
        next.add(domainId);
      }
      return next;
    });
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
          <h1 className="admin-page-title">检索域管理</h1>
          <p className="admin-page-subtitle">管理检索域及其关联的知识库</p>
        </div>
        <Button onClick={() => openDomainDialog()}>
          <Plus className="mr-2 h-4 w-4" />
          新建检索域
        </Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>名称</TableHead>
                <TableHead>描述</TableHead>
                <TableHead>关联知识库</TableHead>
                <TableHead>绑定机器人</TableHead>
                <TableHead>状态</TableHead>
                <TableHead className="w-[100px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {domains.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                    暂无检索域，点击上方按钮创建
                  </TableCell>
                </TableRow>
              ) : (
                domains.map((domain) => {
                  const isExpanded = expandedRows.has(domain.id!);
                  return (
                    <>
                      <TableRow key={domain.id} className="cursor-pointer hover:bg-muted/50">
                        <TableCell className="font-medium">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => toggleRowExpand(domain.id!)}
                              className="p-0.5 hover:bg-muted rounded"
                            >
                              {isExpanded ? (
                                <ChevronDown className="h-4 w-4 text-muted-foreground" />
                              ) : (
                                <ChevronRight className="h-4 w-4 text-muted-foreground" />
                              )}
                            </button>
                            {domain.name}
                          </div>
                        </TableCell>
                        <TableCell className="text-muted-foreground max-w-[200px] truncate">
                          {domain.description || "-"}
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1 max-w-[200px]">
                            {domain.knowledges && domain.knowledges.length > 0 ? (
                              domain.knowledges.slice(0, 3).map((kb) => (
                                <Badge key={kb.id} variant="secondary" className="text-xs">
                                  {kb.name}
                                </Badge>
                              ))
                            ) : (
                              <span className="text-muted-foreground text-sm">未绑定</span>
                            )}
                            {domain.knowledges && domain.knowledges.length > 3 && (
                              <Badge variant="outline" className="text-xs">
                                +{domain.knowledges.length - 3}
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell>
                          {domain.botCount ? (
                            <Badge variant="outline" className="gap-1">
                              <Bot className="h-3 w-3" />
                              {domain.botCount} 个
                            </Badge>
                          ) : (
                            <span className="text-muted-foreground text-sm">无</span>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant={domain.enabled ? "default" : "outline"}>
                            {domain.enabled ? "已启用" : "已禁用"}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon">
                                <MoreHorizontal className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => handleViewDetail(domain)}>
                                <Eye className="mr-2 h-4 w-4" />
                                查看详情
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => openDomainDialog(domain)}>
                                <Pencil className="mr-2 h-4 w-4" />
                                编辑
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => openKnowledgeDialog(domain)}>
                                <Link className="mr-2 h-4 w-4" />
                                绑定知识库
                              </DropdownMenuItem>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                onClick={() => handleDeleteDomain(domain.id!)}
                                className="text-destructive"
                              >
                                <Trash2 className="mr-2 h-4 w-4" />
                                删除
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </TableCell>
                      </TableRow>
                      {isExpanded && (
                        <TableRow key={`${domain.id}-detail`} className="bg-muted/30">
                          <TableCell colSpan={6} className="p-4">
                            <div className="space-y-3">
                              <div className="flex items-center gap-2 text-sm font-medium">
                                <Database className="h-4 w-4" />
                                关联知识库 ({domain.knowledges?.length || 0})
                              </div>
                              {domain.knowledges && domain.knowledges.length > 0 ? (
                                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2 pl-6">
                                  {domain.knowledges.map((kb) => (
                                    <div
                                      key={kb.id}
                                      className="flex items-center gap-2 p-2 rounded-md border bg-background"
                                    >
                                      <Database className="h-3 w-3 text-muted-foreground" />
                                      <span className="text-sm truncate">{kb.name}</span>
                                    </div>
                                  ))}
                                </div>
                              ) : (
                                <div className="pl-6 text-sm text-muted-foreground">
                                  暂未关联知识库，点击"绑定知识库"进行配置
                                </div>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      )}
                    </>
                  );
                })
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* 创建/编辑检索域弹窗 */}
      <Dialog open={showDomainDialog} onOpenChange={setShowDomainDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingDomain ? "编辑检索域" : "新建检索域"}</DialogTitle>
            <DialogDescription>
              检索域是一组知识库的集合，机器人可以绑定检索域来限定检索范围
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(handleSaveDomain)} className="space-y-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>名称</FormLabel>
                    <FormControl>
                      <Input placeholder="输入检索域名称" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>描述</FormLabel>
                    <FormControl>
                      <Textarea placeholder="描述此检索域的用途" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="enabled"
                render={({ field }) => (
                  <FormItem className="flex items-center justify-between rounded-lg border p-4">
                    <div>
                      <FormLabel>启用状态</FormLabel>
                      <FormDescription>禁用后绑定的机器人将无法使用此检索域</FormDescription>
                    </div>
                    <FormControl>
                      <Switch checked={field.value} onCheckedChange={field.onChange} />
                    </FormControl>
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setShowDomainDialog(false)}>
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

      {/* 绑定知识库弹窗 */}
      <Dialog open={showKnowledgeDialog} onOpenChange={setShowKnowledgeDialog}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>绑定知识库</DialogTitle>
            <DialogDescription>
              为检索域「{bindingDomain?.name}」选择要关联的知识库
            </DialogDescription>
          </DialogHeader>
          <div className="max-h-[400px] overflow-y-auto space-y-2 py-4">
            {knowledgeBases.length === 0 ? (
              <div className="text-center text-muted-foreground py-4">
                暂无知识库，请先创建知识库
              </div>
            ) : (
              knowledgeBases.map((kb) => (
                <div
                  key={kb.id}
                  className="flex items-center space-x-3 p-3 rounded-lg border hover:bg-accent cursor-pointer"
                  onClick={() => toggleKnowledge(kb.id!)}
                >
                  <Checkbox
                    checked={selectedKnowledgeIds.includes(kb.id!)}
                    onCheckedChange={() => toggleKnowledge(kb.id!)}
                  />
                  <div className="flex-1">
                    <div className="font-medium">{kb.name}</div>
                    <div className="text-sm text-muted-foreground">
                      {kb.description || "暂无描述"}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowKnowledgeDialog(false)}>
              取消
            </Button>
            <Button onClick={handleSaveKnowledge} disabled={saving}>
              {saving ? "保存中..." : `保存 (${selectedKnowledgeIds.length})`}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 检索域详情弹窗 */}
      <Dialog open={showDetailDialog} onOpenChange={setShowDetailDialog}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>检索域详情</DialogTitle>
            <DialogDescription>
              {detailDomain?.name}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="text-sm text-muted-foreground">状态</div>
                <Badge variant={detailDomain?.enabled ? "default" : "outline"}>
                  {detailDomain?.enabled ? "已启用" : "已禁用"}
                </Badge>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">绑定机器人</div>
                <div className="font-medium">{detailDomain?.botCount || 0} 个</div>
              </div>
            </div>
            {detailDomain?.description && (
              <div>
                <div className="text-sm text-muted-foreground mb-1">描述</div>
                <div className="text-sm">{detailDomain.description}</div>
              </div>
            )}
            <div>
              <div className="text-sm text-muted-foreground mb-2">
                关联知识库 ({detailDomain?.knowledges?.length || 0})
              </div>
              {detailDomain?.knowledges && detailDomain.knowledges.length > 0 ? (
                <div className="space-y-2 max-h-[300px] overflow-y-auto">
                  {detailDomain.knowledges.map((kb) => (
                    <div
                      key={kb.id}
                      className="flex items-center gap-2 p-2 rounded-md border"
                    >
                      <Database className="h-4 w-4 text-muted-foreground" />
                      <span>{kb.name}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-sm text-muted-foreground">
                  暂未关联知识库
                </div>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDetailDialog(false)}>
              关闭
            </Button>
            <Button onClick={() => {
              setShowDetailDialog(false);
              if (detailDomain) {
                openKnowledgeDialog(detailDomain);
              }
            }}>
              <Link className="mr-2 h-4 w-4" />
              绑定知识库
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
