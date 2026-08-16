package com.fanni.shell;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * 凡逆RPG 安卓原生壳
 * 纯WebView加载线上地址 http://124.156.183.73/
 * 服务器更新即时生效（LOAD_NO_CACHE 不缓存）
 */
public class MainActivity extends Activity {

    private static final String HOME_URL = "http://124.156.183.73/";
    /** 加载超过10秒提示网络较慢 */
    private static final long SLOW_NET_MS = 10_000L;

    private WebView webView;
    /** 启动画面覆盖层 */
    private FrameLayout splashOverlay;
    /** 错误页覆盖层 */
    private FrameLayout errorOverlay;
    private TextView splashTip;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean pageLoaded = false;

    private final Runnable slowNetRunnable = new Runnable() {
        @Override
        public void run() {
            if (!pageLoaded && splashOverlay.getVisibility() == View.VISIBLE) {
                splashTip.setText("网络较慢，请稍候…");
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        splashOverlay = findViewById(R.id.splash_overlay);
        errorOverlay = findViewById(R.id.error_overlay);
        splashTip = findViewById(R.id.splash_tip);
        webView = findViewById(R.id.webview);
        Button retryBtn = findViewById(R.id.btn_retry);

        setupWebView();
        applyImmersiveFullscreen();

        retryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideError();
                showSplash();
                webView.loadUrl(HOME_URL);
            }
        });

        startLoad();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);                       // 启用JS
        s.setDomStorageEnabled(true);                       // DOM存储(localStorage存档)
        s.setDatabaseEnabled(true);                         // 数据库存储
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);          // 强制不缓存，服务器更新即时生效
        s.setSupportZoom(false);                            // 禁缩放
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        // User-Agent追加标识，服务端可识别APK请求
        String ua = s.getUserAgentString();
        if (ua == null || !ua.contains("FanNiApp/")) {
            s.setUserAgentString((ua == null ? "" : ua) + " FanNiApp/1.0");
        }

        // 禁用下拉刷新/边缘发光效果
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && url.startsWith("http")) {
                    pageLoaded = true;
                    hideSplash();
                }
            }

            // API 23+ ：只处理主框架错误，子资源失败不弹错误页
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
                    showError();
                }
            }

            // API 21~22 走旧签名
            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl != null && failingUrl.contains("124.156.183.73")) {
                    showError();
                }
            }
        });
    }

    private void startLoad() {
        showSplash();
        webView.loadUrl(HOME_URL);
    }

    private void showSplash() {
        pageLoaded = false;
        splashTip.setText("加载中…");
        splashOverlay.setVisibility(View.VISIBLE);
        handler.removeCallbacks(slowNetRunnable);
        handler.postDelayed(slowNetRunnable, SLOW_NET_MS);
    }

    private void hideSplash() {
        handler.removeCallbacks(slowNetRunnable);
        splashOverlay.setVisibility(View.GONE);
    }

    private void showError() {
        handler.removeCallbacks(slowNetRunnable);
        splashOverlay.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorOverlay.setVisibility(View.GONE);
    }

    /** 全屏沉浸（无状态栏无导航栏），窗口重新获得焦点时再补一次（防下拉呼出） */
    private void applyImmersiveFullscreen() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveFullscreen();
        }
    }

    /** 安卓返回键：可后退则后退，首页弹退出确认 */
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (errorOverlay.getVisibility() == View.VISIBLE) {
            // 错误页时返回=退出确认
            showExitDialog();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            showExitDialog();
        }
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setMessage("确定退出游戏吗？")
                .setPositiveButton("退出", (d, w) -> finish())
                .setNegativeButton("取消", null)
                .setCancelable(true)
                .show();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
