package com.kododake.aabrowser.web

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.kododake.aabrowser.R
import com.kododake.aabrowser.model.UserAgentProfile

data class BrowserCallbacks(
    val onUrlChange: (String) -> Unit = {},
    val onTitleChange: (String?) -> Unit = {},
    val onFaviconReceived: (String, Bitmap?) -> Unit = { _, _ -> },
    val onProgressChange: (Int) -> Unit = {},
    val onShowDownloadPrompt: (Uri) -> Unit = {},
    val onError: (Int, String?) -> Unit = { _, _ -> },
    val onCleartextNavigationRequested: (
        Uri,
        allowOnce: () -> Unit,
        allowHostPermanently: () -> Unit,
        cancel: () -> Unit
    ) -> Unit = { _, _, _, cancel -> cancel() },
    val onEnterFullscreen: (View, WebChromeClient.CustomViewCallback) -> Unit = { _, _ -> },
    val onExitFullscreen: () -> Unit = {},
    val onPageStarted: (String) -> Unit = {},
    val onPermissionRequest: (PermissionRequest) -> Unit = { it.deny() }
)

fun configureWebView(
    webView: WebView,
    callbacks: BrowserCallbacks = BrowserCallbacks(),
    useDesktopMode: Boolean = false,
    userAgentProfile: UserAgentProfile = UserAgentProfile.ANDROID_CHROME,
    allowDarkPages: Boolean = false
) {
    with(webView) {
        setBackgroundColor(Color.TRANSPARENT)

        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = true

        WebView.setWebContentsDebuggingEnabled(false)

        val originalUserAgent = settings.userAgentString
        setTag(R.id.webview_original_user_agent_tag, originalUserAgent)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true

            setSupportMultipleWindows(true)

            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowContentAccess = true
            allowFileAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                offscreenPreRaster = true
            }
        }

        applyPageDarkening(allowDarkPages)
        applyUserAgent(userAgentProfile, useDesktopMode)

        CookieManager.getInstance().also {
            it.setAcceptCookie(true)
            it.setAcceptThirdPartyCookies(this, true)
        }

        //setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (handleCleartextIfNeeded(view, uri, callbacks, onPageStart = false)) return true
                return handleUri(view, uri)
            }

            private fun handleUri(view: WebView, uri: Uri?): Boolean {
                uri ?: return false
                val scheme = uri.scheme?.lowercase()
                if (scheme == null || scheme in setOf("http", "https", "about", "file", "data", "javascript")) {
                    return false
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val stringUrl = url ?: return
                callbacks.onPageStarted(stringUrl)

                if (stringUrl.contains("youtube.com")) {
                    view.evaluateJavascript(YOUTUBE_PREEMPTIVE_JS, null)
                }

                val uri = Uri.parse(stringUrl)
                val scheme = uri.scheme?.lowercase()

                if (scheme == "http") {
                    val allowedOnce = getTag(R.id.webview_allow_once_uri_tag) as? String
                    if (allowedOnce == stringUrl) {
                        setTag(R.id.webview_allow_once_uri_tag, null)
                    } else if (handleCleartextIfNeeded(view, uri, callbacks, onPageStart = true)) {
                        return
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                view.evaluateJavascript(SpeechRecognitionBridge.POLYFILL_JS, null)
                view.evaluateJavascript(RESTORE_UI_JS, null)
                if (url?.contains("youtube.com") == true) {
                    view.evaluateJavascript(YOUTUBE_LAYOUT_FIX_JS, null)
                }
                url?.let(callbacks.onUrlChange)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    val code = error.errorCode
                    val shouldShowErrorPage = when (code) {
                        WebViewClient.ERROR_HOST_LOOKUP,
                        WebViewClient.ERROR_CONNECT,
                        WebViewClient.ERROR_TIMEOUT,
                        WebViewClient.ERROR_UNKNOWN,
                        WebViewClient.ERROR_PROXY_AUTHENTICATION -> true
                        else -> false
                    }

                    if (shouldShowErrorPage) {
                        val failed = request.url?.toString().orEmpty()
                        val message = error.description?.toString().orEmpty()
                        val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(failed)}&code=$code&message=${Uri.encode(message)}"
                        try {
                            view.loadUrl(assetUrl)
                        } catch (_: Exception) {
                            callbacks.onError(code, error.description?.toString())
                        }
                        return
                    }
                }
                callbacks.onError(error.errorCode, error.description?.toString())
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.isForMainFrame) {
                    val code = errorResponse.statusCode
                    if (code in 400..599 && code != 429) {
                        val failed = request.url?.toString().orEmpty()
                        val message = errorResponse.reasonPhrase.orEmpty()
                        val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(failed)}&code=$code&message=${Uri.encode(message)}"
                        try {
                            view.loadUrl(assetUrl)
                        } catch (_: Exception) {
                            callbacks.onError(code, message)
                        }
                        return
                    }
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                val primary = try { error.primaryError } catch (_: Exception) { -1 }
                val url = error.url ?: ""
                val message = "SSL error: $primary"
                val assetUrl = "file:///android_asset/error.html?failedUrl=${Uri.encode(url)}&sslError=$primary&message=${Uri.encode(message)}"
                try {
                    view.loadUrl(assetUrl)
                    handler.cancel()
                    return
                } catch (_: Exception) {}

                handler.cancel()
                callbacks.onError(primary, message)
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                callbacks.onProgressChange(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                callbacks.onTitleChange(title)
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                val pageUrl = view?.url?.takeIf { it.isNotBlank() } ?: return
                callbacks.onFaviconReceived(pageUrl, icon)
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    callbacks.onEnterFullscreen(view, callback)
                    
                    // --- REVISED: Target only fullscreen video & save original styles ---
                    val js = """
                        (function() {
                            var fsEl = document.fullscreenElement || document.webkitFullscreenElement;
                            if (!fsEl) return;
                            
                            var videos = fsEl.tagName.toLowerCase() === 'video' ? [fsEl] : fsEl.querySelectorAll('video');
                            for(var i=0; i<videos.length; i++) {
                                var v = videos[i];
                                if (!v.hasAttribute('data-orig-fit')) {
                                    v.setAttribute('data-orig-fit', v.style.objectFit || '');
                                    v.setAttribute('data-orig-pos', v.style.position || '');
                                    v.setAttribute('data-orig-top', v.style.top || '');
                                    v.setAttribute('data-orig-left', v.style.left || '');
                                    v.setAttribute('data-orig-w', v.style.width || '');
                                    v.setAttribute('data-orig-h', v.style.height || '');
                                    v.setAttribute('data-orig-z', v.style.zIndex || '');
                                }
                                v.style.setProperty('position', 'fixed', 'important');
                                v.style.setProperty('top', '0', 'important');
                                v.style.setProperty('left', '0', 'important');
                                v.style.setProperty('width', '100vw', 'important');
                                v.style.setProperty('height', '100vh', 'important');
                                v.style.setProperty('z-index', '2147483647', 'important');
                                v.style.setProperty('object-fit', 'cover', 'important');
                                v.style.setProperty('object-position', 'center center', 'important');
                            }
                            
                            var selectorsToHide = [
                                '.ytp-chrome-bottom', '.ytp-chrome-top', '.ytp-gradient-bottom', 
                                '.ytp-gradient-top', '.ytp-show-cards-title', 'ytm-header-bar',
                                '.ytp-ce-element', '.ytp-pause-overlay', '.video-player__overlay',
                                '.vp-controls', '.vp-title', '.vp-nudge-wrapper',
                                '.jw-controls', '.jw-title', '.jw-overlays', '.dmp_Controls',
                                '.video-controls', '.player-controls', '.controls-container'
                            ];
                            selectorsToHide.forEach(s => {
                                document.querySelectorAll(s).forEach(el => el.style.setProperty('display', 'none', 'important'));
                            });

                            // Save and Lock the body/html
                            if (!document.documentElement.hasAttribute('data-orig-overflow')) {
                                document.documentElement.setAttribute('data-orig-overflow', document.documentElement.style.overflow || '');
                            }
                            if (!document.body.hasAttribute('data-orig-overflow')) {
                                document.body.setAttribute('data-orig-overflow', document.body.style.overflow || '');
                            }
                            document.documentElement.style.setProperty('overflow', 'hidden', 'important');
                            document.body.style.setProperty('overflow', 'hidden', 'important');
                        })();
                    """.trimIndent()
                    this@with.evaluateJavascript(js, null)
                    // -----------------------------------------------------------------
                    
                } else {
                    super.onShowCustomView(view, callback)
                }
            }

            override fun onHideCustomView() {
                callbacks.onExitFullscreen()
                this@with.evaluateJavascript("if(typeof restoreUI === 'function') { restoreUI(); }", null)
                super.onHideCustomView()
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return

                val allowed = setOf(
                    PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID,
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE
                )

                val grantable = request.resources.filter { it in allowed }.toTypedArray()

                if (grantable.isEmpty()) {
                    request.deny()
                    return
                }

                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in grantable) {
                    callbacks.onPermissionRequest(request)
                } else {
                    this@with.post { request.grant(grantable) }
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                return false
            }
        }

        setDownloadListener(DownloadListener { url, _, _, _, _ ->
            val uri = url?.takeIf { it.isNotBlank() }?.toUri() ?: return@DownloadListener
            callbacks.onShowDownloadPrompt(uri)
        })
    }
}

