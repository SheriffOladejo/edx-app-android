package com.grassroot.academy.module.registration.view;

import android.text.InputType;
import android.view.View;

import com.grassroot.academy.module.registration.model.RegistrationFormField;

/**
 * Created by rohan on 2/11/15.
 */
class RegistrationTextView extends RegistrationEditTextView {

    public RegistrationTextView(RegistrationFormField field, View view) {
        super(field, view);
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
    }
}
