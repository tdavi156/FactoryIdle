package com.github.jacks.factoryIdle.ui

enum class Labels {
    HEADING, BODY, BODY_BOLD, SMALL, DIM;
    operator fun invoke() = name.lowercase()
}
