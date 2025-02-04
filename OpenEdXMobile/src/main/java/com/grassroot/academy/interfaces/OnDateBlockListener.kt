package com.grassroot.academy.interfaces

/**
 * Provides callbacks to handle the click on the date block in the course.
 */
interface OnDateBlockListener {
    fun onClick(link: String, blockId: String)
}
