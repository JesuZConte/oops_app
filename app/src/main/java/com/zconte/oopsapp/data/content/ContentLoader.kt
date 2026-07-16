package com.zconte.oopsapp.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ContentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    fun loadPack(assetPath: String): ContentPack {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString(ContentPack.serializer(), text)
    }
}