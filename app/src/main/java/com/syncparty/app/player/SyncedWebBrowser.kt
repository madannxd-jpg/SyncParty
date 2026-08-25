package com.syncparty.app.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.syncparty.app.theme.*

class WebAppInterface(
    private val onVideoEvent: (action: String, time: Float) -> Unit
) {
    @JavascriptInterface
    fun onMediaAction(action: String, time: Float) {
        onVideoEvent(action, time)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SyncedWebBrowser(
    initialUrl: String = "https://www.crunchyroll.com",
    isHost: Boolean = true,
    onUrlChanged: (String) -> Unit = {},
    onVideoSync: (action: String, time: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrlText by remember { mutableStateOf(initialUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }

    val quickLogins = listOf(
        "Crunchyroll Login" to "https://sso.crunchyroll.com/login",
        "Instagram Login" to "https://www.instagram.com/accounts/login/",
        "Crunchyroll Home" to "https://www.crunchyroll.com",
        "Instagram Feed" to "https://www.instagram.com",
        "AnimePahe" to "https://animepahe.ru",
        "YouTube Web" to "https://m.youtube.com"
    )

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Host Master Status Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isHost) PrimaryPurple.copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isHost) PrimaryPurple else AccentCyan, shape = MaterialTheme.shapes.extraSmall)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHost) "👑 Host Mode: You control playback, login & episodes for all" else "👀 Guest View: Synced with Host",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Cookies Saved ✓",
                style = MaterialTheme.typography.labelSmall,
                color = AccentGreen
            )
        }

        // Browser Navigation Bar & URL Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { webView?.goBack() },
                enabled = webView?.canGoBack() == true
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(
                onClick = { webView?.goForward() },
                enabled = webView?.canGoForward() == true
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { webView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = MaterialTheme.colorScheme.onSurface)
            }

            OutlinedTextField(
                value = currentUrlText,
                onValueChange = { currentUrlText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        var target = currentUrlText.trim()
                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                            target = "https://$target"
                        }
                        webView?.loadUrl(target)
                        onUrlChanged(target)
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Go", tint = AccentCyan)
                    }
                }
            )

            // Desktop / Mobile Mode Toggle
            IconButton(onClick = {
                isDesktopMode = !isDesktopMode
                webView?.settings?.userAgentString = if (isDesktopMode) {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                } else {
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }
                webView?.reload()
            }) {
                Icon(
                    imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                    contentDescription = "Toggle Desktop/Mobile",
                    tint = if (isDesktopMode) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick In-App Login & Bookmark Shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickLogins.forEach { (name, url) ->
                val isLogin = name.contains("Login")
                SuggestionChip(
                    onClick = {
                        currentUrlText = url
                        webView?.loadUrl(url)
                        onUrlChanged(url)
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLogin) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                name,
                                color = if (isLogin) AccentOrange else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isLogin) AccentOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, if (isLogin) AccentOrange.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = AccentCyan
            )
        }

        // WebView In-App Browser Stage
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Enable persistent cookie and session storage
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }

                    addJavascriptInterface(
                        WebAppInterface { action, time ->
                            if (isHost) {
                                onVideoSync(action, time)
                            }
                        },
                        "AndroidSyncParty"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            url?.let { currentUrlText = it }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            url?.let {
                                currentUrlText = it
                                onUrlChanged(it)
                            }
                            // Persist cookies across app sessions
                            CookieManager.getInstance().flush()
                            injectVideoListenerScript(view)
                        }
                    }

                    webChromeClient = WebChromeClient()
                    loadUrl(initialUrl)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun injectVideoListenerScript(view: WebView?) {
    val js = """
        (function() {
            var videos = document.getElementsByTagName('video');
            for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                if (!v._syncHooked) {
                    v._syncHooked = true;
                    v.addEventListener('play', function() {
                        if (window.AndroidSyncParty) {
                            window.AndroidSyncParty.onMediaAction('PLAY', v.currentTime);
                        }
                    });
                    v.addEventListener('pause', function() {
                        if (window.AndroidSyncParty) {
                            window.AndroidSyncParty.onMediaAction('PAUSE', v.currentTime);
                        }
                    });
                    v.addEventListener('seeked', function() {
                        if (window.AndroidSyncParty) {
                            window.AndroidSyncParty.onMediaAction('SEEK', v.currentTime);
                        }
                    });
                }
            }
        })();
    """.trimIndent()
    view?.evaluateJavascript(js, null)
}
