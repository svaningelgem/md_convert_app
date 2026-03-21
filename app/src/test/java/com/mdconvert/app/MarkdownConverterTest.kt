package com.mdconvert.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MarkdownConverterTest {

    private lateinit var converter: MarkdownConverter

    @Before
    fun setUp() {
        converter = MarkdownConverter()
    }

    // -- HTML template structure --

    @Test
    fun `convertMarkdownToHtml returns valid HTML document`() {
        val html = converter.convertMarkdownToHtml("Hello")
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<html>"))
        assertTrue(html.contains("</html>"))
        assertTrue(html.contains("<head>"))
        assertTrue(html.contains("<body>"))
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains("charset=\"UTF-8\""))
        assertTrue(html.contains("viewport"))
    }

    @Test
    fun `wrapInHtmlTemplate wraps body in full HTML`() {
        val html = MarkdownConverter.wrapInHtmlTemplate("<p>Test</p>")
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<p>Test</p>"))
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun `renderBodyOnly returns raw HTML without template`() {
        val body = converter.renderBodyOnly("**bold**")
        assertTrue(body.contains("<strong>bold</strong>"))
        assertFalse(body.contains("<!DOCTYPE html>"))
        assertFalse(body.contains("<style>"))
    }

    // -- Basic markdown elements --

    @Test
    fun `converts headings`() {
        val body = converter.renderBodyOnly("# H1\n## H2\n### H3\n#### H4\n##### H5\n###### H6")
        assertTrue(body.contains("<h1>H1</h1>"))
        assertTrue(body.contains("<h2>H2</h2>"))
        assertTrue(body.contains("<h3>H3</h3>"))
        assertTrue(body.contains("<h4>H4</h4>"))
        assertTrue(body.contains("<h5>H5</h5>"))
        assertTrue(body.contains("<h6>H6</h6>"))
    }

    @Test
    fun `converts bold text`() {
        val body = converter.renderBodyOnly("**bold**")
        assertTrue(body.contains("<strong>bold</strong>"))
    }

    @Test
    fun `converts italic text`() {
        val body = converter.renderBodyOnly("*italic*")
        assertTrue(body.contains("<em>italic</em>"))
    }

    @Test
    fun `converts bold italic text`() {
        val body = converter.renderBodyOnly("***bold italic***")
        assertTrue(body.contains("<strong>") && body.contains("<em>"))
    }

    @Test
    fun `converts paragraphs`() {
        val body = converter.renderBodyOnly("Paragraph one\n\nParagraph two")
        assertTrue(body.contains("<p>Paragraph one</p>"))
        assertTrue(body.contains("<p>Paragraph two</p>"))
    }

    @Test
    fun `converts line breaks`() {
        val body = converter.renderBodyOnly("Line one  \nLine two")
        assertTrue(body.contains("<br"))
    }

    // -- Links and images --

    @Test
    fun `converts links`() {
        val body = converter.renderBodyOnly("[click](https://example.com)")
        assertTrue(body.contains("<a href=\"https://example.com\">click</a>"))
    }

    @Test
    fun `converts images`() {
        val body = converter.renderBodyOnly("![alt](https://example.com/img.png)")
        assertTrue(body.contains("<img"))
        assertTrue(body.contains("src=\"https://example.com/img.png\""))
        assertTrue(body.contains("alt=\"alt\""))
    }

    // -- Lists --

    @Test
    fun `converts unordered lists`() {
        val body = converter.renderBodyOnly("- item1\n- item2\n- item3")
        assertTrue(body.contains("<ul>"))
        assertTrue(body.contains("<li>item1</li>"))
        assertTrue(body.contains("<li>item2</li>"))
        assertTrue(body.contains("<li>item3</li>"))
        assertTrue(body.contains("</ul>"))
    }

    @Test
    fun `converts ordered lists`() {
        val body = converter.renderBodyOnly("1. first\n2. second\n3. third")
        assertTrue(body.contains("<ol>"))
        assertTrue(body.contains("<li>first</li>"))
        assertTrue(body.contains("<li>second</li>"))
        assertTrue(body.contains("</ol>"))
    }

    @Test
    fun `converts nested lists`() {
        val body = converter.renderBodyOnly("- parent\n  - child\n    - grandchild")
        assertTrue(body.contains("<ul>"))
        assertTrue(body.contains("parent"))
        assertTrue(body.contains("child"))
        assertTrue(body.contains("grandchild"))
    }

    // -- Code --

    @Test
    fun `converts inline code`() {
        val body = converter.renderBodyOnly("`code`")
        assertTrue(body.contains("<code>code</code>"))
    }

    @Test
    fun `converts code blocks`() {
        val body = converter.renderBodyOnly("```\nval x = 1\n```")
        assertTrue(body.contains("<pre>"))
        assertTrue(body.contains("<code>"))
        assertTrue(body.contains("val x = 1"))
    }

    @Test
    fun `converts fenced code with language`() {
        val body = converter.renderBodyOnly("```kotlin\nfun main() {}\n```")
        assertTrue(body.contains("<pre>"))
        assertTrue(body.contains("<code"))
        assertTrue(body.contains("fun main() {}"))
    }

    // -- Blockquotes --

    @Test
    fun `converts blockquotes`() {
        val body = converter.renderBodyOnly("> This is a quote")
        assertTrue(body.contains("<blockquote>"))
        assertTrue(body.contains("This is a quote"))
        assertTrue(body.contains("</blockquote>"))
    }

    @Test
    fun `converts nested blockquotes`() {
        val body = converter.renderBodyOnly("> outer\n>> inner")
        assertTrue(body.contains("<blockquote>"))
        assertTrue(body.contains("outer"))
        assertTrue(body.contains("inner"))
    }

    // -- Horizontal rule --

    @Test
    fun `converts horizontal rules`() {
        val body = converter.renderBodyOnly("---")
        assertTrue(body.contains("<hr"))
    }

    // -- GFM Extensions --

    @Test
    fun `converts strikethrough`() {
        val body = converter.renderBodyOnly("~~deleted~~")
        assertTrue(body.contains("<del>deleted</del>"))
    }

    @Test
    fun `converts tables`() {
        val md = """
            | Header 1 | Header 2 |
            |----------|----------|
            | Cell 1   | Cell 2   |
            | Cell 3   | Cell 4   |
        """.trimIndent()
        val body = converter.renderBodyOnly(md)
        assertTrue(body.contains("<table>"))
        assertTrue(body.contains("<thead>"))
        assertTrue(body.contains("<tbody>"))
        assertTrue(body.contains("<th>Header 1</th>"))
        assertTrue(body.contains("<th>Header 2</th>"))
        assertTrue(body.contains("<td>Cell 1</td>"))
        assertTrue(body.contains("<td>Cell 4</td>"))
    }

    @Test
    fun `converts task lists`() {
        val md = "- [ ] unchecked\n- [x] checked"
        val body = converter.renderBodyOnly(md)
        assertTrue(body.contains("type=\"checkbox\""))
        assertTrue(body.contains("checked"))
    }

    @Test
    fun `converts autolinks`() {
        val body = converter.renderBodyOnly("Visit https://example.com for more")
        assertTrue(body.contains("<a href=\"https://example.com\">"))
    }

    // -- Edge cases --

    @Test
    fun `handles empty string`() {
        val html = converter.convertMarkdownToHtml("")
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<body>"))
    }

    @Test
    fun `handles whitespace only`() {
        val html = converter.convertMarkdownToHtml("   \n  \n   ")
        assertTrue(html.contains("<!DOCTYPE html>"))
    }

    @Test
    fun `handles special HTML characters`() {
        val body = converter.renderBodyOnly("5 < 10 & 10 > 5")
        assertTrue(body.contains("&lt;"))
        assertTrue(body.contains("&amp;"))
        assertTrue(body.contains("&gt;"))
    }

    @Test
    fun `handles complex mixed markdown`() {
        val md = """
            # Title

            Some **bold** and *italic* and ~~struck~~ text.

            > A blockquote with `code`

            | Col A | Col B |
            |-------|-------|
            | 1     | 2     |

            - [ ] Task one
            - [x] Task two

            ```
            code block
            ```

            ---

            [link](https://example.com)
        """.trimIndent()
        val html = converter.convertMarkdownToHtml(md)
        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<del>struck</del>"))
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("type=\"checkbox\""))
        assertTrue(html.contains("<pre>"))
        assertTrue(html.contains("<hr"))
        assertTrue(html.contains("<a href=\"https://example.com\">"))
    }

    // -- CSS styling checks --

    @Test
    fun `HTML template contains expected CSS styles`() {
        val html = converter.convertMarkdownToHtml("test")
        assertTrue(html.contains("font-family:"))
        assertTrue(html.contains("line-height: 1.6"))
        assertTrue(html.contains("color: #1a237e"))
        assertTrue(html.contains("border-collapse: collapse"))
        assertTrue(html.contains("overflow-x: auto"))
        assertTrue(html.contains("word-wrap: break-word"))
    }
}
