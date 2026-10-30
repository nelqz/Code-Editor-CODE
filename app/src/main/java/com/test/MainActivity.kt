package com.test

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_OPEN = 1
        private const val REQ_SAVE = 2
    }

    private lateinit var editor: CodeEditorView
    private lateinit var fileLabel: TextView
    private lateinit var statusLabel: TextView

    private var uri: Uri? = null
    private var fileName: String = "untitled.txt"
    private var dirty = false
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editor = findViewById(R.id.editor)
        fileLabel = findViewById(R.id.file_name)
        statusLabel = findViewById(R.id.status)

        findViewById<TextView>(R.id.btnNew).setOnClickListener { confirmDiscard { newFile() } }
        findViewById<TextView>(R.id.btnOpen).setOnClickListener { confirmDiscard { openFile() } }
        findViewById<TextView>(R.id.btnSave).setOnClickListener { save() }

        editor.onCaretMoved = { line, column -> statusLabel.text = "$line:$column" }

        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (loading || dirty) return
                dirty = true
                updateTitle()
            }
        })

        updateTitle()
        editor.requestFocus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("uri", uri)
        outState.putString("name", fileName)
        outState.putBoolean("dirty", dirty)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        uri = savedInstanceState.getParcelable("uri")
        fileName = savedInstanceState.getString("name") ?: "untitled.txt"
        dirty = savedInstanceState.getBoolean("dirty")
        updateTitle()
    }

    override fun onBackPressed() {
        if (!dirty) {
            super.onBackPressed()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Discard changes?")
            .setMessage("Your edits to $fileName will be lost.")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep editing", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        val target = data?.data ?: return

        when (requestCode) {
            REQ_OPEN -> {
                try {
                    val flags = data.flags and (
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    contentResolver.takePersistableUriPermission(target, flags)
                } catch (_: Exception) {
                }
                loadDocument(target)
            }

            REQ_SAVE -> {
                fileName = queryName(target) ?: fileName
                writeDocument(target)
            }
        }
    }

    private fun newFile() {
        loading = true
        editor.setText("")
        loading = false
        uri = null
        fileName = "untitled.txt"
        dirty = false
        editor.setSelection(0)
        updateTitle()
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQ_OPEN)
    }

    private fun loadDocument(target: Uri) {
        try {
            val content = contentResolver.openInputStream(target)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            loading = true
            editor.setText(content)
            editor.setSelection(0)
            loading = false
            uri = target
            fileName = queryName(target) ?: target.lastPathSegment ?: "untitled.txt"
            dirty = false
            updateTitle()
        } catch (e: Exception) {
            toast("Could not open file")
        }
    }

    private fun save() {
        val current = uri
        if (current == null) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            startActivityForResult(intent, REQ_SAVE)
        } else {
            writeDocument(current)
        }
    }

    private fun writeDocument(target: Uri) {
        try {
            contentResolver.openOutputStream(target, "wt")
                ?.bufferedWriter(Charsets.UTF_8)
                ?.use { it.write(editor.text?.toString() ?: "") }
            uri = target
            dirty = false
            updateTitle()
            toast("Saved $fileName")
        } catch (e: Exception) {
            toast("Could not save file")
        }
    }

    private fun queryName(target: Uri): String? {
        return contentResolver.query(
            target,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun updateTitle() {
        fileLabel.text = if (dirty) "$fileName *" else fileName
    }

    private fun confirmDiscard(action: () -> Unit) {
        if (!dirty) {
            action()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Discard changes?")
            .setMessage("Your edits to $fileName will be lost.")
            .setPositiveButton("Discard") { _, _ -> action() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
