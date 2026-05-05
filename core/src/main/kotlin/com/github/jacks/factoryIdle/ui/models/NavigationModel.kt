package com.github.jacks.factoryIdle.ui.models

import com.badlogic.gdx.scenes.scene2d.ui.Table

class NavigationModel {

    private val views = mutableListOf<Table>()
    private val changeListeners = mutableListOf<(Table) -> Unit>()

    fun register(vararg newViews: Table) {
        views.addAll(newViews)
        newViews.forEach { it.isVisible = false }
    }

    fun show(view: Table) {
        views.forEach { it.isVisible = false }
        view.isVisible = true
        changeListeners.forEach { it(view) }
    }

    fun onActiveViewChange(listener: (Table) -> Unit) {
        changeListeners.add(listener)
    }
}
