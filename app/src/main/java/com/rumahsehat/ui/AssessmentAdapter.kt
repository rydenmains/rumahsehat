package com.rumahsehat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rumahsehat.R
import com.rumahsehat.data.model.Assessment
import com.rumahsehat.databinding.ItemAssessmentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssessmentAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<Assessment, AssessmentAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemAssessmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAssessmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val assessment = getItem(position)
        holder.binding.apply {
            root.setOnClickListener { onClick(assessment.id) }
            tvTitle.text = assessment.company
            tvMeta.text = buildString {
                append(assessment.assessorId)
                append(" · ")
                append(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(assessment.createdAt)))
            }
            tvStatus.visibility = android.view.View.GONE
            tvSync.setText(
                if (assessment.syncStatus == "SYNCED") R.string.status_synced else R.string.status_pending
            )
            val syncColor = holder.itemView.context.getColor(
                if (assessment.syncStatus == "SYNCED") R.color.status_pass else R.color.status_pending
            )
            tvSync.setTextColor(syncColor)
            dotSync.setBackgroundTintList(android.content.res.ColorStateList.valueOf(syncColor))
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Assessment>() {
        override fun areItemsTheSame(oldItem: Assessment, newItem: Assessment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Assessment, newItem: Assessment) = oldItem == newItem
    }
}