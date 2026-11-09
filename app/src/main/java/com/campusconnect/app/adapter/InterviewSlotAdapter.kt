package com.campusconnect.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.data.model.InterviewSlotModel
import com.campusconnect.app.databinding.ItemInterviewSlotBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class InterviewSlotAdapter(
    private val onSlotSelected: (InterviewSlotModel) -> Unit
) : RecyclerView.Adapter<InterviewSlotAdapter.SlotViewHolder>() {

    private var slots: List<InterviewSlotModel> = emptyList()
    private var selectedSlotId: String? = null

    fun submitList(newList: List<InterviewSlotModel>) {
        slots = newList
        notifyDataSetChanged()
    }

    fun getSelectedSlot(): InterviewSlotModel? {
        return slots.find { it.id == selectedSlotId }
    }

    inner class SlotViewHolder(val binding: ItemInterviewSlotBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val binding = ItemInterviewSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        val context = holder.itemView.context

        with(holder.binding) {
            val formattedTime = formatTimeWindow(slot.startTime, slot.endTime)
            tvTimeWindow.text = formattedTime
            tvVenue.text = if (slot.venueOrLink.isNotBlank()) "📍 ${slot.venueOrLink}" else "📍 Venue TBA"

            val isSelected = slot.id == selectedSlotId
            rbSelected.isChecked = isSelected

            if (slot.isAvailable) {
                cardSlot.alpha = 1.0f
                cardSlot.isEnabled = true
                rbSelected.isEnabled = true
                tvAvailabilityBadge.text = "Available"
                tvAvailabilityBadge.setBackgroundResource(R.drawable.bg_badge_eligible)
                tvAvailabilityBadge.setTextColor(ContextCompat.getColor(context, R.color.status_registered_text))

                if (isSelected) {
                    cardSlot.strokeColor = ContextCompat.getColor(context, R.color.primary)
                    cardSlot.strokeWidth = 4
                } else {
                    cardSlot.strokeColor = ContextCompat.getColor(context, R.color.border_light)
                    cardSlot.strokeWidth = 2
                }

                cardSlot.setOnClickListener {
                    selectedSlotId = slot.id
                    notifyDataSetChanged()
                    onSlotSelected(slot)
                }
                rbSelected.setOnClickListener {
                    selectedSlotId = slot.id
                    notifyDataSetChanged()
                    onSlotSelected(slot)
                }
            } else {
                cardSlot.alpha = 0.5f
                cardSlot.isEnabled = false
                rbSelected.isEnabled = false
                cardSlot.strokeColor = ContextCompat.getColor(context, R.color.border_light)
                cardSlot.strokeWidth = 2
                tvAvailabilityBadge.text = if (slot.isCancelled) "Cancelled" else "Full"
                tvAvailabilityBadge.setBackgroundResource(R.drawable.bg_badge_ineligible)
                tvAvailabilityBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_rose))
                cardSlot.setOnClickListener(null)
                rbSelected.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount(): Int = slots.size

    private fun formatTimeWindow(start: String, end: String): String {
        return try {
            val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val displayFormat = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
            val timeOnlyFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            val startDate = isoParser.parse(start)
            val endDate = isoParser.parse(end)

            if (startDate != null && endDate != null) {
                "${displayFormat.format(startDate)} - ${timeOnlyFormat.format(endDate)}"
            } else {
                "$start - $end"
            }
        } catch (e: Exception) {
            // Fallback for simple time strings
            if (start.length >= 16 && end.length >= 16) {
                "${start.substring(11, 16)} - ${end.substring(11, 16)}"
            } else {
                "$start - $end"
            }
        }
    }
}
