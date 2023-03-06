package com.grassroot.academy.event

import android.net.Uri

data class FileSelectionEvent(val files: Array<Uri>?) : BaseEvent()
