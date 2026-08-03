package com.gamss.android.domain.safety

data class SupportAgency(
    val id: String,
    val name: String,
    val description: String,
    val phoneNumber: String?,
    val url: String?,
    val priority: Int,
)
