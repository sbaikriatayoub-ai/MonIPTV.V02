package com.monapp.iptv

data class UserInfo(
    val username: String?,
    val status: String?,
    val exp_date: String?,
    val max_connections: String?
)

data class LoginResponse(val user_info: UserInfo?)
data class Category(val category_id: String?, val category_name: String?)
data class Live(val stream_id: Int?, val name: String?, val stream_icon: String?, val category_id: String?)
data class Movie(
    val stream_id: Int?,
    val name: String?,
    val stream_icon: String?,
    val container_extension: String?,
    val category_id: String?,
    val added: String?
)
data class Series(
    val series_id: Int?,
    val name: String?,
    val cover: String?,
    val category_id: String?,
    val last_modified: String?
)

enum class Section(val label: String) {
    LIVE("TV live"),
    FILMS("Films"),
    SERIES("Séries")
}

enum class Dest(val label: String) {
    HOME("Accueil"),
    LIVE("TV live"),
    FILMS("Films"),
    SERIES("Séries"),
    FAVS("Favoris"),
    SETTINGS("Paramètres")
}

enum class DisplayMode(val label: String, val ratio: Float?) {
    FULL("Plein écran", null),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_4("3:4", 3f / 4f);

    companion object {
        fun fromKey(key: String?): DisplayMode =
            entries.firstOrNull { it.name == key } ?: FULL
    }
}

data class Profile(
    val id: String,
    val name: String,
    val accentHex: Long = 0xFFF5A524
)

data class Item(
    val section: Section,
    val id: Int,
    val title: String,
    val image: String?,
    val ext: String?,
    val categoryId: String?,
    val added: Long = 0L
)

data class Meta(
    val plot: String?,
    val genre: String?,
    val year: String?,
    val rating: String?,
    val duration: String?,
    val cast: String?
)

data class Ep(
    val id: String,
    val title: String,
    val ext: String?,
    val num: Int?,
    val image: String?,
    val duration: String?
)

data class SectionData(val categories: List<Category>, val items: List<Item>)
