import { useEffect, useMemo, useState } from "react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  ChevronDown,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  ClipboardList,
  Database,
  GitBranch,
  Layers,
  LayoutDashboard,
  Lightbulb,
  LogOut,
  Menu,
  MessageSquare,
  KeyRound,
  Settings,
  Upload,
  Users,
  FolderKanban,
  Workflow,
  Cpu,
  Bot
} from "lucide-react";
import { useAuthStore } from "@/stores/authStore";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { changePassword } from "@/services/userService";
import { Avatar } from "@/components/common/Avatar";

type MenuChild = {
  path: string;
  label: string;
  icon: any;
  search?: string;
};

type MenuItem = {
  id?: string;
  path: string;
  label: string;
  icon: any;
  search?: string;
  children?: MenuChild[];
};

type MenuGroup = {
  title: string;
  items: MenuItem[];
};

const menuGroups: MenuGroup[] = [
  {
    title: "导航",
    items: [
      {
        path: "/admin/dashboard",
        label: "Dashboard",
        icon: LayoutDashboard
      },
      {
        path: "/admin/chat",
        label: "智能对话",
        icon: MessageSquare
      },
      {
        id: "knowledge",
        path: "/admin/knowledge",
        label: "知识库管理",
        icon: Database,
        children: [
          {
            path: "/admin/knowledge",
            label: "知识库",
            icon: Database
          },
          {
            path: "/admin/settings/retrieval-domains",
            label: "检索域",
            icon: Layers
          }
        ]
      },
      {
        id: "intent",
        path: "/admin/intent-tree",
        label: "意图管理",
        icon: Lightbulb,
        children: [
          {
            path: "/admin/intent-tree",
            label: "意图树配置",
            icon: GitBranch
          },
          {
            path: "/admin/intent-list",
            label: "意图列表",
            icon: ClipboardList
          }
        ]
      },
      {
        id: "ingestion",
        path: "/admin/ingestion",
        label: "数据通道",
        icon: Upload,
        children: [
          {
            path: "/admin/ingestion",
            label: "流水线管理",
            icon: FolderKanban,
            search: "?tab=pipelines"
          },
          {
            path: "/admin/ingestion",
            label: "流水线任务",
            icon: ClipboardList,
            search: "?tab=tasks"
          }
        ]
      },
      {
        path: "/admin/mappings",
        label: "关键词映射",
        icon: KeyRound
      },
      {
        path: "/admin/traces",
        label: "链路追踪",
        icon: Workflow
      },
    ]
  },
  {
    title: "设置",
    items: [
      {
        path: "/admin/settings/models",
        label: "模型配置",
        icon: Cpu
      },
      {
        path: "/admin/settings/chat-bots",
        label: "聊天机器人",
        icon: Bot
      },
      {
        path: "/admin/users",
        label: "用户管理",
        icon: Users
      },
      {
        path: "/admin/sample-questions",
        label: "示例问题",
        icon: Lightbulb
      },
      {
        path: "/admin/settings",
        label: "系统设置",
        icon: Settings
      },
    ]
  }
];

const breadcrumbMap: Record<string, string> = {
  dashboard: "Dashboard",
  chat: "智能对话",
  knowledge: "知识库管理",
  "intent-tree": "意图树配置",
  "intent-list": "意图列表",
  ingestion: "数据通道",
  traces: "链路追踪",
  "sample-questions": "示例问题",
  mappings: "关键词映射",
  settings: "系统设置",
  users: "用户管理",
  models: "模型配置",
  "chat-bots": "机器人管理",
  "retrieval-domains": "检索域管理"
};

