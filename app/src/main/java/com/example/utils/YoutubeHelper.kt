package com.example.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object YoutubeHelper {

    fun extractVideoId(url: String): String {
        val clean = url.trim()
        if (clean.length == 11 && !clean.contains("/") && !clean.contains(".")) {
            return clean
        }
        val regex = "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?v=|embed\\/|v\\/|shorts\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})".toRegex()
        val match = regex.find(clean)
        return match?.groupValues?.getOrNull(1) ?: ""
    }

    fun getThumbnailUrl(videoId: String): String {
        return if (videoId.isNotBlank()) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        } else {
            ""
        }
    }

    fun launchYoutubeVideo(context: Context, videoId: String, fullUrl: String) {
        if (videoId.isNotBlank()) {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
            try {
                context.startActivity(appIntent)
            } catch (e: ActivityNotFoundException) {
                context.startActivity(webIntent)
            }
        } else if (fullUrl.isNotBlank()) {
            val url = if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://")) {
                "https://$fullUrl"
            } else {
                fullUrl
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }
}
