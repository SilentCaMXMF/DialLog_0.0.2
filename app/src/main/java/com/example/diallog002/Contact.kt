package com.example.diallog002

data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    var isFavorite: Boolean = false
)

// New data class for favorite phone numbers
data class FavoriteNumber(
    val phoneNumber: String,
    val contactName: String,
    val contactId: String
)

