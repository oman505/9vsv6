package com.ristoanime

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.WebViewResolver
import org.jsoup.nodes.Document

class RistoanimeProvider : MainAPI() {
    override var mainUrl = "https://ristoanime.co"
    override var name = "Ristoanime"
    override val hasMainPage = true
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val homePageList = ArrayList<HomePageList>()

        // Recent episodes or updates
        // Based on typical Arabic anime site structures
        val sections = document.select("div.widget-content, div.main-widget")
        sections.forEach { section ->
            val title = section.selectFirst("h3, h2")?.text() ?: "أحدث الإضافات"
            val items = section.select("div.anime-card, div.ep-card, div.item").mapNotNull {
                val a = it.selectFirst("a") ?: return@mapNotNull null
                val href = a.attr("href")
                val name = it.selectFirst("h3, .title")?.text() ?: ""
                val poster = it.selectFirst("img")?.attr("src")
                
                newAnimeSearchResponse(name, href, TvType.Anime) {
                    this.posterUrl = poster
                }
            }
            if (items.isNotEmpty()) {
                homePageList.add(HomePageList(title, items))
            }
        }

        if (homePageList.isEmpty()) {
            // Fallback for different layout
            val items = document.select("div.item").mapNotNull {
                val a = it.selectFirst("a") ?: return@mapNotNull null
                val href = a.attr("href")
                val name = it.selectFirst("h3, .title")?.text() ?: ""
                val poster = it.selectFirst("img")?.attr("src")
                
                newAnimeSearchResponse(name, href, TvType.Anime) {
                    this.posterUrl = poster
                }
            }
            if (items.isNotEmpty()) homePageList.add(HomePageList("أحدث الحلقات", items))
        }

        return newHomePageResponse(homePageList)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document

        return document.select("div.item, div.anime-card").mapNotNull {
            val a = it.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            val name = it.selectFirst("h3, .title")?.text() ?: ""
            val poster = it.selectFirst("img")?.attr("src")

            newAnimeSearchResponse(name, href, TvType.Anime) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1.title, h1")?.text() ?: ""
        val poster = document.selectFirst("div.thumb img, .poster img")?.attr("src")
        val plot = document.selectFirst("div.story, .description")?.text()
        
        val episodes = document.select("div.episodes-list a, .episode-item a").mapNotNull {
            val href = it.attr("href")
            val epNum = it.text()
            
            newEpisode(href) {
                this.name = epNum
            }
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = plot
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // Find server links or iframes
        val servers = document.select(".server-link, .video-item, ul.servers-list li")
        
        // Standard iframe extraction
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.startsWith("http")) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
