package com.choice.app.domain

fun matchesSearchQuery(coinName: String, query: String): Boolean {
    if (query.isBlank()) return true
    return coinName.contains(query, ignoreCase = true)
}