export function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [collapsed, setCollapsed] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({ ingestion: true, intent: true });
  const isDashboardRoute = location.pathname.startsWith("/admin/dashboard");

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const breadcrumbs = useMemo(() => {
    const segments = location.pathname.split("/").filter(Boolean);
    const items: { label: string; to?: string }[] = [
      { label: "首页", to: "/admin/dashboard" }
    ];

    if (segments[0] !== "admin") return items;
    const section = segments[1];
    if (section) {
      // 知识库管理菜单
      if (section === "knowledge") {
        items.push({
          label: "知识库管理",
          to: "/admin/knowledge"
        });
        if (segments.length > 2) {
          items.push({ label: "文档管理" });
        }
        if (segments.includes("docs")) {
          items.push({ label: "切片管理" });
        }
      } else if (section === "settings" && segments[2] === "retrieval-domains") {
        // 检索域管理
        items.push({
          label: "知识库管理",
          to: "/admin/knowledge"
        });
        items.push({
          label: "检索域"
        });
      } else if (section === "intent-tree" || section === "intent-list") {
        items.push({
          label: "意图管理",
          to: "/admin/intent-tree"
        });
        if (section === "intent-list" && segments.includes("edit")) {
          items.push({
            label: breadcrumbMap[section] || section,
            to: "/admin/intent-list"
          });
          items.push({
            label: "编辑节点"
          });
        } else {
          items.push({
            label: breadcrumbMap[section] || section
          });
        }
      } else {
        items.push({
          label: breadcrumbMap[section] || section,
          to: `/admin/${section}`
        });
      }
    }

    if (section === "ingestion") {
      const searchParams = new URLSearchParams(location.search);
      const tab = searchParams.get("tab");
      if (tab === "tasks") {
        items.push({ label: "流水线任务" });
      } else if (tab === "pipelines") {
        items.push({ label: "流水线管理" });
      }
    }

    if (section === "traces" && segments.length > 2) {
      items.push({ label: "链路详情" });
    }

    return items;
  }, [location.pathname, location.search]);

  const avatarUrl = user?.avatar?.trim();
  const showAvatar = Boolean(avatarUrl);
  const roleLabel = user?.role === "admin" ? "管理员" : "成员";
  const isIngestionActive = location.pathname.startsWith("/admin/ingestion");
  const isIntentActive =
    location.pathname.startsWith("/admin/intent-tree") || location.pathname.startsWith("/admin/intent-list");
  const isKnowledgeActive =
    location.pathname.startsWith("/admin/knowledge") || location.pathname.startsWith("/admin/settings/retrieval-domains");
  const isChatbotActive = location.pathname.startsWith("/admin/settings/chat-bots");

  useEffect(() => {
    setOpenGroups((prev) => ({
      ...prev,
      ingestion: prev.ingestion || isIngestionActive,
      intent: prev.intent || isIntentActive,
      knowledge: prev.knowledge || isKnowledgeActive,
      chatbot: prev.chatbot || isChatbotActive
    }));
  }, [isIngestionActive, isIntentActive, isKnowledgeActive, isChatbotActive]);

  const handlePasswordSubmit = async () => {
    if (!passwordForm.currentPassword || !passwordForm.newPassword) {
      toast.error("请输入当前密码和新密码");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      toast.error("两次输入的新密码不一致");
      return;
    }
    try {
      setPasswordSubmitting(true);
      await changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword
      });
      toast.success("密码已更新");
      setPasswordOpen(false);
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
    } catch (error) {
      toast.error((error as Error).message || "修改密码失败");
    } finally {
      setPasswordSubmitting(false);
    }
  };

  const isLeafActive = (path: string, search?: string) => {
    if (location.pathname !== path && !location.pathname.startsWith(`${path}/`)) {
      return false;
    }
    if (search) {
      return location.search === search;
    }
    return true;
  };

  return (
    <div className="admin-layout flex h-screen">
      <aside className={cn("admin-sidebar", collapsed && "admin-sidebar--collapsed")}>
        <div className="admin-sidebar__brand">
          <div className={cn("flex items-center gap-3", collapsed && "justify-center")}>
            {!collapsed && (
              <div className="min-w-0">
                <h1 className="admin-sidebar__title">Ragent AI 管理后台</h1>
                <p className="admin-sidebar__subtitle">Knowledge Console</p>
              </div>
            )}
          </div>
        </div>

        <nav className="flex-1 space-y-4 px-2 pb-4">
          {menuGroups.map((group) => (
            <div key={group.title} className="space-y-2">
              {!collapsed && (
                <p className="admin-sidebar__group-title">{group.title}</p>
              )}
              <div className="space-y-1">
                {group.items.flatMap((item) => {
                  if (!item.children || item.children.length === 0) {
                    const Icon = item.icon;
                    const isActive = isLeafActive(item.path, item.search);
                    return (
                      <Link
                        key={item.path}
                        to={item.path}
                        title={collapsed ? item.label : undefined}
                        className={cn(
                          "admin-sidebar__item",
                          isActive && "admin-sidebar__item--active",
                          collapsed && "justify-center"
                        )}
                      >
                        <span
                          className={cn(
                            "admin-sidebar__item-indicator",
                            isActive && "is-active"
                          )}
                        />
                        <Icon className="admin-sidebar__item-icon" />
                        {collapsed ? <span className="sr-only">{item.label}</span> : <span>{item.label}</span>}
                      </Link>
                    );
                  }

                  const isGroupActive = item.children.some((child) => isLeafActive(child.path, child.search));
                  const groupId = item.id as string;
                  const isOpen = openGroups[groupId];

                  if (collapsed) {
                    return item.children.map((child) => {
                      const ChildIcon = child.icon;
                      const isActive = isLeafActive(child.path, child.search);
                      return (
                        <Link
                          key={child.label}
                          to={`${child.path}${child.search || ""}`}
                          title={child.label}
                          className={cn(
                            "admin-sidebar__item",
                            isActive && "admin-sidebar__item--active",
                            "justify-center"
                          )}
                        >
                          <span
                            className={cn(
                              "admin-sidebar__item-indicator",
                              isActive && "is-active"
                            )}
                          />
                          <ChildIcon className="admin-sidebar__item-icon" />
                          <span className="sr-only">{child.label}</span>
                        </Link>
                      );
                    });
                  }

                      return (
                        <div key={item.label} className="space-y-1">
                          <button
                            type="button"
                            onClick={() => setOpenGroups((prev) => ({ ...prev, [groupId]: !prev[groupId] }))}
                            className={cn(
                              "admin-sidebar__item admin-sidebar__item--group w-full text-white/60",
                              isGroupActive && "admin-sidebar__item--group-active text-white"
                            )}
                          >
                            <span
                              className={cn(
                                "admin-sidebar__item-indicator",
                                isGroupActive && "is-group-active"
                              )}
                            />
                        <item.icon className="admin-sidebar__item-icon" />
                        <span className="flex-1 text-left">{item.label}</span>
                        {isOpen ? (
                          <ChevronDown className="h-4 w-4 text-white/60" />
                        ) : (
                          <ChevronRight className="h-4 w-4 text-white/60" />
                        )}
                      </button>
                      {isOpen ? (
                        <div className="ml-6 space-y-1">
                          {item.children.map((child) => {
                            const ChildIcon = child.icon;
                            const isActive = isLeafActive(child.path, child.search);
                            return (
                              <Link
                                key={child.label}
                                to={`${child.path}${child.search || ""}`}
                                className={cn(
                                  "admin-sidebar__item text-[13px]",
                                  isActive && "admin-sidebar__item--active"
                                )}
                              >
                                <span
                                  className={cn(
                                    "admin-sidebar__item-indicator",
                                    isActive && "is-active"
                                  )}
                                />
                                <ChildIcon className="admin-sidebar__item-icon" />
                                <span>{child.label}</span>
                              </Link>
                            );
                          })}
                        </div>
                      ) : null}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="admin-sidebar__footer space-y-2">
          <button
            type="button"
            className="admin-sidebar__collapse"
            onClick={() => setCollapsed((prev) => !prev)}
          >
            {collapsed ? <ChevronsRight className="h-4 w-4" /> : <ChevronsLeft className="h-4 w-4" />}
            {!collapsed && <span>收起侧边栏</span>}
          </button>
        </div>
      </aside>

      <div
        className={cn(
          "admin-main flex min-h-screen flex-1 flex-col overflow-auto",
          isDashboardRoute && "dashboard-scroll-shell"
        )}
      >
        <header className="admin-topbar">
          <div className="admin-topbar-inner">
            <div className="flex items-center gap-3">
              <Button
                variant="ghost"
                size="icon"
                className="lg:hidden"
                onClick={() => setCollapsed((prev) => !prev)}
                aria-label="切换侧边栏"
              >
                <Menu className="h-5 w-5" />
              </Button>
            </div>
            <div className="flex items-center gap-2">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button
                    type="button"
                    className="flex items-center gap-2 rounded-full border border-slate-200 bg-white px-2.5 py-1.5 text-sm text-slate-600 shadow-sm"
                    aria-label="用户菜单"
                  >
                    <Avatar
                      name={user?.username || "管理员"}
                      src={showAvatar ? avatarUrl : undefined}
                      className="h-8 w-8 border-slate-200 bg-indigo-50 text-xs font-semibold text-indigo-600"
                    />
                    <span className="hidden sm:inline">{user?.username || "管理员"}</span>
                    <ChevronDown className="h-4 w-4 text-slate-400" />
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" sideOffset={8} className="w-44">
                  <div className="px-3 py-2 text-xs text-slate-500">
                    {user?.username || "管理员"} · {roleLabel}
                  </div>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={() => setPasswordOpen(true)}>
                    <KeyRound className="mr-2 h-4 w-4" />
                    修改密码
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={handleLogout} className="text-rose-600 focus:text-rose-600">
                    <LogOut className="mr-2 h-4 w-4" />
                    退出登录
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
        </header>

        <div className="admin-content">
          <nav className="admin-breadcrumbs" aria-label="面包屑">
            {breadcrumbs.map((item, index) => {
              const isLast = index === breadcrumbs.length - 1;
              return (
                <span key={`${item.label}-${index}`} className="flex items-center gap-2">
                  {item.to && !isLast ? (
                    <Link to={item.to}>{item.label}</Link>
                  ) : (
                    <span className={isLast ? "text-slate-700" : undefined}>{item.label}</span>
                  )}
                  {!isLast && <span>/</span>}
                </span>
              );
            })}
          </nav>
          <Outlet />
        </div>
      </div>

      <Dialog
        open={passwordOpen}
        onOpenChange={(open) => {
          setPasswordOpen(open);
          if (!open) {
            setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
          }
        }}
      >
        <DialogContent className="sm:max-w-[420px]">
          <DialogHeader>
            <DialogTitle>修改密码</DialogTitle>
            <DialogDescription>请输入当前密码与新密码</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">当前密码</label>
              <Input
                type="password"
                value={passwordForm.currentPassword}
                onChange={(event) => setPasswordForm((prev) => ({ ...prev, currentPassword: event.target.value }))}
                placeholder="请输入当前密码"
                name="current-password"
                autoComplete="current-password"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">新密码</label>
              <Input
                type="password"
                value={passwordForm.newPassword}
                onChange={(event) => setPasswordForm((prev) => ({ ...prev, newPassword: event.target.value }))}
                placeholder="请输入新密码"
                name="new-password"
                autoComplete="new-password"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">确认新密码</label>
              <Input
                type="password"
                value={passwordForm.confirmPassword}
                onChange={(event) => setPasswordForm((prev) => ({ ...prev, confirmPassword: event.target.value }))}
                placeholder="再次输入新密码"
                name="confirm-new-password"
                autoComplete="new-password"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPasswordOpen(false)}>
              取消
            </Button>
            <Button onClick={handlePasswordSubmit} disabled={passwordSubmitting}>
              {passwordSubmitting ? "保存中..." : "保存"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
