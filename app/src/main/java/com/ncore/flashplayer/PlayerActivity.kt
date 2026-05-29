package com.ncore.flashplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.ncore.flashplayer.databinding.ActivityPlayerBinding
import java.io.File

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gamePath = intent.getStringExtra("game_path") ?: return
        val gameName = intent.getStringExtra("game_name") ?: "لعبة"
        val isUrl    = intent.getBooleanExtra("is_url", false)

        binding.tvGameName.text = gameName

        setupWebView(gamePath, isUrl, gameName)

        binding.btnClose.setOnClickListener { finish() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(gamePath: String, isUrl: Boolean, gameName: String) {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    val requestUrl = request.url.toString()
                    if (!isUrl && requestUrl == "https://appassets.androidplatform.net/game_file.swf") {
                        return try {
                            WebResourceResponse(
                                "application/x-shockwave-flash",
                                null,
                                File(gamePath).inputStream()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    return assetLoader.shouldInterceptRequest(request.url)
                }
            }

            val gameUrl = if (isUrl) gamePath else "https://appassets.androidplatform.net/game_file.swf"
            val html = buildRuffleHTML(gameUrl, gameName)
            loadDataWithBaseURL("https://appassets.androidplatform.net/assets/", html, "text/html", "UTF-8", null)
        }
    }

    private fun buildRuffleHTML(gameUrl: String, gameName: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <title>$gameName</title>
            <style>
                * { margin:0; padding:0; box-sizing:border-box; }
                html, body {
                    width:100%; height:100%;
                    background:#000;
                    overflow:hidden;
                    touch-action:none;
                }
                ruffle-player {
                    display:block;
                    width:100vw;
                    height:100vh;
                    touch-action:none;
                }
            </style>
            <script src="ruffle/ruffle.js"></script>
        </head>
        <body>
            <script>
                window.RufflePlayer = window.RufflePlayer || {};
                window.addEventListener('load', function() {
                    const ruffle = window.RufflePlayer.newest();
                    const player = ruffle.createPlayer();
                    player.style.width  = '100vw';
                    player.style.height = '100vh';
                    document.body.appendChild(player);
                    player.load({
                        url: '$gameUrl',
                        allowScriptAccess: 'always',
                        parameters: ''
                    });
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
