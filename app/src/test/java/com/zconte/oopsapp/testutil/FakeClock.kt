package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.domain.util.Clock

class FakeClock(private var millis: Long = 0L) : Clock {
    override fun nowMillis(): Long = millis
    fun advanceBy(deltaMillis: Long) {
        millis += deltaMillis
    }
}
