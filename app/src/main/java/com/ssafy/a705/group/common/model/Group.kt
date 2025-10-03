package com.ssafy.a705.group.common.model

import com.google.gson.annotations.SerializedName

data class Group(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("summary")
    val summary: String,
    @SerializedName("members")
    val members: List<String>
)