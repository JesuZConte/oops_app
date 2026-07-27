package com.zconte.oopsapp.data.util

import com.zconte.oopsapp.domain.util.Clock
import javax.inject.Inject

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
