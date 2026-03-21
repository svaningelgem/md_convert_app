package com.mdconvert.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        // Clear prefs before each test
        context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    // -- onCreate & default launch --

    @Test
    fun `onCreate initializes all views`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<WebView>(R.id.webView))
                assertNotNull(activity.findViewById<MaterialToolbar>(R.id.toolbar))
                assertNotNull(activity.findViewById<FloatingActionButton>(R.id.fabOpen))
                assertNotNull(activity.findViewById<FloatingActionButton>(R.id.fabPaste))
                assertNotNull(activity.findViewById<FloatingActionButton>(R.id.fabToggle))
                assertNotNull(activity.findViewById<FloatingActionButton>(R.id.fabCopy))
            }
        }
    }

    @Test
    fun `first launch shows welcome content`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isWelcomeContent)
                assertNotNull(activity.currentMarkdown)
                assertNotNull(activity.currentHtml)
                assertTrue(activity.currentMarkdown!!.contains("Welcome to MD Convert"))
                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                assertEquals("Welcome", toolbar.subtitle)
            }
        }
    }

    @Test
    fun `welcome content loads from assets`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webView)
                val shadowWebView = shadowOf(webView)
                val lastData = shadowWebView.lastLoadDataWithBaseURL
                assertNotNull(lastData)
                assertTrue(lastData.data.contains("Welcome to MD Convert"))
            }
        }
    }

    // -- handleIncomingIntent --

    @Test
    fun `handleIncomingIntent returns false for null intent`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.handleIncomingIntent(null))
            }
        }
    }

    @Test
    fun `handleIncomingIntent returns false for empty intent`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.handleIncomingIntent(Intent()))
            }
        }
    }

    @Test
    fun `handleIncomingIntent handles ACTION_SEND with EXTRA_TEXT`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, "# Hello")
                }
                assertTrue(activity.handleIncomingIntent(intent))
                assertNotNull(activity.currentHtml)
                assertEquals("Shared text", activity.currentSubtitle)
                assertNull(activity.currentSourceUri)
                assertFalse(activity.isWelcomeContent)
            }
        }
    }

    @Test
    fun `handleIncomingIntent returns false for ACTION_SEND with blank text`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, "   ")
                }
                assertFalse(activity.handleIncomingIntent(intent))
            }
        }
    }

    @Test
    fun `handleIncomingIntent returns false for ACTION_SEND with no extras`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.handleIncomingIntent(Intent(Intent.ACTION_SEND)))
            }
        }
    }

    @Test
    fun `handleIncomingIntent handles intent with data URI`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://com.example/test.md"))
                assertTrue(activity.handleIncomingIntent(intent))
            }
        }
    }

    // -- pasteFromClipboard --

    @Test
    fun `pasteFromClipboard shows toast when clipboard is empty`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.clearPrimaryClip()
                activity.pasteFromClipboard()
                assertEquals("Clipboard is empty", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    @Test
    fun `pasteFromClipboard shows toast when clipboard has blank text`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("label", "   "))
                activity.pasteFromClipboard()
                assertEquals("Clipboard has no text", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    @Test
    fun `pasteFromClipboard converts valid clipboard text`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("label", "# Pasted"))
                activity.pasteFromClipboard()
                assertNotNull(activity.currentHtml)
                assertTrue(activity.currentHtml!!.contains("<h1>Pasted</h1>"))
                assertEquals("Pasted content", activity.currentSubtitle)
                assertNull(activity.currentSourceUri)
            }
        }
    }

    // -- toggleView --

    @Test
    fun `toggleView switches to raw markdown`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("test.md", "# Hello", null)
                assertFalse(activity.showingRawMarkdown)

                activity.toggleView()
                assertTrue(activity.showingRawMarkdown)

                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                assertEquals("test.md (raw)", toolbar.subtitle)
            }
        }
    }

    @Test
    fun `toggleView switches back to HTML`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("test.md", "# Hello", null)
                activity.toggleView() // to raw
                activity.toggleView() // back to HTML
                assertFalse(activity.showingRawMarkdown)

                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                assertEquals("test.md", toolbar.subtitle)
            }
        }
    }

    @Test
    fun `toggleView does nothing when no content`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.currentMarkdown = null
                activity.currentSubtitle = null
                activity.toggleView()
                assertFalse(activity.showingRawMarkdown)
            }
        }
    }

    @Test
    fun `toggleView works on welcome content`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Welcome is loaded by default
                assertTrue(activity.isWelcomeContent)
                activity.toggleView()
                assertTrue(activity.showingRawMarkdown)
            }
        }
    }

    // -- copyHtmlToClipboard --

    @Test
    fun `copyHtmlToClipboard copies HTML to clipboard`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("test.md", "# Hello", null)
                activity.copyHtmlToClipboard()

                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipHtml = clipboard.primaryClip?.getItemAt(0)?.htmlText
                assertNotNull(clipHtml)
                assertTrue(clipHtml!!.contains("<h1>Hello</h1>"))
                assertEquals("HTML copied to clipboard", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    @Test
    fun `copyHtmlToClipboard shows toast when nothing to copy`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.currentHtml = null
                activity.copyHtmlToClipboard()
                assertEquals("Nothing to copy", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    // -- displayHtml --

    @Test
    fun `displayHtml sets toolbar subtitle`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.displayHtml("my-file.md", "<p>Hello</p>")
                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                assertEquals("my-file.md", toolbar.subtitle)
            }
        }
    }

    // -- displayRawMarkdown --

    @Test
    fun `displayRawMarkdown escapes HTML characters`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.displayRawMarkdown("test.md", "Use <div> & \"quotes\"")
                val webView = activity.findViewById<WebView>(R.id.webView)
                val data = shadowOf(webView).lastLoadDataWithBaseURL.data
                assertTrue(data.contains("&lt;div&gt;"))
                assertTrue(data.contains("&amp;"))
            }
        }
    }

    @Test
    fun `displayRawMarkdown uses monospace font`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.displayRawMarkdown("test.md", "# Hello")
                val webView = activity.findViewById<WebView>(R.id.webView)
                val data = shadowOf(webView).lastLoadDataWithBaseURL.data
                assertTrue(data.contains("monospace"))
                assertTrue(data.contains("pre-wrap"))
            }
        }
    }

    // -- onConversionReady --

    @Test
    fun `onConversionReady stores markdown, html, and subtitle`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("test.md", "# Test", null)
                assertNotNull(activity.currentMarkdown)
                assertNotNull(activity.currentHtml)
                assertEquals("test.md", activity.currentSubtitle)
                assertFalse(activity.showingRawMarkdown)
                assertFalse(activity.isWelcomeContent)
            }
        }
    }

    @Test
    fun `onConversionReady with autoCopy copies to clipboard`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(MainActivity.PREF_AUTO_COPY, true).commit()

                activity.onConversionReady("test.md", "# Copied", null)

                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipHtml = clipboard.primaryClip?.getItemAt(0)?.htmlText
                assertNotNull(clipHtml)
                assertTrue(clipHtml!!.contains("<h1>Copied</h1>"))
                assertEquals("HTML copied to clipboard", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    @Test
    fun `onConversionReady persists state`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("file.md", "# Saved", "content://some/uri")

                val prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                assertEquals("file.md", prefs.getString(MainActivity.PREF_LAST_SUBTITLE, null))
                assertEquals("# Saved", prefs.getString(MainActivity.PREF_LAST_MARKDOWN, null))
                assertEquals("content://some/uri", prefs.getString(MainActivity.PREF_LAST_URI, null))
            }
        }
    }

    @Test
    fun `onConversionReady with null sourceUri persists without uri`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("Pasted", "# Paste", null)

                val prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                assertEquals("Pasted", prefs.getString(MainActivity.PREF_LAST_SUBTITLE, null))
                assertNull(prefs.getString(MainActivity.PREF_LAST_URI, null))
            }
        }
    }

    // -- restoreOrShowWelcome --

    @Test
    fun `restoreOrShowWelcome shows welcome on first launch`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isWelcomeContent)
                assertEquals("Welcome", activity.currentSubtitle)
            }
        }
    }

    @Test
    fun `restoreOrShowWelcome restores pasted content without URI`() {
        // Pre-populate prefs with pasted content (no URI)
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(MainActivity.PREF_LAST_SUBTITLE, "Pasted content")
            .putString(MainActivity.PREF_LAST_MARKDOWN, "# Restored")
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isWelcomeContent)
                assertEquals("Pasted content", activity.currentSubtitle)
                assertTrue(activity.currentHtml!!.contains("<h1>Restored</h1>"))
            }
        }
    }

    @Test
    fun `restoreOrShowWelcome clears state when file URI is unreadable`() {
        // Pre-populate prefs with a file URI that can't be read
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(MainActivity.PREF_LAST_SUBTITLE, "old.md")
            .putString(MainActivity.PREF_LAST_MARKDOWN, "# Old")
            .putString(MainActivity.PREF_LAST_URI, "content://gone/file.md")
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // File is gone — should show nothing and clear state
                assertNull(activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(MainActivity.PREF_LAST_MARKDOWN, null))
            }
        }
    }

    // -- clearPersistedState --

    @Test
    fun `clearPersistedState removes all persisted keys`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(MainActivity.PREF_LAST_SUBTITLE, "test")
                    .putString(MainActivity.PREF_LAST_MARKDOWN, "# test")
                    .putString(MainActivity.PREF_LAST_URI, "content://uri")
                    .commit()

                activity.clearPersistedState()

                assertNull(prefs.getString(MainActivity.PREF_LAST_SUBTITLE, null))
                assertNull(prefs.getString(MainActivity.PREF_LAST_MARKDOWN, null))
                assertNull(prefs.getString(MainActivity.PREF_LAST_URI, null))
            }
        }
    }

    // -- Menu --

    @Test
    fun `onCreateOptionsMenu returns true`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.onCreateOptionsMenu(
                    activity.findViewById<MaterialToolbar>(R.id.toolbar).menu
                ))
            }
        }
    }

    @Test
    fun `autoCopy defaults to false`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.autoCopy)
            }
        }
    }

    // -- loadMarkdownFile --

    @Test
    fun `loadMarkdownFile shows error toast for unreadable URI`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.loadMarkdownFile(Uri.parse("content://invalid/path"))
                val toast = ShadowToast.getTextOfLatestToast()
                assertNotNull(toast)
            }
        }
    }

    // -- FAB click listeners --

    @Test
    fun `fabPaste click triggers pasteFromClipboard`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("label", "# From FAB"))
                activity.findViewById<FloatingActionButton>(R.id.fabPaste).performClick()
                assertNotNull(activity.currentHtml)
                assertTrue(activity.currentHtml!!.contains("<h1>From FAB</h1>"))
            }
        }
    }

    @Test
    fun `fabToggle click toggles view`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("test.md", "# Toggle", null)
                assertFalse(activity.showingRawMarkdown)

                activity.findViewById<FloatingActionButton>(R.id.fabToggle).performClick()
                assertTrue(activity.showingRawMarkdown)

                activity.findViewById<FloatingActionButton>(R.id.fabToggle).performClick()
                assertFalse(activity.showingRawMarkdown)
            }
        }
    }

    @Test
    fun `fabCopy click copies HTML`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("test.md", "**bold**", null)
                activity.findViewById<FloatingActionButton>(R.id.fabCopy).performClick()

                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipHtml = clipboard.primaryClip?.getItemAt(0)?.htmlText
                assertNotNull(clipHtml)
                assertTrue(clipHtml!!.contains("<strong>bold</strong>"))
            }
        }
    }

    // -- showWelcome --

    @Test
    fun `showWelcome sets isWelcomeContent flag`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showWelcome()
                assertTrue(activity.isWelcomeContent)
                assertEquals("Welcome", activity.currentSubtitle)
                assertNotNull(activity.currentMarkdown)
                assertNotNull(activity.currentHtml)
                assertNull(activity.currentSourceUri)
                assertFalse(activity.showingRawMarkdown)
            }
        }
    }

    // -- Converter integration --

    @Test
    fun `converter is initialized and functional`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val html = activity.converter.convertMarkdownToHtml("**test**")
                assertTrue(html.contains("<strong>test</strong>"))
            }
        }
    }
}
