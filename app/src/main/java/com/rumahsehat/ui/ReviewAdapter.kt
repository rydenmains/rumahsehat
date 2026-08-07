package com.rumahsehat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rumahsehat.R
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.databinding.ItemReviewBinding

class ReviewAdapter(
    private val titles: Map<String, Int>,
    private val weights: Map<String, Int>
) : ListAdapter<ScoreItem, ReviewAdapter.ViewHolder>(Diff) {

    class ViewHolder(val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val max = weights[item.itemId] ?: 0
        holder.binding.apply {
            val titleRes = titles[item.itemId]
            if (titleRes != null) tvTitle.setText(titleRes) else tvTitle.text = item.itemId
            if (item.isApplicable) {
                tvLevel.setText(AssessmentLevel.labelRes(item.score, max))
                tvLevel.setTextColor(holder.itemView.context.getColor(R.color.forest_green))
            } else {
                tvLevel.setText(R.string.item_not_applicable)
                tvLevel.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            }
        }
    }

    companion object Diff : DiffUtil.ItemCallback<ScoreItem>() {
        override fun areItemsTheSame(oldItem: ScoreItem, newItem: ScoreItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScoreItem, newItem: ScoreItem) = oldItem == newItem
    }
}