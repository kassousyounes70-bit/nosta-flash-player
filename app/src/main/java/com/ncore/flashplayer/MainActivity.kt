package com.ncore.flashplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.ncore.flashplayer.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PICK_FILE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun setupUI() {
        // زر استيراد ملف SWF أو ZIP
        binding.btnImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/x-shockwave-flash",
                    "application/zip",
                    "application/octet-stream"
                ))
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "اختر ملف SWF أو ZIP"), PICK_FILE_REQUEST)
        }
    }

    // استقبال Intent من التطبيق الرئيسي (تذكرة + رابط اللعبة)
    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            "com.ncore.nostagames.LAUNCH_GAME" -> {
                val ticket  = intent.getStringExtra("ticket")  ?: return
                val gameUrl = intent.getStringExtra("game_url") ?: return
                val gameName = intent.getStringExtra("game_name") ?: "لعبة"

                // التحقق من التذكرة
                if (TicketValidator.isValid(ticket)) {
                    launchGame(gameUrl, gameName, isUrl = true)
                } else {
                    Toast.makeText(this, "❌ تذكرة غير صالحة", Toast.LENGTH_SHORT).show()
                }
            }
            Intent.ACTION_VIEW -> {
                // فتح ملف SWF مباشرة
                intent.data?.let { uri ->
                    val name = getFileName(uri) ?: "game.swf"
                    val localFile = copyUriToCache(uri, name)
                    if (localFile != null) {
                        launchGame(localFile.absolutePath, name, isUrl = false)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val name = getFileName(uri) ?: "file"
                showLoading(true)

                Thread {
                    val result = when {
                        name.endsWith(".zip", ignoreCase = true) -> extractZipAndGetSwf(uri)
                        name.endsWith(".swf", ignoreCase = true) -> copyUriToCache(uri, name)
                        else -> null
                    }

                    runOnUiThread {
                        showLoading(false)
                        if (result != null) {
                            launchGame(result.absolutePath, name.removeSuffix(".swf"), isUrl = false)
                        } else {
                            Toast.makeText(this, "❌ لم يتم العثور على ملف SWF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }
    }

    // فك ضغط ZIP والبحث عن SWF رئيسي
    private fun extractZipAndGetSwf(uri: Uri): File? {
        val extractDir = File(cacheDir, "extracted_${System.currentTimeMillis()}")
        extractDir.mkdirs()
        var mainSwf: File? = null

        contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(extractDir, entry.name.replace("/", "_"))
                        FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        // نختار أول SWF أو الأكبر حجماً
                        if (entry.name.endsWith(".swf", ignoreCase = true)) {
                            if (mainSwf == null || outFile.length() > mainSwf!!.length()) {
                                mainSwf = outFile
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return mainSwf
    }

    private fun copyUriToCache(uri: Uri, name: String): File? {
        return try {
            val file = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { out -> input.copyTo(out) }
            }
            file
        } catch (e: Exception) { null }
    }

    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(idx)
        }
    }

    private fun launchGame(path: String, name: String, isUrl: Boolean) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("game_path", path)
            putExtra("game_name", name)
            putExtra("is_url", isUrl)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnImport.isEnabled = !show
    }
}
