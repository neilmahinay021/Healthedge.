package com.example.healthedge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.healthedge.models.Diagnosis
import java.text.SimpleDateFormat
import java.util.*

class DiagnosisAdapter(private val onDownloadClick: (Diagnosis) -> Unit) : RecyclerView.Adapter<DiagnosisAdapter.DiagnosisViewHolder>() {
    private var diagnoses: List<Diagnosis> = emptyList()

    fun submitList(list: List<Diagnosis>?) {
        diagnoses = list ?: emptyList()
        notifyDataSetChanged()
    }

    fun getLatestDiagnosis(): Diagnosis? = diagnoses.firstOrNull()

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagnosisViewHolder {
        return if (viewType == 1) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_latest_diagnosis, parent, false)
            LatestDiagnosisViewHolder(view, onDownloadClick)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            DiagnosisViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: DiagnosisViewHolder, position: Int) {
        val diagnosis = diagnoses[position]
        if (holder is LatestDiagnosisViewHolder) {
            holder.bind(diagnosis)
        } else {
            holder.title?.text = diagnosis.diagnosis ?: "No diagnosis"
            holder.desc?.text = diagnosis.createdAt?.toString() ?: "No date available"
        }
    }

    override fun getItemCount(): Int = diagnoses.size

    open class DiagnosisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView? = itemView.findViewById(android.R.id.text1)
        val desc: TextView? = itemView.findViewById(android.R.id.text2)
    }

    class LatestDiagnosisViewHolder(
        itemView: View,
        private val onDownloadClick: (Diagnosis) -> Unit
    ) : DiagnosisViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val patientCodeText: TextView = itemView.findViewById(R.id.patientCodeText)
        private val addressText: TextView = itemView.findViewById(R.id.addressText)
        private val weightText: TextView = itemView.findViewById(R.id.weightText)
        private val heightText: TextView = itemView.findViewById(R.id.heightText)
        private val bpText: TextView = itemView.findViewById(R.id.bpText)
        private val respirationRateText: TextView = itemView.findViewById(R.id.respirationRateText)
        private val oxygenSaturationText: TextView = itemView.findViewById(R.id.oxygenSaturationText)
        private val temperatureText: TextView = itemView.findViewById(R.id.temperatureText)
        private val diagnosisText: TextView = itemView.findViewById(R.id.diagnosisText)
        private val medicineText: TextView = itemView.findViewById(R.id.medicineText)
        private val adviceText: TextView = itemView.findViewById(R.id.adviceText)
        private val nextVisitText: TextView = itemView.findViewById(R.id.nextVisitText)
        private val downloadButton: Button = itemView.findViewById(R.id.downloadButton)

        fun bind(d: Diagnosis) {
            titleText.text = "Latest Diagnosis"
            patientCodeText.text = "Patient Code: ${d.patientCode}"
            addressText.text = "Address: ${d.address ?: "-"}"
            weightText.text = "Weight: ${d.weight ?: "-"}"
            heightText.text = "Height: ${d.height ?: "-"}"
            bpText.text = "BP: ${d.bloodPressure ?: "-"}"
            respirationRateText.text = "Respiration Rate: -"
            oxygenSaturationText.text = "Oxygen Saturation: -"
            temperatureText.text = "Temperature: -"
            diagnosisText.text = "Diagnosis: ${d.diagnosis ?: "-"}"
            medicineText.text = "Medicine: ${d.medicineName ?: "-"}"
            adviceText.text = "Advice: ${d.adviceGiven ?: "-"}"
            
            // Format next visit date
            nextVisitText.text = "Next Visit: ${d.nextVisit?.let { dateStr ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    val date = inputFormat.parse(dateStr)
                    outputFormat.format(date)
                } catch (e: Exception) {
                    dateStr
                }
            } ?: "-"}"
            
            downloadButton.setOnClickListener {
                onDownloadClick(d)
            }
        }
    }
} 