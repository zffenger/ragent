import React, { useState, useRef, useCallback, useEffect } from "react";
import { RefreshCw } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface SliderCaptchaProps {
  onVerify: (success: boolean, token?: string) => void;
  onRefresh?: () => void;
  className?: string;
}

interface CaptchaData {
  id: string;
  backgroundImage: string;
  templateImage: string;
  backgroundImageWidth: number;
  backgroundImageHeight: number;
  templateImageWidth: number;
  templateImageHeight: number;
}

interface Track {
  t: number;
  x: number;
  y: number;
  type: "move" | "down" | "up";
}

// 最大显示宽度
const MAX_DISPLAY_WIDTH = 320;
// API 基础路径
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

export function SliderCaptcha({ onVerify, onRefresh, className }: SliderCaptchaProps) {
  const [captchaData, setCaptchaData] = useState<CaptchaData | null>(null);
  const [sliderX, setSliderX] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const [isVerified, setIsVerified] = useState(false);
  const [verifyFailed, setVerifyFailed] = useState(false);
  const [loading, setLoading] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);
  const startTimeRef = useRef<number>(0);
  const trackListRef = useRef<Track[]>([]);
  const startXRef = useRef(0);

  // 获取验证码
  const fetchCaptcha = useCallback(async () => {
    setLoading(true);
    setSliderX(0);
    setIsVerified(false);
    setVerifyFailed(false);
    trackListRef.current = [];

    try {
      const response = await fetch(`${API_BASE_URL}/captcha/generate`);
      const data = await response.json();
      if (data.success && data.data) {
        setCaptchaData({
          id: data.data.id,
          backgroundImage: data.data.backgroundImage,
          templateImage: data.data.templateImage,
          backgroundImageWidth: data.data.backgroundImageWidth || 280,
          backgroundImageHeight: data.data.backgroundImageHeight || 160,
          templateImageWidth: data.data.templateImageWidth || 50,
          templateImageHeight: data.data.templateImageHeight || 160,
        });
      }
    } catch (error) {
      console.error("获取验证码失败", error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCaptcha();
  }, [fetchCaptcha]);

  // 计算显示缩放比例（保持宽高比）
  const getDisplayScale = useCallback(() => {
    if (!captchaData) return 1;
    // 如果原图宽度大于最大显示宽度，则缩放
    if (captchaData.backgroundImageWidth > MAX_DISPLAY_WIDTH) {
      return MAX_DISPLAY_WIDTH / captchaData.backgroundImageWidth;
    }
    return 1;
  }, [captchaData]);

  // 验证滑块位置
  const verifyCaptcha = useCallback(async (displayX: number) => {
    if (!captchaData) return;

    const stopTime = Date.now();
    const scale = getDisplayScale();
    // 显示位置转换为实际位置
    const actualX = Math.round(displayX / scale);

    // 构建验证数据
    const verifyData = {
      bgImageWidth: captchaData.backgroundImageWidth,
      bgImageHeight: captchaData.backgroundImageHeight,
      templateImageWidth: captchaData.templateImageWidth,
      templateImageHeight: captchaData.templateImageHeight,
      startTime: startTimeRef.current,
      stopTime: stopTime,
      left: actualX,
      top: 0,
      trackList: trackListRef.current.map((t) => ({
        t: t.t - startTimeRef.current,
        x: Math.round(t.x / scale),
        y: t.y,
        type: t.type,
      })),
    };

    try {
      const response = await fetch(`${API_BASE_URL}/captcha/check`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          id: captchaData.id,
          data: verifyData,
        }),
      });
      const result = await response.json();

      if (result.success && result.data?.success) {
        setIsVerified(true);
        onVerify(true, result.data.token);
      } else {
        setVerifyFailed(true);
        onVerify(false);
        setTimeout(() => {
          fetchCaptcha();
        }, 1000);
      }
    } catch (error) {
      console.error("验证失败", error);
      setVerifyFailed(true);
      onVerify(false);
    }
  }, [captchaData, getDisplayScale, onVerify, fetchCaptcha]);

  // 鼠标/触摸事件处理
  const handleMouseDown = (e: React.MouseEvent | React.TouchEvent) => {
    if (isVerified || !captchaData) return;

    setIsDragging(true);
    setVerifyFailed(false);
    startTimeRef.current = Date.now();
    trackListRef.current = [];

    const clientX = "touches" in e ? e.touches[0].clientX : e.clientX;
    startXRef.current = clientX - sliderX;

    trackListRef.current.push({
      t: Date.now(),
      x: sliderX,
      y: 0,
      type: "down",
    });
  };

  const handleMouseMove = useCallback((e: MouseEvent | TouchEvent) => {
    if (!isDragging || !captchaData) return;

    const clientX = "touches" in e ? e.touches[0].clientX : e.clientX;
    const scale = getDisplayScale();
    const displayBgWidth = captchaData.backgroundImageWidth * scale;
    const displaySliderWidth = captchaData.templateImageWidth * scale;
    const maxWidth = displayBgWidth - displaySliderWidth;

    let newX = clientX - startXRef.current;
    newX = Math.max(0, Math.min(newX, maxWidth));
    setSliderX(newX);

    trackListRef.current.push({
      t: Date.now(),
      x: Math.round(newX),
      y: 0,
      type: "move",
    });
  }, [isDragging, captchaData, getDisplayScale]);

  const handleMouseUp = useCallback(() => {
    if (!isDragging) return;
    setIsDragging(false);

    trackListRef.current.push({
      t: Date.now(),
      x: sliderX,
      y: 0,
      type: "up",
    });

    if (sliderX > 10) {
      verifyCaptcha(sliderX);
    }
  }, [isDragging, sliderX, verifyCaptcha]);

  useEffect(() => {
    if (isDragging) {
      document.addEventListener("mousemove", handleMouseMove);
      document.addEventListener("mouseup", handleMouseUp);
      document.addEventListener("touchmove", handleMouseMove);
      document.addEventListener("touchend", handleMouseUp);
    }
    return () => {
      document.removeEventListener("mousemove", handleMouseMove);
      document.removeEventListener("mouseup", handleMouseUp);
      document.removeEventListener("touchmove", handleMouseMove);
      document.removeEventListener("touchend", handleMouseUp);
    };
  }, [isDragging, handleMouseMove, handleMouseUp]);

  const handleRefresh = () => {
    fetchCaptcha();
    onRefresh?.();
  };

  if (loading) {
    return (
      <div className={cn("flex items-center justify-center h-[180px] bg-muted rounded-lg", className)}>
        <span className="text-sm text-muted-foreground">加载中...</span>
      </div>
    );
  }

  if (!captchaData) {
    return (
      <div className={cn("flex items-center justify-center h-[180px] bg-muted rounded-lg gap-2", className)}>
        <span className="text-sm text-muted-foreground">验证码加载失败</span>
        <Button variant="ghost" size="sm" onClick={handleRefresh}>
          <RefreshCw className="h-4 w-4" />
        </Button>
      </div>
    );
  }

  const scale = getDisplayScale();
  const displayBgWidth = captchaData.backgroundImageWidth * scale;
  const displayBgHeight = captchaData.backgroundImageHeight * scale;
  const displaySliderWidth = captchaData.templateImageWidth * scale;
  const displaySliderHeight = captchaData.templateImageHeight * scale;

  return (
    <div className={cn("relative select-none", className)}>
      {/* 背景图片 */}
      <div
        ref={containerRef}
        className="relative bg-muted rounded-lg overflow-hidden"
        style={{
          width: displayBgWidth,
          height: displayBgHeight,
        }}
      >
        {captchaData.backgroundImage && (
          <img
            src={captchaData.backgroundImage}
            alt="验证码背景"
            className="block"
            style={{
              width: displayBgWidth,
              height: displayBgHeight,
            }}
            draggable={false}
          />
        )}

        {/* 滑块图片 */}
        {captchaData.templateImage && (
          <img
            src={captchaData.templateImage}
            alt="滑块"
            className="absolute pointer-events-none"
            style={{
              left: sliderX,
              top: 0,
              width: displaySliderWidth,
              height: displaySliderHeight,
            }}
            draggable={false}
          />
        )}

        {/* 验证成功提示 */}
        {isVerified && (
          <div className="absolute inset-0 flex items-center justify-center bg-green-500/80">
            <span className="text-white font-medium">验证成功</span>
          </div>
        )}

        {/* 验证失败提示 */}
        {verifyFailed && (
          <div className="absolute inset-0 flex items-center justify-center bg-red-500/80">
            <span className="text-white font-medium">验证失败，请重试</span>
          </div>
        )}

        {/* 刷新按钮 */}
        {!isVerified && (
          <Button
            variant="ghost"
            size="sm"
            className="absolute top-2 right-2 h-7 w-7 p-0 bg-black/30 hover:bg-black/50"
            onClick={handleRefresh}
          >
            <RefreshCw className="h-4 w-4 text-white" />
          </Button>
        )}
      </div>

      {/* 滑动条 */}
      <div
        className="relative mt-2 bg-muted rounded border h-10"
        style={{ width: displayBgWidth }}
      >
        <div
          className={cn(
            "absolute top-0 left-0 h-full flex items-center justify-center cursor-grab rounded-l border-r transition-colors",
            isVerified ? "bg-green-500" : "bg-primary hover:bg-primary/90",
            isDragging && "cursor-grabbing"
          )}
          style={{
            width: 50,
            transform: `translateX(${sliderX}px)`,
          }}
          onMouseDown={handleMouseDown}
          onTouchStart={handleMouseDown}
        >
          <span className="text-white text-lg">→</span>
        </div>
        <div className="flex items-center justify-center h-full text-sm text-muted-foreground">
          {isVerified ? "验证通过" : "向右滑动完成验证"}
        </div>
      </div>
    </div>
  );
}
