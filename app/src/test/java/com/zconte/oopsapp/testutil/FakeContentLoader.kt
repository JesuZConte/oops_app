package com.zconte.oopsapp.testutil

import com.zconte.oopsapp.data.content.ContentLoader
import com.zconte.oopsapp.data.content.ContentPack

class FakeContentLoader(private val packsByPath: Map<String, ContentPack>) : ContentLoader {
    override fun loadPack(assetPath: String): ContentPack =
        packsByPath[assetPath] ?: error("No fake pack registered for path: $assetPath")
}
