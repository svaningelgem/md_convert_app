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
    private lateinit var fabToggle: FloatingActionButton
    private lateinit var fabCopy: FloatingActionButton
    private lateinit var prefs: SharedPreferences

    internal val converter = MarkdownConverter()

    internal var currentMarkdown: String? = null
    internal var currentHtml: String? = null
    internal var currentSubtitle: String? = null
    internal var currentSourceUri: String? = null
    internal var showingRawMarkdown = false
    internal var isWelcomeContent = false

    internal val autoCopy: Boolean
        get() = prefs.getBoolean(PREF_AUTO_COPY, false)

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

        fabToggle = findViewById(R.id.fabToggle)
        fabToggle.setOnClickListener { toggleView() }

        fabCopy = findViewById(R.id.fabCopy)
        fabCopy.setOnClickListener { copyHtmlToClipboard() }

        if (!handleIncomingIntent(intent)) {
            restoreOrShowWelcome()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_auto_copy).isChecked = autoCopy
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_auto_copy -> {
                item.isChecked = !item.isChecked
                prefs.edit().putBoolean(PREF_AUTO_COPY, item.isChecked).apply()
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
                onConversionReady("Shared text", sharedText, null)
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

        onConversionReady("Pasted content", text, null)
    }

    internal fun openFilePicker() {
        openFileLauncher.launch(arrayOf("text/*", "text/markdown", "*/*"))
    }

    internal fun loadMarkdownFile(uri: Uri) {
        try {
            // Take persistable permission so we can reload on next launch
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Not all URIs support persistable permissions
            }

            val markdown = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            } ?: run {
                Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = uri.lastPathSegment ?: "Markdown"
            onConversionReady(fileName, markdown, uri.toString())
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    internal fun onConversionReady(subtitle: String, markdown: String, sourceUri: String?) {
        val html = converter.convertMarkdownToHtml(markdown)
        currentMarkdown = markdown
        currentHtml = html
        currentSubtitle = subtitle
        currentSourceUri = sourceUri
        showingRawMarkdown = false
        isWelcomeContent = false

        persistState(subtitle, markdown, sourceUri)

        if (autoCopy) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newHtmlText("Converted HTML", html, html))
            Toast.makeText(this, "HTML copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        displayHtml(subtitle, html)
    }

    internal fun toggleView() {
        val markdown = currentMarkdown ?: return
        val subtitle = currentSubtitle ?: return

        showingRawMarkdown = !showingRawMarkdown

        if (showingRawMarkdown) {
            displayRawMarkdown(subtitle, markdown)
        } else {
            val html = currentHtml ?: converter.convertMarkdownToHtml(markdown)
            displayHtml(subtitle, html)
        }
    }

    internal fun copyHtmlToClipboard() {
        val html = currentHtml
        if (html == null) {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newHtmlText("Converted HTML", html, html))
        Toast.makeText(this, "HTML copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    internal fun displayHtml(subtitle: String, html: String) {
        toolbar.subtitle = subtitle
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        fabToggle.setImageResource(R.drawable.ic_visibility)
    }

    internal fun displayRawMarkdown(subtitle: String, markdown: String) {
        toolbar.subtitle = "$subtitle (raw)"
        val escaped = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val rawHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Courier New', monospace;
                        font-size: 14px;
                        line-height: 1.5;
                        padding: 16px;
                        color: #1a1a1a;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                    }
                </style>
            </head>
            <body>$escaped</body>
            </html>
        """.trimIndent()
        webView.loadDataWithBaseURL(null, rawHtml, "text/html", "UTF-8", null)
        fabToggle.setImageResource(R.drawable.ic_code)
    }

    internal fun restoreOrShowWelcome() {
        val savedUri = prefs.getString(PREF_LAST_URI, null)
        val savedMarkdown = prefs.getString(PREF_LAST_MARKDOWN, null)
        val savedSubtitle = prefs.getString(PREF_LAST_SUBTITLE, null)

        if (savedMarkdown != null && savedSubtitle != null) {
            // Had a previous file — try to reload from source
            if (savedUri != null) {
                try {
                    val uri = Uri.parse(savedUri)
                    val freshMarkdown = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        it.readText()
                    }
                    if (freshMarkdown != null) {
                        onConversionReady(savedSubtitle, freshMarkdown, savedUri)
                        return
                    }
                } catch (_: Exception) {
                    // File was removed or permission lost — fall through
                }
                // File is gone — clear persisted state and show nothing
                clearPersistedState()
                return
            }
            // Had pasted/shared content (no URI) — restore from saved markdown
            onConversionReady(savedSubtitle, savedMarkdown, null)
            return
        }

        // First launch — show welcome
        showWelcome()
    }

    internal fun showWelcome() {
        val markdown = assets.open("welcome.md").bufferedReader().use { it.readText() }
        val html = converter.convertMarkdownToHtml(markdown)
        currentMarkdown = markdown
        currentHtml = html
        currentSubtitle = "Welcome"
        currentSourceUri = null
        showingRawMarkdown = false
        isWelcomeContent = true

        toolbar.subtitle = "Welcome"
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        fabToggle.setImageResource(R.drawable.ic_visibility)
    }

    private fun persistState(subtitle: String, markdown: String, sourceUri: String?) {
        prefs.edit()
            .putString(PREF_LAST_SUBTITLE, subtitle)
            .putString(PREF_LAST_MARKDOWN, markdown)
            .putString(PREF_LAST_URI, sourceUri)
            .apply()
    }

    internal fun clearPersistedState() {
        prefs.edit()
            .remove(PREF_LAST_SUBTITLE)
            .remove(PREF_LAST_MARKDOWN)
            .remove(PREF_LAST_URI)
            .apply()
    }

    companion object {
        internal const val PREFS_NAME = "md_convert_prefs"
        internal const val PREF_AUTO_COPY = "auto_copy_html"
        internal const val PREF_LAST_URI = "last_source_uri"
        internal const val PREF_LAST_MARKDOWN = "last_markdown"
        internal const val PREF_LAST_SUBTITLE = "last_subtitle"
    }
}
