package com.example.healthedge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QRScannerActivity : AppCompatActivity() {
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            Toast.makeText(this, "QR Code: ${result.contents}", Toast.LENGTH_LONG).show()
            // TODO: Call your API with the QR code value
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrscanner)
        val scanButton = findViewById<Button>(R.id.scanQrButton)
        scanButton.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Scan a QR code")
            options.setBeepEnabled(true)
            options.setOrientationLocked(false)
            barcodeLauncher.launch(options)
        }
    }
} 