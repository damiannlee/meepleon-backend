package com.meepleday.common

/**
 * Escapes SQL LIKE wildcards in raw user input before wrapping it as a `%...%` contains-pattern,
 * so a search term like `100%` or `a_b` doesn't act as a wildcard instead of a literal match.
 */
fun toContainsLikePattern(raw: String): String {
    val escaped = raw
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return "%$escaped%"
}
