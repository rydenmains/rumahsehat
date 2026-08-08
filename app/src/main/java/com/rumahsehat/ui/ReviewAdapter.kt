package com.rumahsehat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rumahsehat.R
import com.rumahsehat.data.model.FormItem
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.databinding.ItemReviewBinding

class ReviewAdapter(
    private val itemsById: Map<String, FormItem>
) : ListAdapter<ScoreItem, ReviewAdapter.ViewHolder>(DiffItemCallback) {

    class ViewHolder(val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val formItem = itemsById[item.itemId]
        holder.binding.apply {
            if (formItem != null) tvTitle.setText(formItem.titleRes) else tvTitle.text = item.itemId
            if (item.isApplicable) {
                bindOptionLabel(tvLevel, formItem, item.score)
                tvLevel.setTextColor(holder.itemView.context.getColor(R.color.forest_green))
            } else {
                tvLevel.setText(R.string.item_not_applicable)
                tvLevel.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            }
        }
    }

    /** Tampilkan label opsi terpilih; skor numerik tersembunyi. */
    private fun bindOptionLabel(tv: TextView, formItem: FormItem?, score: Int) {
        val option = formItem?.options?.firstOrNull {
            formItem.scoreForOption(formItem.options.indexOf(it)) == score
        }
        if (option != null) {
            tv.text = "${option.letter}. ${option.label}"
        } else {
            tv.setText(AssessmentLevel.labelRes(score, formItem?.maxScore ?: 0))
        }
    }

    companion object DiffItemCallback : DiffUtil.ItemCallback<ScoreItem>() {
        override fun areItemsTheSame(oldItem: ScoreItem, newItem: ScoreItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScoreItem, newItem: ScoreItem) = oldItem == newItem
    }
}