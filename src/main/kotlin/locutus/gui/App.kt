package locutus.gui

import kweb.Kweb
import kweb.plugins.fomanticUI.fomanticUIPlugin

fun main() {
    Kweb(port = 3784, plugins = listOf(fomanticUIPlugin)) {

    }
}