package com.animhq

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.WebViewResolver
import org.jsoup.nodes.Document

class AnimhqProvider : MainAPI() {
    override var mainUrl = "https://animhq.com"
    override var name = "Animhq"
    override val hasMainPage = true
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val homePageList = ArrayList<HomePageList>()

        // Recent episodes or updates
        val sections = document.select("div.main-widget")
        sections.forEach { section ->
            val title = section.selectFirst("div.main-didget-head h3")?.text() ?: "أحدث الإضافات"
            val items = section.select("div.anime-card-container, div.episodes-card-container").mapNotNull {
                val a = it.selectFirst("a") ?: return@mapNotNull null
                val href = a.attr("href")
                val name = it.selectFirst("div.anime-card-title h3, div.ep-card-anime-title")?.text() ?: ""
                val poster = it.selectFirst("img")?.attr("src")
                
                newAnimeSearchResponse(name, href, TvType.Anime) {
                    this.posterUrl = poster
                }
            }
            if (items.isNotEmpty()) {
                homePageList.add(HomePageList(title, items))
            }
        }

        return newHomePageResponse(homePageList)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document

        return document.select("div.anime-card-container").mapNotNull {
            val a = it.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            val name = it.selectFirst("div.anime-card-title h3")?.text() ?: ""
            val poster = it.selectFirst("img")?.attr("src")

            newAnimeSearchResponse(name, href, TvType.Anime) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h1.anime-details-title")?.text() ?: ""
        val poster = document.selectFirst("div.anime-thumbnail img")?.attr("src")
        val plot = document.selectFirst("p.anime-story")?.text()
        
        val episodes = document.select("div.episodes-card-container").mapNotNull {
            val a = it.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            val epNum = it.selectFirst("div.episodes-card-title")?.text() ?: "1"
            
            newEpisode(href) {
                this.name = epNum
            }
        }.reversed()

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
        
        // Find iframes or server links
        val servers = document.select("ul.nav-tabs li a, div.video-item, .server-link")
        
        servers.forEach { server ->
            val serverId = server.attr("data-server-id") ?: server.attr("data-id")
            if (!serverId.isNullOrBlank()) {
                // This usually requires a POST request to get the actual link
                // For now, let's look for direct links in the HTML
            }
        }
        
        // Look for any iframe with src
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.startsWith("http")) {
                loadExtractor(src, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
