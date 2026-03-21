package com.mdconvert.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // -- onCreate & default launch --

    @Test
    fun `onCreate initializes all views`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<WebView>(R.id.webView))
                assertNotNull(activity.findViewById<MaterialToolbar>(R.id.toolbar))
                assertNotNull(activity.findViewById<FloatingActionButton>(R.id.fabOpen))
                assertNotNull(activity.findViewById<FloatingActionButton>(R.id.fabPaste))
                assertNotNull(activity.findViewById<FloatingActionButton>(R.id.fabShow))
            }
        }
    }

    @Test
    fun `default launch shows welcome in WebView`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(R.id.webView)
                val shadowWebView = shadowOf(webView)
                val lastData = shadowWebView.lastLoadDataWithBaseURL
                assertNotNull(lastData)
                assertTrue(lastData.data.contains("MD Convert"))
                assertTrue(lastData.data.contains("Supported Features"))
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
                val intent = Intent()
                assertFalse(activity.handleIncomingIntent(intent))
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
                assertNotNull(activity.lastHtml)
                assertEquals("Shared text", activity.lastSubtitle)
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
                val intent = Intent(Intent.ACTION_SEND)
                assertFalse(activity.handleIncomingIntent(intent))
            }
        }
    }

    @Test
    fun `handleIncomingIntent handles intent with data URI`() {
        val testUri = Uri.parse("content://com.example/test.md")
        val intent = Intent(Intent.ACTION_VIEW, testUri)
        // This will trigger loadMarkdownFile which will fail to read, but the branch is covered
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val result = activity.handleIncomingIntent(intent)
                assertTrue(result)
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
                val latestToast = ShadowToast.getTextOfLatestToast()
                assertEquals("Clipboard is empty", latestToast)
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
                val latestToast = ShadowToast.getTextOfLatestToast()
                assertEquals("Clipboard has no text", latestToast)
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
                assertNotNull(activity.lastHtml)
                assertTrue(activity.lastHtml!!.contains("<h1>Pasted</h1>"))
                assertEquals("Pasted content", activity.lastSubtitle)
            }
        }
    }

    // -- showLastConversion --

    @Test
    fun `showLastConversion shows toast when no conversion available`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.lastHtml = null
                activity.lastSubtitle = null
                activity.showLastConversion()
                assertEquals("No conversion available", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    @Test
    fun `showLastConversion displays stored HTML`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val testHtml = "<html><body>Test</body></html>"
                activity.lastHtml = testHtml
                activity.lastSubtitle = "test.md"
                activity.showLastConversion()

                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                assertEquals("test.md", toolbar.subtitle)
            }
        }
    }

    @Test
    fun `showLastConversion does nothing when only html is null`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.lastHtml = null
                activity.lastSubtitle = "test.md"
                activity.showLastConversion()
                assertEquals("No conversion available", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    @Test
    fun `showLastConversion does nothing when only subtitle is null`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.lastHtml = "<p>html</p>"
                activity.lastSubtitle = null
                activity.showLastConversion()
                assertEquals("No conversion available", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    // -- displayHtml --

    @Test
    fun `displayHtml sets toolbar subtitle and shows fabShow`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.displayHtml("my-file.md", "<p>Hello</p>")
                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                val fabShow = activity.findViewById<FloatingActionButton>(R.id.fabShow)
                assertEquals("my-file.md", toolbar.subtitle)
                assertEquals(View.VISIBLE, fabShow.visibility)
            }
        }
    }

    // -- onConversionReady --

    @Test
    fun `onConversionReady stores html and subtitle`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.onConversionReady("test.md", "<p>Test</p>")
                assertEquals("<p>Test</p>", activity.lastHtml)
                assertEquals("test.md", activity.lastSubtitle)
            }
        }
    }

    @Test
    fun `onConversionReady with autoCopy copies to clipboard`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val prefs = activity.getSharedPreferences("md_convert_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("auto_copy_html", true).commit()

                activity.onConversionReady("test.md", "<p>Copied</p>")

                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipText = clipboard.primaryClip?.getItemAt(0)?.htmlText
                assertEquals("<p>Copied</p>", clipText)
                assertEquals("HTML copied to clipboard", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    @Test
    fun `onConversionReady with noAutoShow shows eye button toast`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val prefs = activity.getSharedPreferences("md_convert_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("no_auto_show", true).commit()

                activity.onConversionReady("test.md", "<p>Hidden</p>")

                val fabShow = activity.findViewById<FloatingActionButton>(R.id.fabShow)
                assertEquals(View.VISIBLE, fabShow.visibility)
                val toast = ShadowToast.getTextOfLatestToast()
                assertTrue(toast.contains("tap the eye button"))
            }
        }
    }

    @Test
    fun `onConversionReady without settings displays html directly`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val prefs = activity.getSharedPreferences("md_convert_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("auto_copy_html", false).putBoolean("no_auto_show", false).commit()

                activity.onConversionReady("file.md", "<p>Direct</p>")

                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                assertEquals("file.md", toolbar.subtitle)
            }
        }
    }

    @Test
    fun `onConversionReady with both autoCopy and noAutoShow`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val prefs = activity.getSharedPreferences("md_convert_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("auto_copy_html", true)
                    .putBoolean("no_auto_show", true)
                    .commit()

                activity.onConversionReady("test.md", "<p>Both</p>")

                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipText = clipboard.primaryClip?.getItemAt(0)?.htmlText
                assertEquals("<p>Both</p>", clipText)

                val fabShow = activity.findViewById<FloatingActionButton>(R.id.fabShow)
                assertEquals(View.VISIBLE, fabShow.visibility)
            }
        }
    }

    // -- Menu --

    @Test
    fun `onCreateOptionsMenu returns true`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.onCreateOptionsMenu(activity.findViewById<MaterialToolbar>(R.id.toolbar).menu))
            }
        }
    }

    // -- Settings preferences --

    @Test
    fun `autoCopy defaults to false`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.autoCopy)
            }
        }
    }

    @Test
    fun `noAutoShow defaults to false`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.noAutoShow)
            }
        }
    }

    // -- loadMarkdownFile --

    @Test
    fun `loadMarkdownFile shows error toast for unreadable URI`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val uri = Uri.parse("content://invalid/path")
                activity.loadMarkdownFile(uri)
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
                assertNotNull(activity.lastHtml)
                assertTrue(activity.lastHtml!!.contains("<h1>From FAB</h1>"))
            }
        }
    }

    @Test
    fun `fabShow click triggers showLastConversion`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.lastHtml = null
                activity.lastSubtitle = null
                activity.findViewById<FloatingActionButton>(R.id.fabShow).performClick()
                assertEquals("No conversion available", ShadowToast.getTextOfLatestToast())
            }
        }
    }

    // -- showWelcome --

    @Test
    fun `showWelcome loads welcome markdown`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.showWelcome()
                val webView = activity.findViewById<WebView>(R.id.webView)
                val shadowWebView = shadowOf(webView)
                val lastData = shadowWebView.lastLoadDataWithBaseURL
                assertNotNull(lastData)
                assertTrue(lastData.data.contains("MD Convert"))
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
