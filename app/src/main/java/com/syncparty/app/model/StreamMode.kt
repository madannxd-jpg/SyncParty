package com.syncparty.app.model

enum class StreamMode(val displayName: String, val description: String) {
    SCREEN_SHARE("Screen Share", "Stream full phone screen with games, apps & audio"),
    YOUTUBE("YouTube Watch Party", "Synchronized YouTube videos & playlists"),
    WEB_BROWSER("Web Co-Watch", "Co-browse Instagram, Crunchyroll & Anime portals"),
    DIRECT_VIDEO("Direct Video Stream", "Play direct MP4 / HLS (.m3u8) video links")
}