private fun handleCleartextIfNeeded(view: WebView, uri: Uri?, callbacks: BrowserCallbacks, onPageStart: Boolean = false): Boolean {
    uri ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http") return false

    val allowedOnce = view.getTag(R.id.webview_allow_once_uri_tag) as? String
    if (allowedOnce == uri.toString()) {
        view.setTag(R.id.webview_allow_once_uri_tag, null)
        return false
    }

    val host = uri.host?.lowercase()
    if (com.kododake.aabrowser.data.BrowserPreferences.isHostAllowedCleartext(view.context, host)) return false
    if (onPageStart) view.stopLoading()
    val allowOnce = {
        view.setTag(R.id.webview_allow_once_uri_tag, uri.toString())
        view.post { view.loadUrl(uri.toString()) }
        kotlin.Unit
    }
    val allowHost = {
        view.context?.let { ctx ->
            val hostToStore = uri.host?.lowercase()
            if (hostToStore != null) com.kododake.aabrowser.data.BrowserPreferences.addAllowedCleartextHost(ctx, hostToStore)
        }
        view.setTag(R.id.webview_allow_once_uri_tag, uri.toString())
        view.post { view.loadUrl(uri.toString()) }
        kotlin.Unit
    }
    val cancel = {
        if (onPageStart) view.stopLoading()
        kotlin.Unit
    }
    callbacks.onCleartextNavigationRequested(uri, allowOnce, allowHost, cancel)
    return true
}

