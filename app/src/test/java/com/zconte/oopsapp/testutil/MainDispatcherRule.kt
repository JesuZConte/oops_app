package com.zconte.oopsapp.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Installs a coroutine dispatcher as [Dispatchers.Main] for the duration of a test, so
 * `viewModelScope` (which defaults to `Dispatchers.Main.immediate`) can run in plain JUnit4
 * tests. Uses [UnconfinedTestDispatcher] by default: launched coroutines run eagerly, so
 * assertions right after a ViewModel call see the coroutine's effects without an explicit
 * `advanceUntilIdle()`.
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
