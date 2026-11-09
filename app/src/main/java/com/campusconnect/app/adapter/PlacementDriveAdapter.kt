package com.campusconnect.app.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.data.model.PlacementDriveModel
import com.campusconnect.app.databinding.ItemPlacementDriveBinding

class PlacementDriveAdapter(
    private val onDriveClick: (PlacementDriveModel) -> Unit
) : RecyclerView.Adapter<PlacementDriveAdapter.DriveViewHolder>() {

    private var drives: List<PlacementDriveModel> = emptyList()

    fun submitList(newList: List<PlacementDriveModel>) {
        drives = newList
        notifyDataSetChanged()
    }

    inner class DriveViewHolder(val binding: ItemPlacementDriveBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriveViewHolder {
        val binding = ItemPlacementDriveBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DriveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DriveViewHolder, position: Int) {
        val drive = drives[position]
        val context = holder.itemView.context

        with(holder.binding) {
            tvJobTitle.text = drive.jobTitle
            tvCompanyName.text = drive.companyName
            tvSalaryBadge.text = drive.salaryPackageCtc
            tvLocation.text = "📍 ${drive.jobLocation}"
            
            val minCgpa = drive.eligibilityCriteria?.minCgpa ?: 6.0
            tvMinCgpa.text = "🎓 Min CGPA: $minCgpa"

            val deadline = if (drive.applicationDeadline.isNotBlank()) {
                val formatted = drive.applicationDeadline.take(10)
                "Deadline: $formatted"
            } else {
                "Open for Applications"
            }
            tvDeadline.text = deadline

            when (drive.isStudentEligible) {
                true -> {
                    tvEligibilityStatus.text = "● Eligible"
                    tvEligibilityStatus.setBackgroundResource(R.drawable.bg_badge_eligible)
                    tvEligibilityStatus.setTextColor(ContextCompat.getColor(context, R.color.status_registered_text))
                }
                false -> {
                    tvEligibilityStatus.text = "● Ineligible"
                    tvEligibilityStatus.setBackgroundResource(R.drawable.bg_badge_ineligible)
                    tvEligibilityStatus.setTextColor(ContextCompat.getColor(context, R.color.status_full_text))
                }
                null -> {
                    tvEligibilityStatus.text = "Check Eligibility →"
                    tvEligibilityStatus.background = null
                    tvEligibilityStatus.setTextColor(ContextCompat.getColor(context, R.color.primary))
                }
            }

            if (drive.companyLogoUrl.isNotBlank()) {
                Glide.with(context)
                    .load(drive.companyLogoUrl)
                    .placeholder(R.drawable.ic_logo)
                    .error(R.drawable.ic_logo)
                    .into(ivCompanyLogo)
            } else {
                ivCompanyLogo.setImageResource(R.drawable.ic_logo)
            }

            root.setOnClickListener {
                onDriveClick(drive)
            }
        }
    }

    override fun getItemCount(): Int = drives.size
}
