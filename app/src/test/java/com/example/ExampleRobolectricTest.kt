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
        assertEquals("नारद एक्सप्लोरर", appName)
    }

    @Test
    fun `check manage external storage permission is declared`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions ?: emptyArray()
        val hasManageStorage = permissions.contains("android.permission.MANAGE_EXTERNAL_STORAGE")
        assertEquals(true, hasManageStorage)
    }

    @Test
    fun `test ScannedFile model attributes and storage classification`() {
        val file = com.example.data.ScannedFile(
            path = "drive/xyz123",
            name = "project_proposal.pdf",
            size = 1048576,
            mimeType = "application/pdf",
            category = "documents",
            storageType = "DRIVE",
            lastModified = System.currentTimeMillis(),
            aiDescription = "A Google Drive PDF File"
        )
        assertEquals("DRIVE", file.storageType)
        assertEquals("documents", file.category)
        assertEquals("project_proposal.pdf", file.name)
    }
}
