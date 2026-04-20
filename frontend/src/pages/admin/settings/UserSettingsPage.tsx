import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Link, Unlink, User } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar } from "@/components/common/Avatar";
import { useAuthStore } from "@/stores/authStore";
import {
  getFeishuBinding,
  bindFeishuAccount,
  unbindFeishuAccount,
  getFeishuAuthorizeUrl,
  type FeishuUser,
} from "@/services/feishuOAuthService";
import { getErrorMessage } from "@/utils/error";

export function UserSettingsPage() {
  const { user } = useAuthStore();
  const [feishuBinding, setFeishuBinding] = useState<FeishuUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [binding, setBinding] = useState(false);

  const loadBinding = async () => {
    try {
      setLoading(true);
      const binding = await getFeishuBinding();
      setFeishuBinding(binding);
    } catch (error) {
      console.error("获取飞书绑定信息失败", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBinding();
  }, []);

  const handleBindFeishu = async () => {
    try {
      setBinding(true);
      const redirectUri = `${window.location.origin}/admin/settings/user/bind-feishu`;
      const authorizeUrl = await getFeishuAuthorizeUrl(redirectUri);
      // 保存当前路径以便回调后返回
      sessionStorage.setItem("feishu_bind_redirect", "/admin/settings/user");
      window.location.href = authorizeUrl;
    } catch (error) {
      toast.error(getErrorMessage(error, "获取飞书授权链接失败"));
      setBinding(false);
    }
  };

  const handleUnbindFeishu = async () => {
    if (!confirm("确定要解绑飞书账号吗？解绑后将无法使用飞书登录。")) {
      return;
    }
    try {
      setBinding(true);
      await unbindFeishuAccount();
      toast.success("飞书账号已解绑");
      setFeishuBinding(null);
    } catch (error) {
      toast.error(getErrorMessage(error, "解绑失败"));
    } finally {
      setBinding(false);
    }
  };

  return (
    <div className="admin-page">
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">个人设置</h1>
          <p className="admin-page-subtitle">管理您的账号和绑定信息</p>
        </div>
      </div>

      <div className="grid gap-6 max-w-2xl">
        {/* 基本信息 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">基本信息</CardTitle>
            <CardDescription>您的账号基本信息</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <Avatar
                name={user?.username || "用户"}
                src={user?.avatar}
                className="h-16 w-16 text-lg"
              />
              <div>
                <div className="font-medium text-lg">{user?.username}</div>
                <div className="text-sm text-muted-foreground">
                  {user?.role === "admin" ? "管理员" : "成员"}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 飞书账号绑定 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">飞书账号绑定</CardTitle>
            <CardDescription>
              绑定飞书账号后可使用飞书扫码登录系统
            </CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="text-sm text-muted-foreground">加载中...</div>
            ) : feishuBinding ? (
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {feishuBinding.avatar && (
                    <img
                      src={feishuBinding.avatar}
                      alt={feishuBinding.name || "飞书头像"}
                      className="h-10 w-10 rounded-full"
                    />
                  )}
                  <div>
                    <div className="font-medium">{feishuBinding.name || "飞书用户"}</div>
                    <div className="text-sm text-muted-foreground">
                      已绑定 · {feishuBinding.username}
                    </div>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleUnbindFeishu}
                  disabled={binding}
                >
                  <Unlink className="mr-2 h-4 w-4" />
                  解绑
                </Button>
              </div>
            ) : (
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-muted">
                    <User className="h-5 w-5 text-muted-foreground" />
                  </div>
                  <div>
                    <div className="font-medium text-muted-foreground">未绑定飞书账号</div>
                    <div className="text-sm text-muted-foreground">
                      绑定后可使用飞书扫码登录
                    </div>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleBindFeishu}
                  disabled={binding}
                >
                  <Link className="mr-2 h-4 w-4" />
                  绑定飞书
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
