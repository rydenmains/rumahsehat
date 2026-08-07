package com.rumahsehat.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rumahsehat.data.model.FormItem
import com.rumahsehat.databinding.ItemScoreFormBinding

class FormAdapter(private val items: List<FormItem>) : RecyclerView.Adapter<FormAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemScoreFormBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScoreFormBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvTitle.setText(item.titleRes)

            // Setup Slider for 5 discrete steps (0, 25%, 50%, 75%, 100%)
            sliderScore.valueFrom = 0f
            sliderScore.valueTo = 4f
            sliderScore.stepSize = 1f
            
            // Map score to slider value
            val initialSliderValue = when (item.currentScore) {
                (item.maxScore * 0.25).toInt() -> 1f
                (item.maxScore * 0.50).toInt() -> 2f
                (item.maxScore * 0.75).toInt() -> 3f
                item.maxScore -> 4f
                else -> 0f
            }
            sliderScore.value = initialSliderValue
            etScore.setText(item.currentScore.toString())

            // Sync Slider -> EditText & Model
            sliderScore.clearOnChangeListeners()
            sliderScore.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val score = when (value) {
                        1f -> (item.maxScore * 0.25).toInt()
                        2f -> (item.maxScore * 0.50).toInt()
                        3f -> (item.maxScore * 0.75).toInt()
                        4f -> item.maxScore
                        else -> 0
                    }
                    item.currentScore = score
                    etScore.setText(score.toString())
                }
            }

            // Sync EditText -> Slider & Model
            etScore.removeTextChangedListener(etScore.tag as? TextWatcher)
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val score = s.toString().toIntOrNull() ?: 0
                    if (score != item.currentScore) {
                        item.currentScore = score
                        // Update slider only if it hits a known step exactly
                        val sliderVal = when (score) {
                            0 -> 0f
                            (item.maxScore * 0.25).toInt() -> 1f
                            (item.maxScore * 0.50).toInt() -> 2f
                            (item.maxScore * 0.75).toInt() -> 3f
                            item.maxScore -> 4f
                            else -> -1f // Don't move slider if manual entry is "in-between"
                        }
                        if (sliderVal != -1f) sliderScore.value = sliderVal
                    }
                }
            }
            etScore.addTextChangedListener(textWatcher)
            etScore.tag = textWatcher

            cbApplicable.setOnCheckedChangeListener { _, isChecked ->
                item.isApplicable = isChecked
                sliderScore.isEnabled = isChecked
                tilScore.isEnabled = isChecked
            }
            
            sliderScore.isEnabled = item.isApplicable
            tilScore.isEnabled = item.isApplicable
        }
    }

    override fun getItemCount() = items.size
}
