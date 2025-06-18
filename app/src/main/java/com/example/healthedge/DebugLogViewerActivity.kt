package com.example.healthedge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout

class DebugLogViewerActivity : AppCompatActivity() {
    private lateinit var logTextView: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var clearButton: Button
    private lateinit var copyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_log_viewer)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Face Scan Logs"

        logTextView = findViewById(R.id.logTextView)
        tabLayout = findViewById(R.id.tabLayout)
        clearButton = findViewById(R.id.clearButton)
        copyButton = findViewById(R.id.copyButton)

        setupTabs()
        setupClearButton()
        setupCopyButton()
        loadInitialLog()
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Recent Scans"))
        tabLayout.addTab(tabLayout.newTab().setText("Scan History"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadLog("recent_face_scans.txt")
                    1 -> loadLog("face_scan_history.txt")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClearButton() {
        clearButton.setOnClickListener {
            val currentTab = tabLayout.selectedTabPosition
            val filename = when (currentTab) {
                0 -> "recent_face_scans.txt"
                1 -> "face_scan_history.txt"
                else -> return@setOnClickListener
            }
            clearLog(filename)
        }
    }

    private fun setupCopyButton() {
        copyButton.setOnClickListener {
            val logText = logTextView.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Face Scan Log", logText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Face scan logs copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInitialLog() {
        loadLog("recent_face_scans.txt")
    }

    private fun loadLog(filename: String) {
        val logContent = readLogFile(filename)
        logTextView.text = logContent
    }

    private fun clearLog(filename: String) {
        clearLogFile(filename)
        loadLog(filename)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 