package com.mdconvert.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toolbar: MaterialToolbar

    private val extensions = listOf(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        AutolinkExtension.create(),
        TaskListItemsExtension.create(),
    )

    private val parser: Parser = Parser.builder()
        .extensions(extensions)
        .build()

    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .extensions(extensions)
        .build()

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { loadMarkdownFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = false

        val fab: FloatingActionButton = findViewById(R.id.fabOpen)
        fab.setOnClickListener { openFilePicker() }

        // Handle incoming intent or show welcome
        if (!handleIncomingIntent(intent)) {
            showWelcome()
        }
    }

    private fun handleIncomingIntent(intent: Intent?): Boolean {
        if (intent == null) return false

        // VIEW intent (opened from file manager)
        intent.data?.let { uri ->
            loadMarkdownFile(uri)
            return true
        }

        // SEND intent (share menu)
        if (intent.action == Intent.ACTION_SEND) {
            // Try stream URI first (file shared via share menu)
            val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (streamUri != null) {
                loadMarkdownFile(streamUri)
                return true
            }

            // Fall back to plain text content
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                toolbar.subtitle = "Shared text"
                val html = convertMarkdownToHtml(sharedText)
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                return true
            }
        }

        return false
    }

    private fun openFilePicker() {
        openFileLauncher.launch(arrayOf("text/*", "text/markdown", "*/*"))
    }

    private fun loadMarkdownFile(uri: Uri) {
        try {
            val markdown = contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            } ?: run {
                Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = uri.lastPathSegment ?: "Markdown"
            toolbar.subtitle = fileName

            val html = convertMarkdownToHtml(markdown)
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        val document = parser.parse(markdown)
        val body = renderer.render(document)

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    line-height: 1.6;
                    padding: 16px;
                    color: #1a1a1a;
                    max-width: 100%;
                    word-wrap: break-word;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 1.2em;
                    margin-bottom: 0.4em;
                    color: #1a237e;
                }
                h1 { border-bottom: 2px solid #e0e0e0; padding-bottom: 0.3em; }
                h2 { border-bottom: 1px solid #e0e0e0; padding-bottom: 0.2em; }
                code {
                    background: #f5f5f5;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-size: 0.9em;
                }
                pre {
                    background: #f5f5f5;
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                }
                pre code {
                    padding: 0;
                    background: none;
                }
                blockquote {
                    border-left: 4px solid #1a237e;
                    margin-left: 0;
                    padding: 8px 16px;
                    color: #555;
                    background: #f9f9f9;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 1em 0;
                }
                th, td {
                    border: 1px solid #ddd;
                    padding: 8px 12px;
                    text-align: left;
                }
                th {
                    background: #f0f0f0;
                    font-weight: 600;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                a {
                    color: #1565c0;
                }
                hr {
                    border: none;
                    border-top: 1px solid #e0e0e0;
                    margin: 1.5em 0;
                }
                ul, ol {
                    padding-left: 1.5em;
                }
                li {
                    margin: 0.25em 0;
                }
                input[type="checkbox"] {
                    margin-right: 0.5em;
                }
            </style>
        </head>
        <body>
            $body
        </body>
        </html>
        """.trimIndent()
    }

    private fun showWelcome() {
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

        val html = convertMarkdownToHtml(welcomeMd)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
