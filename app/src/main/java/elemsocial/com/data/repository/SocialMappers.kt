package elemsocial.com.data.repository

import elemsocial.com.domain.model.FeedPost
import elemsocial.com.domain.model.PostAuthor
import elemsocial.com.domain.model.PostComment
import elemsocial.com.domain.model.PostCommentContent
import elemsocial.com.domain.model.PostCommentReply
import elemsocial.com.domain.model.PostContent
import elemsocial.com.domain.model.PostFile
import elemsocial.com.domain.model.PostImage
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PostPollOption
import elemsocial.com.domain.model.PostReactions
import elemsocial.com.domain.model.PostSong
import elemsocial.com.domain.model.PostVideo
import elemsocial.com.domain.model.PostVideoInfo
import org.json.JSONObject

internal fun parseFeedPosts(raw: Any?): List<FeedPost> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull { parseFeedPost(it) }
}

internal fun parseFeedPost(raw: Any?): FeedPost? {
    val map = raw.asMap() ?: return null
    val id = map["id"].asInt() ?: return null

    // content — МАССИВ блоков:
    // [ {type:"images", items:[...]}, {type:"videos", items:[...]}, {type:"tracks", items:[...]} ]
    val contentBlocks = (map["content"] as? List<*>).orEmpty()

    val images = mutableListOf<PostImage>()
    val videos = mutableListOf<PostVideo>()
    val files = mutableListOf<PostFile>()
    val songs = mutableListOf<PostSong>()

    contentBlocks.forEach { blockRaw ->
        val block = blockRaw.asRichMap() ?: return@forEach
        val type = block["type"]?.toString()?.lowercase() ?: return@forEach
        val items = (block["items"] as? List<*>).orEmpty()

        when (type) {
            "images" -> items.forEach { itemRaw ->
                val item = itemRaw.asRichMap() ?: return@forEach
                // ассет лежит под ключом "image"
                val asset = parseAsset(item["image"])
                val fileName = item["file_name"]?.toString()
                val fileSize = item["file_size"].asLong()
                if (asset != null || fileName != null) {
                    images.add(
                        PostImage(
                            asset = asset,
                            fileName = fileName,
                            fileSize = fileSize
                        )
                    )
                }
            }
            "videos" -> items.forEach { itemRaw ->
                val item = itemRaw.asRichMap() ?: return@forEach
                // ассет лежит под ключом "video"
                val videoMap = item["video"].asRichMap()
                val videoId = videoMap?.get("file_id").asInt()
                val preview = parseAsset(videoMap?.get("preview"))
                val fileName = item["file_name"]?.toString()
                val fileSize = item["file_size"].asLong()
                if (videoId != null && videoId > 0) {
                    videos.add(
                        PostVideo(
                            file = "",
                            fileId = videoId,
                            fileName = fileName,
                            fileSize = fileSize,
                            preview = preview,
                            info = null
                        )
                    )
                }
            }
            "files" -> items.forEach { itemRaw ->
                val item = itemRaw.asRichMap() ?: return@forEach
                // ассет лежит под ключом "file"
                val fileMap = item["file"].asRichMap() ?: item
                val fileId = fileMap["id"].asInt()
                    ?: fileMap["file_id"].asInt()
                val fileName = item["file_name"]?.toString()
                    ?: fileMap["name"]?.toString()
                val fileSize = item["file_size"].asLong()
                    ?: fileMap["size"].asLong()
                if (fileId != null && fileId > 0) {
                    files.add(
                        PostFile(
                            id = fileId,
                            fileId = fileId,
                            name = fileName ?: "file",
                            size = fileSize ?: 0L,
                            mimeType = fileMap["mime"]?.toString(),
                            path = "",
                            file = ""
                        )
                    )
                }
            }
            "tracks" -> items.forEach { itemRaw ->
                val item = itemRaw.asRichMap() ?: return@forEach
                val songId = item["id"].asInt() ?: return@forEach
                songs.add(
                    PostSong(
                        id = songId,
                        title = "",
                        artist = ""
                    )
                )
            }
        }
    }

    return FeedPost(
        id = id,
        author = parseAuthor(map["author"]),
        text = map["text"]?.toString(),
        poll = parsePoll(map["poll"]),
        content = PostContent(
            images = images,
            videos = videos,
            files = files,
            filesCount = files.size,
            videosCount = videos.size,
            songs = songs
        ),
        createDate = map["create_date"]?.toString(),
        editedAt = map["edited_at"]?.toString(),
        likes = map["likes"].asInt(0) ?: 0,
        dislikes = map["dislikes"].asInt(0) ?: 0,
        comments = map["comments"].asInt(0) ?: 0,
        liked = map["liked"].asBoolean(),
        disliked = map["disliked"].asBoolean(),
        myPost = map["my_post"].asBoolean(),
        deleted = map["deleted"].asBoolean(),
        archived = map["archived"].asBoolean(),
        reactions = parseReactions(map["reactions"])
    )
}

internal fun parseReactions(raw: Any?): PostReactions? {
    val map = raw.asRichMap() ?: return null
    val resultsMap = map["results"].asRichMap() ?: return null

    val results = resultsMap.mapNotNull { (key, value) ->
        val count = value.asInt() ?: return@mapNotNull null
        if (count <= 0) return@mapNotNull null
        key to count
    }.toMap()

    val userReactions = (map["user_reactions"] as? List<*>)
        .orEmpty()
        .mapNotNull { item ->
            when (item) {
                is String -> item.takeIf { it.isNotBlank() }
                else -> item.asRichMap()?.get("reaction")?.toString()?.takeIf { it.isNotBlank() }
            }
        }

    if (results.isEmpty() && userReactions.isEmpty()) return null

    return PostReactions(
        results = results,
        userReactions = userReactions
    )
}

