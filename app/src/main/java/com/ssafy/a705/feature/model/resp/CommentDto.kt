package com.ssafy.a705.feature.model.resp

import com.google.gson.annotations.SerializedName

data class CommentDto(
    val id: Long,
    val content: String,
    val author: String,
    val authorEmail: String,
    val authorProfileUrl: String?,
    @SerializedName("updatedDate") val updatedDate: String? = null,
    @SerializedName("createdDate") val createdDate: String? = null,
    val parentComment: Long?,
    val timestamp: String
)