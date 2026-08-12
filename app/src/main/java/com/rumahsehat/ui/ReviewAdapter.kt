package com.rumahsehat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rumahsehat.R
import com.rumahsehat.data.model.FormItem
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.databinding.ItemReviewBinding
import com.rumahsehat.databinding.ItemReviewHeaderBinding

/**
 * Hasil per item dikelompokkan per section (I/II/III), diturunkan dari prefix
 * itemId ("1.","2.","3.") — tidak butuh field baru di data model.
 */
class ReviewAdapter(
    private val itemsById: Map<String, FormItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class Row {
        data class Header(val titleRes: Int) : Row()
        data class Item(val scoreItem: ScoreItem) : Row()
    }

    private var rows: List<Row> = emptyList()

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is Row.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemReviewHeaderBinding.inflate(inflater, parent, false))
        } else {
            ViewHolder(ItemReviewBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> {
                (holder as HeaderHolder).binding.root.setText(row.titleRes)
            }
            is Row.Item -> {
                (holder as ViewHolder).binding.bind(row.scoreItem, itemsById)
            }
        }
    }

    fun submitList(items: List<ScoreItem>) {
        rows = groupBySection(items)
        notifyDataSetChanged()
    }

    private fun groupBySection(items: List<ScoreItem>): List<Row> {
        val result = mutableListOf<Row>()
        var lastPrefix: String? = null
        for (scoreItem in items) {
            val prefix = scoreItem.itemId.substringBefore('.')
            if (prefix != lastPrefix) {
                result += Row.Header(sectionTitleRes(prefix))
                lastPrefix = prefix
            }
            result += Row.Item(scoreItem)
        }
        return result
    }

    private fun sectionTitleRes(section: String): Int = when (section) {
        "2" -> R.string.cat_sanitation
        "3" -> R.string.cat_behavior
        else -> R.string.cat_housing
    }

    class ViewHolder(val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root)

    class HeaderHolder(val binding: ItemReviewHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}

private fun ItemReviewBinding.bind(scoreItem: ScoreItem, itemsById: Map<String, FormItem>) {
    val formItem = itemsById[scoreItem.itemId]
    if (formItem != null) tvTitle.setText(formItem.titleRes) else tvTitle.text = scoreItem.itemId
    if (scoreItem.isApplicable) {
        bindOptionLabel(tvLevel, formItem, scoreItem.score, scoreItem.optionIndex)
        tvLevel.setTextColor(root.context.getColor(R.color.forest_green))
    } else {
        tvLevel.setText(R.string.item_not_applicable)
        tvLevel.setTextColor(root.context.getColor(R.color.text_secondary))
    }
}

/** Tampilkan label opsi terpilih; skor numerik tersembunyi. */
private fun bindOptionLabel(tv: TextView, formItem: FormItem?, score: Int, optionIndex: Int) {
    val option = formItem?.options?.getOrNull(optionIndex)
        ?: formItem?.options?.firstOrNull {
            formItem.scoreForOption(formItem.options.indexOf(it)) == score
        }
    if (option != null) {
        tv.text = "${option.letter}. ${option.label}"
    } else {
        tv.setText(AssessmentLevel.labelRes(score, formItem?.maxScore ?: 0))
    }
}