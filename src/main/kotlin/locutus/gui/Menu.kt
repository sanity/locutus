package locutus.gui

import kweb.ElementCreator
import kweb.a
import kweb.div
import kweb.new
import kweb.plugins.fomanticUI.fomantic

class Menu {
    fun render(ec : ElementCreator<*>) {
        ec.apply {
            div(fomantic.ui.secondary.menu).new {
                a(fomantic.item).new {

                }
            }
        }
    }
}