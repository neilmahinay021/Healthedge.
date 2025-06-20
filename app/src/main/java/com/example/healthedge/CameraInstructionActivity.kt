package com.example.healthedge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CameraInstructionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_instruction)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }
} 