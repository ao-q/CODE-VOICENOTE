package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Voice Notes", appName)
  }

  @Test
  fun `storage preferences resolves to real documents directory by default`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.storage.StoragePreferences(context)
    val dir = prefs.getEffectiveBaseDirectory()
    org.junit.Assert.assertNotNull(dir)
    org.junit.Assert.assertTrue(dir.path.contains("Documents") || dir.path.contains("VoiceNotes"))
    org.junit.Assert.assertTrue(dir.exists())
  }
}
