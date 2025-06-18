package com.example.healthedge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.healthedge.api.ApiClient

class FeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        val feedbackInput = findViewById<EditText>(R.id.feedbackInput)
        val submitButton = findViewById<Button>(R.id.submitFeedbackButton)

        submitButton.setOnClickListener {
            val feedback = feedbackInput.text.toString().trim()
            if (feedback.isNotEmpty()) {
                submitFeedback(9, feedback) // Example: userId = 9
            } else {
                Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitFeedback(userId: Int, feedback: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.addFeedback(mapOf("user_id" to userId, "feedback" to feedback))
                if (response.isSuccessful && response.body()?.get("success") == true) {
                    Toast.makeText(this@FeedbackActivity, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FeedbackActivity, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FeedbackActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 