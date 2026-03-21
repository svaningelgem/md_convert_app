package com.mdconvert.app

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

class MarkdownConverter {

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

    fun convertMarkdownToHtml(markdown: String): String {
        val document = parser.parse(markdown)
        val body = renderer.render(document)
        return wrapInHtmlTemplate(body)
    }

    fun renderBodyOnly(markdown: String): String {
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    companion object {
        fun wrapInHtmlTemplate(body: String): String {
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
    }
}
