package com.grassroot.academy.view

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import com.grassroot.academy.R
import com.grassroot.academy.base.BaseSingleFragmentActivity

@AndroidEntryPoint
class CourseHandoutActivity : BaseSingleFragmentActivity() {
    override fun onStart() {
        super.onStart()
        title = getString(R.string.tab_label_handouts)
    }

    override fun getFirstFragment(): Fragment {
        val courseHandoutFragment = CourseHandoutFragment()
        courseHandoutFragment.arguments = intent.extras
        return courseHandoutFragment
    }
}
