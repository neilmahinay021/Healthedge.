package com.example.healthedge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.healthedge.models.Diagnosis
import com.example.healthedge.utils.PdfGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DiagnosisHistoryAdapter(
    private val onDownloadClick: (Diagnosis) -> Unit
) : RecyclerView.Adapter<DiagnosisHistoryAdapter.DiagnosisViewHolder>() {
    private var diagnoses: List<Diagnosis> = emptyList()

    fun submitList(list: List<Diagnosis>) {
        diagnoses = list.sortedByDescending { it.createdAt }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagnosisViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnosis, parent, false)
        return DiagnosisViewHolder(view, onDownloadClick)
    }

    override fun onBindViewHolder(holder: DiagnosisViewHolder, position: Int) {
        holder.bind(diagnoses[position])
    }

    override fun getItemCount(): Int = diagnoses.size

    class DiagnosisViewHolder(
        itemView: View,
        private val onDownloadClick: (Diagnosis) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.diagnosisDateText)
        private val diagnosisText: TextView = itemView.findViewById(R.id.diagnosisText)
        private val medicineText: TextView = itemView.findViewById(R.id.medicineText)
        private val adviceText: TextView = itemView.findViewById(R.id.adviceText)
        private val nextVisitText: TextView = itemView.findViewById(R.id.nextVisitText)
        private val downloadButton: ImageButton = itemView.findViewById(R.id.downloadButton)

        fun bind(diagnosis: Diagnosis) {
            // Format and display diagnosis date
            diagnosis.createdAt?.let { dateStr ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
                    val date = inputFormat.parse(dateStr)
                    dateText.text = outputFormat.format(date)
                } catch (e: Exception) {
                    dateText.text = dateStr
                }
            } ?: run {
                dateText.text = "Date not available"
            }

            diagnosisText.text = "Diagnosis: ${diagnosis.diagnosis ?: "Not specified"}"
            medicineText.text = "Medicine: ${diagnosis.medicineName ?: "Not specified"}"
            adviceText.text = "Advice: ${diagnosis.adviceGiven ?: "Not specified"}"
            
            // Format next visit date
            nextVisitText.text = "Next Visit: ${diagnosis.nextVisit?.let { dateStr ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    val date = inputFormat.parse(dateStr)
                    outputFormat.format(date)
                } catch (e: Exception) {
                    dateStr
                }
            } ?: "Not scheduled"}"

            downloadButton.setOnClickListener {
                onDownloadClick(diagnosis)
            }
        }
    }
} 