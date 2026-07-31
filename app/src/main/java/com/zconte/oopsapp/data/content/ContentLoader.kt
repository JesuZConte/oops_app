package com.zconte.oopsapp.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface ContentLoader {
    fun loadPack(assetPath: String): ContentPack
}

class AssetContentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : ContentLoader {
    override fun loadPack(assetPath: String): ContentPack {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return json.decodeFromString(ContentPack.serializer(), text)
    }
}
