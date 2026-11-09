package com.campusconnect.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.data.model.ApplicationModel
import com.campusconnect.app.databinding.ItemApplicationBinding

class ApplicationAdapter(
    private val onWithdrawClick: (ApplicationModel) -> Unit,
    private val onBookSlotClick: (ApplicationModel) -> Unit = {}
) : RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {

    private var applications: List<ApplicationModel> = emptyList()

    fun submitList(newList: List<ApplicationModel>) {
        applications = newList
        notifyDataSetChanged()
    }

    inner class ApplicationViewHolder(val binding: ItemApplicationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val binding = ItemApplicationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ApplicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val app = applications[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvCompanyName.text = app.companyName ?: "Recruiter Drive"
            tvJobTitle.text = app.driveTitle ?: "Campus Placement"
            tvSalaryPackage.text = app.salaryPackage ?: "Competitive"

            val dateStr = if (app.appliedAt.isNotBlank()) {
                val formatted = if (app.appliedAt.length >= 10) app.appliedAt.take(10) else app.appliedAt
                "Applied: $formatted"
            } else {
                "Applied Recently"
            }
            tvAppliedDate.text = dateStr

            tvApplicationStatus.text = app.status.replace("_", " ")

            when (app.status.uppercase()) {
                "APPLIED" -> {
                    tvApplicationStatus.setBackgroundResource(R.drawable.bg_rounded_card)
                    tvApplicationStatus.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    btnBookSlot.visibility = View.GONE
                    btnWithdraw.visibility = View.VISIBLE
                    divider.visibility = View.VISIBLE
                    layoutActions.visibility = View.VISIBLE
                    tvWithdrawalReason.visibility = View.GONE
                }
                "SHORTLISTED", "ASSESSMENT", "TECHNICAL_INTERVIEW", "HR_INTERVIEW" -> {
                    tvApplicationStatus.setBackgroundResource(R.drawable.bg_badge_eligible)
                    tvApplicationStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_amber))
                    btnBookSlot.visibility = View.VISIBLE
                    btnBookSlot.text = "Book Interview Slot"
                    btnWithdraw.visibility = View.VISIBLE
                    divider.visibility = View.VISIBLE
                    layoutActions.visibility = View.VISIBLE
                    tvWithdrawalReason.visibility = View.GONE
                }
                "INTERVIEW_SCHEDULED" -> {
                    tvApplicationStatus.setBackgroundResource(R.drawable.bg_badge_eligible)
                    tvApplicationStatus.setTextColor(ContextCompat.getColor(context, R.color.primary))
                    btnBookSlot.visibility = View.VISIBLE
                    btnBookSlot.text = "Reschedule Slot"
                    btnWithdraw.visibility = View.VISIBLE
                    divider.visibility = View.VISIBLE
                    layoutActions.visibility = View.VISIBLE
                    tvWithdrawalReason.visibility = View.GONE
                }
                "SELECTED", "ACCEPTED", "OFFER_EXTENDED" -> {
                    tvApplicationStatus.setBackgroundResource(R.drawable.bg_badge_eligible)
                    tvApplicationStatus.setTextColor(ContextCompat.getColor(context, R.color.status_registered_text))
                    btnBookSlot.visibility = View.GONE
                    btnWithdraw.visibility = View.GONE
                    divider.visibility = View.GONE
                    layoutActions.visibility = View.GONE
                    tvWithdrawalReason.visibility = View.GONE
                }
                "REJECTED" -> {
                    tvApplicationStatus.setBackgroundResource(R.drawable.bg_badge_ineligible)
                    tvApplicationStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_rose))
                    btnBookSlot.visibility = View.GONE
                    btnWithdraw.visibility = View.GONE
                    divider.visibility = View.GONE
                    layoutActions.visibility = View.GONE
                    tvWithdrawalReason.visibility = View.GONE
                }
                "WITHDRAWN" -> {
                    tvApplicationStatus.setBackgroundResource(R.drawable.bg_rounded_card)
                    tvApplicationStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    btnBookSlot.visibility = View.GONE
                    btnWithdraw.visibility = View.GONE
                    divider.visibility = View.GONE
                    layoutActions.visibility = View.GONE
                    if (!app.withdrawalReason.isNullOrBlank()) {
                        tvWithdrawalReason.visibility = View.VISIBLE
                        tvWithdrawalReason.text = "Reason: ${app.withdrawalReason}"
                    } else {
                        tvWithdrawalReason.visibility = View.GONE
                    }
                }
                else -> {
                    tvApplicationStatus.setBackgroundResource(R.drawable.bg_rounded_card)
                    tvApplicationStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    btnBookSlot.visibility = View.GONE
                    btnWithdraw.visibility = View.GONE
                    divider.visibility = View.GONE
                    layoutActions.visibility = View.GONE
                    tvWithdrawalReason.visibility = View.GONE
                }
            }

            btnWithdraw.setOnClickListener {
                onWithdrawClick(app)
            }

            btnBookSlot.setOnClickListener {
                onBookSlotClick(app)
            }
        }
    }

    override fun getItemCount(): Int = applications.size
}
