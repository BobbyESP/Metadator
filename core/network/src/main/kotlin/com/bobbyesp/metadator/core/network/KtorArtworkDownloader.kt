package com.bobbyesp.metadator.core.network

import com.bobbyesp.metadator.lookup.api.ArtworkDownloader
import com.bobbyesp.metadator.lookup.api.DownloadedImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

class KtorArtworkDownloader(private val client: HttpClient) : ArtworkDownloader {
    override suspend fun download(url: String): DownloadedImage? =
        networkCall(onError = { null }) {
            val response = client.get(url)
            if (!response.status.isSuccess()) return@networkCall null
            val bytes = response.bodyAsBytes()
            if (bytes.size > MAX_BYTES) return@networkCall null
            val mime =
                response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()
                    ?: sniffMimeType(bytes)
                    ?: return@networkCall null
            if (!mime.startsWith("image/")) return@networkCall null
            DownloadedImage(bytes, mime)
        }

    private companion object {
        /** Covers above this are not covers. */
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}

/** JPEG or PNG by their magic numbers, for servers that do not say. */
fun sniffMimeType(bytes: ByteArray): String? =
    when {
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte() -> "image/jpeg"
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() &&
            bytes[3] == 'G'.code.toByte() -> "image/png"
        bytes.size >= 12 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
        else -> null
    }
