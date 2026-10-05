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
    val contentMap = map["content"].asRichMap() ?: emptyMap()

    return FeedPost(
        id = id,
        author = parseAuthor(map["author"]),
        text = map["text"]?.toString(),
        poll = parsePoll(map["poll"]),
        content = PostContent(
            images = parseImages(contentMap),
            videos = parseVideos(contentMap),
            files = parseFiles(contentMap),
            filesCount = (contentMap["files"] as? List<*>)?.size ?: 0,
            videosCount = (contentMap["videos"] as? List<*>)?.size ?: 0,
            songs = parseSongs(contentMap)
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
    val resultsMap = map["results"].asRichMap() ?: map.asRichMap() ?: return null

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
        val contentMap = map["content"].asRichMap() ?: emptyMap()
        val replyMap = contentMap["reply"].asRichMap()

        PostComment(
            id = id,
            postId = postId,
            author = parseAuthor(map["author"]),
            text = map["text"]?.toString().orEmpty(),
            content = PostCommentContent(
                reply = replyMap?.let {
                    PostCommentReply(
                        commentId = it["comment_id"].asInt(),
                        author = parseAuthor(it["author"]),
                        text = it["text"]?.toString().orEmpty()
                    )
                },
                images = parseImages(contentMap),
                filesCount = (contentMap["files"] as? List<*>)?.size ?: 0
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
            item.asMap()?.get("icon_id")?.toString() ?: item?.toString()
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

internal fun parseAsset(raw: Any?): PostImageAsset? {
    val baseMap = raw.asRichMap() ?: return null

    // Ищем file_id во всех возможных местах (Neo)
    val fileId = baseMap["file_id"].asInt()
        ?: baseMap["fileId"].asInt()
        ?: baseMap["id"].asInt()

    // Fallback: path/file из img_data/image/asset
    val map = baseMap["img_data"].asRichMap()
        ?: baseMap["image"].asRichMap()
        ?: baseMap["asset"].asRichMap()
        ?: baseMap

    val path = map["path"]?.toString()?.takeIf { it.isNotBlank() }
        ?: baseMap["path"]?.toString()?.takeIf { it.isNotBlank() }
        ?: ""
    val file = map["file"]?.toString()?.takeIf { it.isNotBlank() }
        ?: baseMap["file"]?.toString()?.takeIf { it.isNotBlank() }
        ?: ""

    if (fileId == null && path.isBlank() && file.isBlank()) return null

    return PostImageAsset(
        fileId = fileId,
        path = path,
        file = file,
        simple = map["simple"]?.toString() ?: baseMap["simple"]?.toString(),
        aura = map["aura"]?.toString() ?: baseMap["aura"]?.toString(),
        preview = map["preview"]?.toString() ?: baseMap["preview"]?.toString()
    )
}

private fun parseImages(contentMap: Map<String, Any?>): List<PostImage> {
    val images = (contentMap["images"] as? List<*>)
        .orEmpty()
        .mapNotNull { parseImage(it) }
        .toMutableList()

    val legacy = contentMap["Image"]?.let { parseImage(it) }
    if (legacy != null) {
        images.add(legacy)
    }

    return images
}

private fun parseImage(raw: Any?): PostImage? {
    val map = raw.asRichMap() ?: return null
    val fileName = map["orig_name"]?.toString() ?: map["file_name"]?.toString()
    val fileSize = map["file_size"].asLong()

    val asset = parseAsset(map)
    val resolvedAsset = asset ?: fileName
        ?.takeIf { it.isNotBlank() }
        ?.let { legacyName ->
            PostImageAsset(
                path = "posts/images",
                file = legacyName
            )
        }

    if (resolvedAsset == null && fileName.isNullOrBlank()) {
        return null
    }

    return PostImage(
        asset = resolvedAsset,
        fileName = fileName,
        fileSize = fileSize
    )
}

private fun parseVideos(contentMap: Map<String, Any?>): List<PostVideo> {
    return (contentMap["videos"] as? List<*>)
        .orEmpty()
        .mapNotNull { parseVideo(it) }
}

private fun parseFiles(contentMap: Map<String, Any?>): List<PostFile> {
    return (contentMap["files"] as? List<*>)
        .orEmpty()
        .mapNotNull { parseFile(it) }
}

private fun parseFile(raw: Any?): PostFile? {
    val map = raw.asRichMap() ?: return null

    val id = map["id"].asInt() ?: 0
    val fileId = map["file_id"].asInt()
        ?: map["fileId"].asInt()
        ?: map["storage_id"].asInt()
        ?: map["storageId"].asInt()

    val file = map["file"]?.toString()?.takeIf { it.isNotBlank() }
        ?: map["name"]?.toString()?.takeIf { it.isNotBlank() }
        ?: ""
    val name = map["name"]?.toString()
        ?: map["file_name"]?.toString()
        ?: map["orig_name"]?.toString()
        ?: file
    val size = map["size"].asLong()
        ?: map["file_size"].asLong()
        ?: 0L
    val mimeType = map["type"]?.toString()
        ?: map["mime_type"]?.toString()
    val path = map["path"]?.toString()?.takeIf { it.isNotBlank() }
        ?: "posts/files"

    if (fileId == null && file.isBlank() && name.isBlank()) return null

    return PostFile(
        id = id,
        fileId = fileId,
        name = name,
        size = size,
        mimeType = mimeType,
        path = path,
        file = file
    )
}

private fun parseSongs(contentMap: Map<String, Any?>): List<PostSong> {
    return (contentMap["songs"] as? List<*>)
        .orEmpty()
        .mapNotNull { parseSong(it) }
}

private fun parseSong(raw: Any?): PostSong? {
    val map = raw.asRichMap() ?: return null
    val id = map["id"].asInt() ?: return null
    val title = map["title"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
    val artist = map["artist"]?.toString()?.takeIf { it.isNotBlank() } ?: return null

    return PostSong(
        id = id,
        title = title,
        artist = artist,
        album = map["album"]?.toString(),
        cover = parseAsset(map["cover"]),
        durationSeconds = map["duration"].asDouble()
    )
}

private fun parseVideo(raw: Any?): PostVideo? {
    val map = raw.asRichMap() ?: return null
    val fileId = map["file_id"].asInt()
        ?: map["fileId"].asInt()
        ?: map["id"].asInt()
    val file = map["file"]?.toString()?.takeIf { it.isNotBlank() } ?: ""
    if (fileId == null && file.isBlank()) return null

    val preview = parseAsset(map["preview"])
    val info = map["info"].asRichMap()?.let {
        PostVideoInfo(
            width = it["width"].asInt(),
            height = it["height"].asInt()
        )
    }

    return PostVideo(
        file = file,
        fileId = fileId,
        fileName = map["name"]?.toString() ?: map["file_name"]?.toString(),
        fileSize = map["size"].asLong() ?: map["file_size"].asLong(),
        preview = preview,
        info = info
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
