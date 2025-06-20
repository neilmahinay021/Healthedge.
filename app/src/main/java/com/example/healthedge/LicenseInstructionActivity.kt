package com.example.healthedge

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LicenseInstructionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_instruction)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
} 