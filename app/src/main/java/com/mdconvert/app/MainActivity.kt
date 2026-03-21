package com.mdconvert.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fabShow: FloatingActionButton
    private lateinit var prefs: SharedPreferences

    internal var lastHtml: String? = null
    internal var lastSubtitle: String? = null

    internal val converter = MarkdownConverter()

    internal val autoCopy: Boolean
        get() = prefs.getBoolean(PREF_AUTO_COPY, false)

    internal val noAutoShow: Boolean
        get() = prefs.getBoolean(PREF_NO_AUTO_SHOW, false)

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadMarkdownFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = false

        val fabOpen: FloatingActionButton = findViewById(R.id.fabOpen)
        fabOpen.setOnClickListener { openFilePicker() }

        val fabPaste: FloatingActionButton = findViewById(R.id.fabPaste)
        fabPaste.setOnClickListener { pasteFromClipboard() }

        fabShow = findViewById(R.id.fabShow)
        fabShow.setOnClickListener { showLastConversion() }

        if (!handleIncomingIntent(intent)) {
            showWelcome()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_auto_copy).isChecked = autoCopy
        menu.findItem(R.id.action_no_auto_show).isChecked = noAutoShow
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_auto_copy -> {
                item.isChecked = !item.isChecked
                prefs.edit().putBoolean(PREF_AUTO_COPY, item.isChecked).apply()
                true
            }
            R.id.action_no_auto_show -> {
                item.isChecked = !item.isChecked
                prefs.edit().putBoolean(PREF_NO_AUTO_SHOW, item.isChecked).apply()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    internal fun handleIncomingIntent(intent: Intent?): Boolean {
        if (intent == null) return false

        intent.data?.let { uri ->
            loadMarkdownFile(uri)
            return true
        }

        if (intent.action == Intent.ACTION_SEND) {
            val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (streamUri != null) {
                loadMarkdownFile(streamUri)
                return true
            }

            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                onConversionReady("Shared text", converter.convertMarkdownToHtml(sharedText))
                return true
            }
        }

        return false
    }

    internal fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val text = clip.getItemAt(0).coerceToText(this).toString()
        if (text.isBlank()) {
            Toast.makeText(this, "Clipboard has no text", Toast.LENGTH_SHORT).show()
            return
        }

        onConversionReady("Pasted content", converter.convertMarkdownToHtml(text))
    }

    internal fun openFilePicker() {
        openFileLauncher.launch(arrayOf("text/*", "text/markdown", "*/*"))
    }

    internal fun loadMarkdownFile(uri: Uri) {
        try {
            val markdown = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            } ?: run {
                Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = uri.lastPathSegment ?: "Markdown"
            onConversionReady(fileName, converter.convertMarkdownToHtml(markdown))
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    internal fun onConversionReady(subtitle: String, html: String) {
        lastHtml = html
        lastSubtitle = subtitle

        if (autoCopy) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newHtmlText("Converted HTML", html, html))
            Toast.makeText(this, "HTML copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        if (noAutoShow) {
            fabShow.visibility = View.VISIBLE
            Toast.makeText(this, "Conversion ready \u2014 tap the eye button to view", Toast.LENGTH_SHORT).show()
        } else {
            displayHtml(subtitle, html)
        }
    }

    internal fun showLastConversion() {
        val html = lastHtml
        val subtitle = lastSubtitle
        if (html != null && subtitle != null) {
            displayHtml(subtitle, html)
        } else {
            Toast.makeText(this, "No conversion available", Toast.LENGTH_SHORT).show()
        }
    }

    internal fun displayHtml(subtitle: String, html: String) {
        toolbar.subtitle = subtitle
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        fabShow.visibility = View.VISIBLE
    }

    internal fun showWelcome() {
        val welcomeMd = """
            # MD Convert

            Welcome! Tap the **+** button to open a Markdown file.

            ## Supported Features

            - **Bold**, *italic*, ~~strikethrough~~
            - [Links](https://example.com)
            - Tables (GFM)
            - Task lists
            - Code blocks
            - Blockquotes
            - And more...
        """.trimIndent()

        val html = converter.convertMarkdownToHtml(welcomeMd)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    companion object {
        private const val PREFS_NAME = "md_convert_prefs"
        private const val PREF_AUTO_COPY = "auto_copy_html"
        private const val PREF_NO_AUTO_SHOW = "no_auto_show"
    }
}
