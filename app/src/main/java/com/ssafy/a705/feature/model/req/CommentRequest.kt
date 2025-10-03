package com.ssafy.a705.feature.model.req

import com.google.gson.annotations.SerializedName

data class CommentRequest(
    val content: String,
    @SerializedName("parentId") val parentId: Long? = null
)