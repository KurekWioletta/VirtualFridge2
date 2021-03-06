package com.example.virtualfridge.domain.createEvent

import android.content.Intent
import android.os.Bundle
import com.example.virtualfridge.R
import com.example.virtualfridge.domain.base.BaseActivity
import com.example.virtualfridge.utils.BaseValidationViewModel
import kotlinx.android.synthetic.main.activity_create_event.*
import javax.inject.Inject

class CreateEventActivity : BaseActivity() {

    @Inject
    lateinit var presenter: CreateEventActivityPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        btnCreateEvent.setOnClickListener {
            presenter.createEventClicked(
                etTitle.text.toString(),
                etDescription.text.toString(),
                etPlace.text.toString(),
                etStartDate.text.toString(),
                etEndDate.text.toString()
            )
        }
    }

    fun showValidationResults(validationViewModel: ValidationViewModel) {
        etTitle.error = validationViewModel.titleError
        etStartDate.error = validationViewModel.startDateError
        etEndDate.error = validationViewModel.endDateError
    }

    data class ValidationViewModel(
        val titleError: String?,
        val startDateError: String?,
        val endDateError: String?
    ) : BaseValidationViewModel() {
        override fun toList() = listOf(titleError)
    }

    companion object {
        const val RC_CREATE_EVENT = 1

        fun getIntent(activity: BaseActivity) = Intent(activity, CreateEventActivity::class.java)
    }
}