fun WebView.updateDesktopMode(enable: Boolean, profile: UserAgentProfile) {
    applyUserAgent(profile, enable)
    reload()
}

fun WebView.updateUserAgentProfile(profile: UserAgentProfile, desktop: Boolean) {
    applyUserAgent(profile, desktop)
    reload()
}

fun WebView.updatePageDarkening(enabled: Boolean) {
    applyPageDarkening(enabled)
    reload()
}

fun WebView.releaseCompletely() {
    stopLoading()
    webChromeClient = WebChromeClient()
    webViewClient = WebViewClient()
    destroy()
}

private fun WebView.applyUserAgent(profile: UserAgentProfile, desktop: Boolean) {
    setTag(R.id.webview_user_agent_profile_tag, profile.storageKey)
    settings.userAgentString = buildUserAgent(profile, desktop)
    settings.useWideViewPort = desktop
    settings.loadWithOverviewMode = desktop
    
    val scale = if (desktop) {
        1 // Overview Mode handles scaling
    } else {
        (context.resources.displayMetrics.density * com.kododake.aabrowser.data.BrowserPreferences.getGlobalScalePercent(context)).toInt()
    }
    setInitialScale(scale)
}

private fun WebView.applyPageDarkening(enabled: Boolean) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, enabled)
    }
}

private fun buildUserAgent(profile: UserAgentProfile, desktop: Boolean): String {
    return when (profile) {
        UserAgentProfile.ANDROID_CHROME -> if (desktop) WINDOWS_CHROME_UA else MOBILE_CHROME_UA
        UserAgentProfile.SAFARI -> if (desktop) SAFARI_MAC_UA else SAFARI_IOS_UA
    }
}

private const val CHROME_VERSION = "144.0.0.0"
private const val MOBILE_CHROME_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${CHROME_VERSION} Mobile Safari/537.36"
private const val WINDOWS_CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${CHROME_VERSION} Safari/537.36"
private const val SAFARI_MAC_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0_0) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
private const val SAFARI_IOS_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

