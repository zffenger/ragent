import * as React from "react";
import { Eye, EyeOff, Lock, User } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { SliderCaptcha } from "@/components/common/SliderCaptcha";
import { useAuthStore } from "@/stores/authStore";
import { getFeishuAuthorizeUrl, feishuCallback } from "@/services/feishuOAuthService";
import { toast } from "sonner";

const FEISHU_OAUTH_ENABLED = import.meta.env.VITE_FEISHU_OAUTH_ENABLED === "true";

export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login, isLoading } = useAuthStore();
  const [showPassword, setShowPassword] = React.useState(false);
  const [remember, setRemember] = React.useState(true);
  const [form, setForm] = React.useState({ username: "admin", password: "admin" });
  const [error, setError] = React.useState<string | null>(null);
  const [feishuLoading, setFeishuLoading] = React.useState(false);
  const [captchaVerified, setCaptchaVerified] = React.useState(false);
  const [captchaToken, setCaptchaToken] = React.useState<string | null>(null);

  // 处理飞书 OAuth 回调
  React.useEffect(() => {
    const code = searchParams.get("code");
    const state = searchParams.get("state");
    if (code) {
      setFeishuLoading(true);
      feishuCallback(code, state || undefined)
        .then((result) => {
          // 使用飞书登录结果
          useAuthStore.getState().setUser({
            userId: result.userId,
            username: result.username,
            role: result.role,
            token: result.token,
            avatar: result.avatar,
          });
          toast.success("飞书登录成功");
          navigate("/admin/chat", { replace: true });
        })
        .catch((err) => {
          toast.error(err.message || "飞书登录失败");
          // 清除 URL 中的 code 参数
          navigate("/login", { replace: true });
        })
        .finally(() => {
          setFeishuLoading(false);
        });
    }
  }, [searchParams, navigate]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!form.username.trim() || !form.password.trim()) {
      setError("请输入用户名和密码。");
      return;
    }

    if (!captchaVerified) {
      setError("请先完成滑块验证。");
      return;
    }

    try {
      await login(form.username.trim(), form.password.trim(), captchaToken);
      if (!remember) {
        // 如需仅在内存中保存登录态，可在此扩展。
      }
      navigate("/admin/chat");
    } catch (err) {
      setError((err as Error).message || "登录失败，请稍后重试。");
      // 登录失败后重置验证码
      setCaptchaVerified(false);
      setCaptchaToken(null);
    }
  };

  const handleFeishuLogin = async () => {
    try {
      const redirectUri = `${window.location.origin}/login`;
      const authorizeUrl = await getFeishuAuthorizeUrl(redirectUri);
      window.location.href = authorizeUrl;
    } catch (err) {
      toast.error((err as Error).message || "获取飞书授权链接失败");
    }
  };

  const handleCaptchaVerify = (success: boolean, token?: string) => {
    setCaptchaVerified(success);
    setCaptchaToken(token || null);
    if (success) {
      setError(null);
    }
  };

  const handleCaptchaRefresh = () => {
    setCaptchaVerified(false);
    setCaptchaToken(null);
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4">
      <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-blue-50/50 to-blue-100 dark:from-slate-950 dark:via-slate-900 dark:to-slate-900" />
      <div className="relative z-10 w-full max-w-md rounded-3xl border border-border/70 bg-background/80 p-8 shadow-soft backdrop-blur">
        <div className="mb-6">
          <p className="font-display text-2xl font-semibold">欢迎回来</p>
          <p className="mt-1 text-sm text-muted-foreground">
            登录后继续你的检索增强对话。
          </p>
        </div>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              用户名
            </label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="请输入用户名"
                value={form.username}
                onChange={(event) => setForm((prev) => ({ ...prev, username: event.target.value }))}
                className="pl-10"
                autoComplete="username"
              />
            </div>
          </div>
          <div className="space-y-2">
            <label className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
              密码
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                type={showPassword ? "text" : "password"}
                placeholder="请输入密码"
                value={form.password}
                onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
                className="pl-10 pr-10"
                autoComplete="current-password"
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                aria-label="显示或隐藏密码"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>

          {/* 滑块验证码 */}
          <div className="space-y-2">
            <SliderCaptcha
              onVerify={handleCaptchaVerify}
              onRefresh={handleCaptchaRefresh}
            />
          </div>

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center gap-2 text-muted-foreground">
              <Checkbox checked={remember} onCheckedChange={(value) => setRemember(Boolean(value))} />
              记住我
            </label>
            <span className="text-xs text-muted-foreground">账号由管理员初始化</span>
          </div>
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
          <Button type="submit" className="w-full" disabled={isLoading || feishuLoading || !captchaVerified}>
            {isLoading ? "正在登录..." : "登录"}
          </Button>
        </form>

        {/* 飞书登录 */}
        {FEISHU_OAUTH_ENABLED && (
          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-background px-2 text-muted-foreground">或</span>
              </div>
            </div>
            <Button
              type="button"
              variant="outline"
              className="mt-4 w-full"
              onClick={handleFeishuLogin}
              disabled={feishuLoading || isLoading}
            >
              <svg className="mr-2 h-4 w-4" viewBox="0 0 48 48" fill="none">
                <path d="M10 8c0 1 7 3.5 14.745 16.744 0 0 4.184-4.363 6.255-5.744 1.5-1 2.712-1.332 2.712-1.332C33.712 15.156 29.5 8 28 8z" fill="#00d6b9"/>
                <path d="M43.5 18.5c-1-.667-3.65-1.771-6.5-1.5a15 15 0 0 0-3.288.668S32.5 18 31 19c-2.07 1.38-6.255 5.744-6.255 5.744-1.428 1.397-3.05 2.732-5.245 3.756 0 0 7 3 11.5 3 5.063 0 7-3.5 7-3.5 1.5-3.305 3.5-7 5.5-9.5" fill="#163c9a"/>
                <path d="M4 17.5v17c0 1 6 5.5 15 5.5 10 0 17.05-7.705 19-12 0 0-1.937 3.5-7 3.5-4.5 0-11.5-3-11.5-3-5.117-2.239-10.03-6.577-12.906-9.117C4.974 17.953 4 17.093 4 17.5" fill="#3370ff"/>
              </svg>
              {feishuLoading ? "正在登录..." : "飞书登录"}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
