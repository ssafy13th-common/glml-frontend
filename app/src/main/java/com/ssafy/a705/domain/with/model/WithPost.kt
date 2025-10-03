package com.ssafy.a705.domain.with.model


data class WithPost(
    val id: Long,
    val title: String,
    val content: String,
    val author: String,
    val authorEmail: String,
    val authorProfileUrl: String?,
    val date: String,
    val commentCount: Int
)
