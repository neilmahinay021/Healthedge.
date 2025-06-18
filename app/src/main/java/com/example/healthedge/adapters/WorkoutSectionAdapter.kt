package com.example.healthedge.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.healthedge.R
import com.example.healthedge.models.DiseaseWorkout
import com.example.healthedge.WorkoutAdapter
import java.util.Calendar
import java.util.TimeZone
import android.content.Context
import android.preference.PreferenceManager
import android.widget.LinearLayout

class WorkoutSectionAdapter(
    private val onWorkoutClick: (DiseaseWorkout) -> Unit
) : RecyclerView.Adapter<WorkoutSectionAdapter.SectionViewHolder>() {

    private var sections: List<WorkoutSection> = emptyList()

    data class WorkoutSection(
        val diseaseName: String,
        val workouts: List<DiseaseWorkout>
    )

    data class WorkoutPreview(
        val today: DiseaseWorkout?,
        val tomorrow: DiseaseWorkout?
    )

    fun updateSections(newSections: List<WorkoutSection>) {
        sections = newSections
        notifyDataSetChanged()
    }

    fun getSections(): List<WorkoutSection> = sections

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount() = sections.size

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sectionTitle: TextView = itemView.findViewById(R.id.sectionTitle)
        private val todayCard: View = itemView.findViewById(R.id.todayWorkoutCard)
        private val tomorrowCard: View = itemView.findViewById(R.id.tomorrowWorkoutCard)
        private val todayTitle: TextView = itemView.findViewById(R.id.todayWorkoutTitle)
        private val todayDesc: TextView = itemView.findViewById(R.id.todayWorkoutDesc)
        private val todayImage: ImageView = itemView.findViewById(R.id.todayWorkoutImage)
        private val tomorrowTitle: TextView = itemView.findViewById(R.id.tomorrowWorkoutTitle)
        private val tomorrowImage: ImageView = itemView.findViewById(R.id.tomorrowWorkoutImage)
        private val todayLabel: TextView = itemView.findViewById(R.id.todayLabel)
        private val tomorrowLabel: TextView = itemView.findViewById(R.id.tomorrowLabel)
        private val todayLevelBadge: TextView = itemView.findViewById(R.id.todayLevelBadge)
        private val todayProgressBar: ProgressBar = itemView.findViewById(R.id.todayProgressBar)
        private val todayProgressText: TextView = itemView.findViewById(R.id.todayProgressText)
        private val todayWorkoutsContainer: LinearLayout = itemView.findViewById(R.id.todayWorkoutsContainer)

        fun bind(section: WorkoutSection) {
            sectionTitle.text = section.diseaseName
            val originalWorkouts = section.workouts
            val workouts = originalWorkouts.toMutableList()
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila")) // PHT
            val daysSinceEpoch = (calendar.timeInMillis / (1000 * 60 * 60 * 24)).toInt()
            val todayIndex = daysSinceEpoch % workouts.size
            val tomorrowIndex = (todayIndex + 1) % workouts.size
            // Today's workouts: all except tomorrow's preview
            val todayWorkouts = workouts.filterIndexed { idx, _ -> idx != tomorrowIndex }
            val tomorrowWorkout = if (workouts.size > 1) workouts[tomorrowIndex] else null

            // Remove all previous views
            todayWorkoutsContainer.removeAllViews()
            // Dynamically add a card for each workout
            for (workout in todayWorkouts) {
                val cardView = LayoutInflater.from(itemView.context).inflate(R.layout.item_today_workout_card, todayWorkoutsContainer, false)
                // Bind data to the card
                val title = cardView.findViewById<TextView>(R.id.todayWorkoutTitle)
                val desc = cardView.findViewById<TextView>(R.id.todayWorkoutDesc)
                val image = cardView.findViewById<ImageView>(R.id.todayWorkoutImage)
                val levelBadge = cardView.findViewById<TextView>(R.id.todayLevelBadge)
                val progressBar = cardView.findViewById<ProgressBar>(R.id.todayProgressBar)
                val progressText = cardView.findViewById<TextView>(R.id.todayProgressText)
                title.text = workout.workout_name
                desc.text = workout.description
                Glide.with(cardView.context).asGif().load(workout.gif_url).into(image)
                levelBadge.text = when (workout.intensity?.lowercase()) {
                    "low" -> "Beginner"
                    "medium" -> "Intermediate"
                    "high" -> "Advanced"
                    else -> workout.intensity?.capitalize() ?: "Beginner"
                }
                progressBar.progress = workout.progress
                progressText.text = "${workout.progress}%"
                cardView.setOnClickListener { onWorkoutClick(workout) }
                todayWorkoutsContainer.addView(cardView)
            }

            todayLabel.visibility = View.VISIBLE
            todayCard.visibility = View.GONE

            // Tomorrow preview (single card)
            if (tomorrowWorkout != null) {
                tomorrowTitle.text = tomorrowWorkout.workout_name
                Glide.with(itemView.context).asGif().load(tomorrowWorkout.gif_url).into(tomorrowImage)
                tomorrowCard.isClickable = false
                tomorrowCard.isFocusable = false
                tomorrowCard.setOnClickListener(null)
                tomorrowCard.setOnLongClickListener(null)
                tomorrowCard.visibility = View.VISIBLE
                tomorrowLabel.visibility = View.VISIBLE
            } else {
                tomorrowCard.visibility = View.GONE
                tomorrowLabel.visibility = View.GONE
            }
        }

        private fun saveWorkoutOrderToPrefs(context: Context, sectionName: String, workouts: List<DiseaseWorkout>) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val order = workouts.joinToString(",") { it.workout_name }
            prefs.edit().putString("workout_order_$sectionName", order).apply()
        }
    }
} 