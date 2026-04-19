import * as React from "react";
import { useNavigate, useParams } from "react-router-dom";
import { MessageSquare, Plus, Search, MoreHorizontal, Pencil, Trash2, Bot } from "lucide-react";
import { differenceInCalendarDays, isValid } from "date-fns";

import { ChatInput } from "@/components/chat/ChatInput";
import { MessageList } from "@/components/chat/MessageList";
import { useChatStore } from "@/stores/chatStore";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function AdminChatPage() {
  const navigate = useNavigate();
  const { sessionId } = useParams<{ sessionId: string }>();
  const {
    messages,
    isLoading,
    isStreaming,
    currentSessionId,
    sessions,
    sessionsLoaded,
    isCreatingNew,
    fetchSessions,
    selectSession,
    createSession,
    deleteSession,
    renameSession,
  } = useChatStore();

  const [query, setQuery] = React.useState("");
  const [renamingId, setRenamingId] = React.useState<string | null>(null);
  const [renameValue, setRenameValue] = React.useState("");
  const renameInputRef = React.useRef<HTMLInputElement | null>(null);

  const showWelcome = messages.length === 0 && !isLoading;
  const sessionExists = React.useMemo(() => {
    if (!sessionId) return false;
    return sessions.some((session) => session.id === sessionId);
  }, [sessionId, sessions]);

  React.useEffect(() => {
    fetchSessions().catch(() => null);
  }, [fetchSessions]);

  React.useEffect(() => {
    if (sessionId) {
      if (sessionsLoaded && !sessionExists) {
        createSession().catch(() => null);
        navigate("/admin/chat", { replace: true });
        return;
      }
      selectSession(sessionId).catch(() => null);
      return;
    }
    if (!sessionsLoaded) {
      return;
    }
    if (isCreatingNew) {
      return;
    }
    if (currentSessionId) {
      return;
    }
  }, [
    sessionId,
    sessionsLoaded,
    sessionExists,
    isCreatingNew,
    currentSessionId,
    selectSession,
    createSession,
    navigate,
  ]);

  React.useEffect(() => {
    if (currentSessionId && currentSessionId !== sessionId) {
      navigate(`/admin/chat/${currentSessionId}`, { replace: true });
    }
  }, [currentSessionId, sessionId, navigate]);

  React.useEffect(() => {
    if (renamingId) {
      renameInputRef.current?.focus();
      renameInputRef.current?.select();
    }
  }, [renamingId]);

  // 筛选会话
  const filteredSessions = React.useMemo(() => {
    const keyword = query.trim().toLowerCase();
    if (!keyword) return sessions;
    return sessions.filter((session) => {
      const title = (session.title || "新对话").toLowerCase();
      return title.includes(keyword) || session.id.toLowerCase().includes(keyword);
    });
  }, [query, sessions]);

  // 按日期分组
  const groupedSessions = React.useMemo(() => {
    const now = new Date();
    const groups = new Map<string, typeof filteredSessions>();
    const order: string[] = [];

    const resolveLabel = (value?: string) => {
      const parsed = value ? new Date(value) : now;
      const date = isValid(parsed) ? parsed : now;
      const diff = Math.max(0, differenceInCalendarDays(now, date));
      if (diff === 0) return "今天";
      if (diff <= 7) return "7天内";
      if (diff <= 30) return "30天内";
      return "更早";
    };

    filteredSessions.forEach((session) => {
      const label = resolveLabel(session.lastTime);
      if (!groups.has(label)) {
        groups.set(label, []);
        order.push(label);
      }
      groups.get(label)?.push(session);
    });

    return order.map((label) => ({
      label,
      items: groups.get(label) || [],
    }));
  }, [filteredSessions]);

  const startRename = (id: string, title: string) => {
    setRenamingId(id);
    setRenameValue(title || "新对话");
  };

  const cancelRename = () => {
    setRenamingId(null);
    setRenameValue("");
  };

  const commitRename = async () => {
    if (!renamingId) return;
    const nextTitle = renameValue.trim();
    if (!nextTitle) {
      cancelRename();
      return;
    }
    const currentTitle = sessions.find((session) => session.id === renamingId)?.title || "新对话";
    if (nextTitle === currentTitle) {
      cancelRename();
      return;
    }
    await renameSession(renamingId, nextTitle);
    cancelRename();
  };

  const handleDeleteSession = async (id: string) => {
    if (!confirm("确定要删除此会话吗？")) return;
    try {
      await deleteSession(id);
      if (currentSessionId === id) {
        navigate("/admin/chat", { replace: true });
      }
      toast.success("删除成功");
    } catch (error) {
      toast.error("删除失败");
    }
  };

  const handleNewChat = async () => {
    await createSession();
    navigate("/admin/chat");
  };

  const sessionTitleFont =
    "-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"PingFang SC\", \"Hiragino Sans GB\", \"Microsoft YaHei\", \"Helvetica Neue\", Arial, sans-serif";

  return (
    <div className="flex h-[calc(100vh-120px)] gap-0">
      {/* 左侧会话列表 */}
      <div className="w-[280px] flex-shrink-0 border-r bg-[#FAFAFA] flex flex-col">
        <div className="p-3 border-b">
          <Button onClick={handleNewChat} className="w-full" size="sm">
            <Plus className="mr-2 h-4 w-4" />
            新建对话
          </Button>
        </div>
        <div className="p-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="搜索对话..."
              className="pl-9 h-9"
            />
          </div>
        </div>
        <div className="flex-1 min-h-0 overflow-y-auto">
          {sessions.length === 0 && !sessionsLoaded ? (
            <div className="flex items-center justify-center h-32 text-muted-foreground text-sm">
              加载中...
            </div>
          ) : groupedSessions.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-32 text-muted-foreground">
              <MessageSquare className="h-8 w-8 mb-2" />
              <p className="text-sm">暂无对话记录</p>
            </div>
          ) : (
            <div className="px-2" style={{ fontFamily: sessionTitleFont }}>
              {groupedSessions.map((group, index) => (
                <div key={group.label} className={cn("flex flex-col", index === 0 ? "mt-0" : "mt-4")}>
                  <p className="mb-1.5 px-2 text-[12px] font-normal leading-[18px] text-muted-foreground">
                    {group.label}
                  </p>
                  {group.items.map((session) => (
                    <div
                      key={session.id}
                      className={cn(
                        "group flex min-h-[36px] cursor-pointer items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-[14px] leading-[22px] transition-colors",
                        currentSessionId === session.id
                          ? "bg-[#DBEAFE] text-[#2563EB]"
                          : "text-[#333333] hover:bg-[#F5F5F5]"
                      )}
                      onClick={() => {
                        if (renamingId === session.id) return;
                        selectSession(session.id).catch(() => null);
                        navigate(`/admin/chat/${session.id}`);
                      }}
                    >
                      {renamingId === session.id ? (
                        <input
                          ref={renameInputRef}
                          value={renameValue}
                          onChange={(e) => setRenameValue(e.target.value)}
                          onClick={(e) => e.stopPropagation()}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") {
                              e.preventDefault();
                              commitRename();
                            }
                            if (e.key === "Escape") {
                              e.preventDefault();
                              cancelRename();
                            }
                          }}
                          onBlur={commitRename}
                          className="h-6 flex-1 rounded-md border bg-white px-2 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                        />
                      ) : (
                        <span className="min-w-0 flex-1 truncate font-normal">
                          {session.title || "新对话"}
                        </span>
                      )}
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <button
                            type="button"
                            className={cn(
                              "flex h-6 w-6 items-center justify-center rounded text-muted-foreground hover:bg-muted",
                              currentSessionId === session.id
                                ? "opacity-100"
                                : "pointer-events-none opacity-0 group-hover:pointer-events-auto group-hover:opacity-100"
                            )}
                            onClick={(e) => e.stopPropagation()}
                          >
                            <MoreHorizontal className="h-4 w-4" />
                          </button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="start">
                          <DropdownMenuItem onClick={(e) => {
                            e.stopPropagation();
                            startRename(session.id, session.title || "新对话");
                          }}>
                            <Pencil className="mr-2 h-4 w-4" />
                            重命名
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteSession(session.id);
                            }}
                            className="text-destructive focus:text-destructive"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            删除
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 右侧聊天区域 */}
      <div className="flex-1 flex flex-col bg-white min-w-0">
        <div className="flex-1 min-h-0">
          <MessageList
            messages={messages}
            isLoading={isLoading}
            isStreaming={isStreaming}
            sessionKey={currentSessionId}
          />
        </div>
        {showWelcome ? null : (
          <div className="relative z-20 bg-white border-t">
            <div className="mx-auto max-w-[800px] px-6 pt-3 pb-4">
              <ChatInput />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
