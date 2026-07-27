package com.zconte.oopsapp.domain.util

/** Seam for "now", so a countdown timer can be driven by a fake clock in JVM tests. */
interface Clock {
    fun nowMillis(): Long
}
