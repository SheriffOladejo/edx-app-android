package com.grassroot.academy.event

import com.grassroot.academy.deeplink.PushLink

class PushLinkReceivedEvent(val pushLink: PushLink) : BaseEvent()
