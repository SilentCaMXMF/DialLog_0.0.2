package com.example.diallog002

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    var isFavorite: Boolean = false
)

