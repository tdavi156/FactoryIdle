package com.github.jacks.factoryIdle.ui

enum class Buttons {
    DEFAULT, ACCENT, DANGER, NAVIGATION;
    operator fun invoke() = name.lowercase()
}