internal fun parsePoll(raw: Any?): PostPoll? {
    val map = raw.asRichMap() ?: return null
    val id = map["id"].asInt() ?: 0
    val options = (map["options"] as? List<*>)
        .orEmpty()
        .mapNotNull { parsePollOption(it) }

    return PostPoll(
        id = id,
        question = map["question"]?.toString().orEmpty(),
        isAnonymous = map["is_anonymous"].asBoolean(),
        multipleChoice = map["multiple_choice"].asBoolean(),
        expiresAt = map["expires_at"]?.toString(),
        totalVotes = map["total_votes"].asInt(0) ?: 0,
        userVote = parseIntList(map["user_vote"]),
        options = options
    )
}

private fun parsePollOption(raw: Any?): PostPollOption? {
    val map = raw.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val text = map["text"]?.toString()?.takeIf { it.isNotBlank() } ?: return null

    return PostPollOption(
        id = id,
        text = text,
        votesCount = map["votes_count"].asInt(0) ?: 0
    )
}

private fun parseIntList(raw: Any?): List<Int> {
    return (raw as? List<*>)
        .orEmpty()
        .mapNotNull { it.asInt() }
}

internal fun parseComments(raw: Any?): List<PostComment> {
    val list = raw as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        val map = item.asMap() ?: return@mapNotNull null
        val id = map["id"].asInt() ?: return@mapNotNull null
        val postId = map["post_id"].asInt(0) ?: 0

        PostComment(
            id = id,
            postId = postId,
            author = parseAuthor(map["author"]),
            text = map["text"]?.toString().orEmpty(),
            content = PostCommentContent(
                reply = null,
                images = emptyList(),
                filesCount = 0
            ),
            date = map["date"]?.toString(),
            deleted = map["deleted"].asBoolean()
        )
    }
}

internal fun parseAuthor(raw: Any?): PostAuthor? {
    val map = raw.asMap() ?: return null
    val icons = when (val source = map["icons"]) {
        is List<*> -> source.mapNotNull { item ->
            item.asRichMap()?.get("icon_id")?.toString() ?: item?.toString()
        }
        is String -> listOf(source)
        else -> emptyList()
    }

    return PostAuthor(
        id = map["id"].asInt(),
        type = map["type"].asInt(),
        username = map["username"]?.toString(),
        name = map["name"]?.toString(),
        avatar = parseAsset(map["avatar"]),
        icons = icons,
        blocked = map["blocked"].asBoolean(),
        deleted = map["deleted"].asBoolean()
    )
}

/**
 * Парсит ассет (avatar, image, video, cover).
 * Ожидает объект вида:
 *   { file_id: 123, width: 1080, height: 1080, dominant_color: "...", blur_hash: "..." }
 * либо legacy:
 *   { img_data: { path, file, simple, aura, preview } }
 */
internal fun parseAsset(raw: Any?): PostImageAsset? {
    if (raw == null) return null

    // Если пришла строка (старый API) — считаем её file name
    if (raw is String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "None") return null
        return PostImageAsset(
            fileId = null,
            path = "/Content/Avatars",
            file = trimmed
        )
    }

    val baseMap = raw.asRichMap() ?: return null

    val fileId = baseMap["file_id"].asInt()
        ?: baseMap["fileId"].asInt()
        ?: baseMap["id"].asInt()

    val inner = baseMap["img_data"].asRichMap()
        ?: baseMap["image"].asRichMap()
        ?: baseMap

    val path = inner["path"]?.toString()?.takeIf { it.isNotBlank() } ?: ""
    val file = inner["file"]?.toString()?.takeIf { it.isNotBlank() } ?: ""

    if ((fileId == null || fileId <= 0) && (path.isBlank() || file.isBlank())) {
        return null
    }

    return PostImageAsset(
        fileId = fileId,
        path = path,
        file = file,
        simple = inner["simple"]?.toString() ?: baseMap["simple"]?.toString(),
        aura = inner["aura"]?.toString() ?: baseMap["aura"]?.toString(),
        preview = inner["preview"]?.toString() ?: baseMap["preview"]?.toString()
    )
}

internal fun Any?.asMap(): Map<String, Any?>? {
    val source = this as? Map<*, *> ?: return null
    return source.entries.associate { (k, v) -> k.toString() to v }
}

internal fun Any?.asJsonObjectMap(): Map<String, Any?>? {
    val text = this as? String ?: return null
    if (!text.trim().startsWith("{")) return null
    return runCatching {
        val json = JSONObject(text)
        json.keys().asSequence().associateWith { key -> json.opt(key) as Any? }
    }.getOrNull()
}

internal fun Any?.asRichMap(): Map<String, Any?>? {
    return asMap() ?: asJsonObjectMap()
}

internal fun Any?.asInt(default: Int? = null): Int? {
    return when (this) {
        is Number -> toInt()
        is String -> toIntOrNull() ?: default
        else -> default
    }
}

internal fun Any?.asLong(default: Long? = null): Long? {
    return when (this) {
        is Number -> toLong()
        is String -> toLongOrNull() ?: default
        else -> default
    }
}

internal fun Any?.asDouble(default: Double? = null): Double? {
    return when (this) {
        is Number -> toDouble()
        is String -> toDoubleOrNull() ?: default
        else -> default
    }
}

internal fun Any?.asBoolean(): Boolean {
    return when (this) {
        is Boolean -> this
        is Number -> toInt() != 0
        is String -> this.equals("true", ignoreCase = true) || this == "1"
        else -> false
    }
}
