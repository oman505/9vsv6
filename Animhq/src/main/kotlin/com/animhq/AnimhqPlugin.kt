package com.animhq

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class AnimhqPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(AnimhqProvider())
    }
}
