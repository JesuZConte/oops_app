package com.zconte.oopsapp.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.zconte.oopsapp.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(): SettingsRepositoryImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
            produceFile = { tempFolder.newFile("test.preferences_pb") }
        )
        return SettingsRepositoryImpl(dataStore)
    }

    @Test
    fun `defaults to SYSTEM when nothing stored`() = runTest {
        val repository = createRepository()

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun `setThemeMode persists and is reflected in the flow`() = runTest {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun `setThemeMode overwrites a previous value`() = runTest {
        val repository = createRepository()

        repository.setThemeMode(ThemeMode.DARK)
        repository.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
    }
}