private const val RESTORE_UI_JS = """
    window.restoreUI = function() {
        var selectorsToHide = [
            '.ytp-chrome-bottom', '.ytp-chrome-top', '.ytp-gradient-bottom', 
            '.ytp-gradient-top', '.ytp-show-cards-title', 'ytm-header-bar',
            '.ytp-ce-element', '.ytp-pause-overlay', '.video-player__overlay',
            '.vp-controls', '.vp-title', '.vp-nudge-wrapper',
            '.jw-controls', '.jw-title', '.jw-overlays', '.dmp_Controls',
            '.video-controls', '.player-controls', '.controls-container'
        ];
        selectorsToHide.forEach(function(s) {
            document.querySelectorAll(s).forEach(function(el) {
                el.style.display = "";
            });
        });

        if (document.documentElement.hasAttribute('data-orig-overflow')) {
            document.documentElement.style.overflow = document.documentElement.getAttribute('data-orig-overflow');
            document.documentElement.removeAttribute('data-orig-overflow');
        }
        if (document.body.hasAttribute('data-orig-overflow')) {
            document.body.style.overflow = document.body.getAttribute('data-orig-overflow');
            document.body.removeAttribute('data-orig-overflow');
        }

        var videos = document.getElementsByTagName('video');
        for (var i = 0; i < videos.length; i++) {
            var v = videos[i];
            if (v.hasAttribute('data-orig-fit')) {
                v.style.objectFit = v.getAttribute('data-orig-fit');
                v.style.position = v.getAttribute('data-orig-pos');
                v.style.top = v.getAttribute('data-orig-top');
                v.style.left = v.getAttribute('data-orig-left');
                v.style.width = v.getAttribute('data-orig-w');
                v.style.height = v.getAttribute('data-orig-h');
                v.style.zIndex = v.getAttribute('data-orig-z');
                v.style.objectPosition = v.getAttribute('data-orig-object-position');
                v.style.transform = v.getAttribute('data-orig-transform');
                v.style.transition = v.getAttribute('data-orig-transition');
                v.style.transformOrigin = v.getAttribute('data-orig-transform-origin');

                v.removeAttribute('data-orig-fit');
                v.removeAttribute('data-orig-pos');
                v.removeAttribute('data-orig-top');
                v.removeAttribute('data-orig-left');
                v.removeAttribute('data-orig-w');
                v.removeAttribute('data-orig-h');
                v.removeAttribute('data-orig-z');
                v.removeAttribute('data-orig-object-position');
                v.removeAttribute('data-orig-transform');
                v.removeAttribute('data-orig-transition');
                v.removeAttribute('data-orig-transform-origin');
            } else {
                v.style.removeProperty('position');
                v.style.removeProperty('top');
                v.style.removeProperty('left');
                v.style.removeProperty('width');
                v.style.removeProperty('height');
                v.style.removeProperty('z-index');
                v.style.removeProperty('object-fit');
                v.style.removeProperty('object-position');
                v.style.removeProperty('transform');
                v.style.removeProperty('transition');
                v.style.removeProperty('transform-origin');
            }
        }
        window.dispatchEvent(new Event('resize'));
    };
"""

private const val YOUTUBE_PREEMPTIVE_JS = """
    (function() {
        var css = 'ytd-watch-flexy[flexy] #primary.ytd-watch-flexy { margin-left: 0 !important; margin-right: auto !important; } ytd-watch-flexy[flexy] #secondary.ytd-watch-flexy { display: block !important; }';
        var style = document.createElement('style');
        style.type = 'text/css';
        style.innerHTML = css;
        (document.head || document.documentElement).appendChild(style);
    })();
"""

private const val YOUTUBE_LAYOUT_FIX_JS = """
    (function() {
        function applyFix() {
            var watchPage = document.querySelector('ytd-watch-flexy');
            if (watchPage) {
                watchPage.setAttribute('is-two-columns-layout', '');
                watchPage.removeAttribute('theater');
                watchPage.removeAttribute('fullscreen');
                watchPage.removeAttribute('is-extra-wide-video');
                
                var playerData = watchPage.querySelector('#player-container-outer');
                if (playerData) {
                    playerData.style.marginLeft = '0';
                    playerData.style.marginRight = '0';
                }
            }
            var cinematic = document.querySelector('#cinematic-container');
            if (cinematic) cinematic.style.display = 'none';
            window.dispatchEvent(new Event('resize'));
        }
        applyFix();
        // Handle YouTube's SPA navigation
        document.addEventListener('yt-navigate-finish', applyFix);
        // Fallback for dynamic layout shifts
        var observer = new MutationObserver(function(mutations) {
            if (document.querySelector('ytd-watch-flexy') && !document.querySelector('ytd-watch-flexy').hasAttribute('is-two-columns-layout')) {
                applyFix();
            }
        });
        observer.observe(document.documentElement, { attributes: true, childList: true, subtree: true });
    })();
"""
