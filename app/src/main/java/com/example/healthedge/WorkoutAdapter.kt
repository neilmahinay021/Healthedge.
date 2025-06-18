package com.example.healthedge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.healthedge.models.DiseaseWorkout

class WorkoutAdapter(
    private val onWorkoutClick: (DiseaseWorkout) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {
    private var workouts: List<DiseaseWorkout> = emptyList()

    fun submitList(list: List<DiseaseWorkout>?) {
        workouts = list ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        holder.bind(workout)
        holder.itemView.setOnClickListener { onWorkoutClick(workout) }
    }

    override fun getItemCount(): Int = workouts.size

    class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.workoutImage)
        private val name: TextView = itemView.findViewById(R.id.workoutName)
        private val levelBadge: TextView = itemView.findViewById(R.id.levelBadge)
        private val workoutDetails: TextView = itemView.findViewById(R.id.workoutDetails)

        fun bind(workout: DiseaseWorkout) {
            name.text = workout.workout_name
            levelBadge.text = when (workout.intensity?.lowercase()) {
                "low" -> "Beginner"
                "medium" -> "Intermediate"
                "high" -> "Advanced"
                else -> workout.intensity?.capitalize() ?: "Beginner"
            }
            workoutDetails.text = workout.description
            Glide.with(itemView.context)
                .asGif()
                .load(workout.gif_url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(image)
        }
    }
} 