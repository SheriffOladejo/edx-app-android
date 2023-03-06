package com.grassroot.academy.module.registration.view

import android.view.View
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.grassroot.academy.databinding.ViewRegisterCheckboxBinding
import com.grassroot.academy.module.registration.model.RegistrationFormField
import com.grassroot.academy.module.registration.view.IRegistrationFieldView.IActionListener

class RegistrationCheckBoxView(
    field: RegistrationFormField,
    view: View
) : IRegistrationFieldView {

    private val binding: ViewRegisterCheckboxBinding = ViewRegisterCheckboxBinding.bind(view)
    private val mField: RegistrationFormField = field
    private var actionListener: IActionListener? = null

    init {
        binding.registerCheckbox.text = field.label
        binding.registerCheckbox.isChecked = field.defaultValue?.toBoolean() ?: true
        binding.registerCheckbox.setOnCheckedChangeListener { _, _ ->
            actionListener?.onClickAgreement()
        }
    }

    override fun getCurrentValue(): JsonElement = JsonPrimitive(binding.registerCheckbox.isChecked)

    override fun getField(): RegistrationFormField = mField

    override fun getOnErrorFocusView(): View = binding.registerCheckbox

    override fun getView(): View = binding.root

    override fun hasValue(): Boolean = true

    override fun isValidInput(): Boolean = true

    override fun setRawValue(value: String?): Boolean = false

    override fun setInstructions(instructions: String?) {
    }

    override fun handleError(errorMessage: String?) {
    }

    override fun setEnabled(enabled: Boolean) {
        binding.registerCheckbox.isEnabled = enabled
    }

    override fun setActionListener(actionListener: IActionListener?) {
        this.actionListener = actionListener
    }
}
