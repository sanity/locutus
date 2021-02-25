package locutus.gui

import kweb.Kweb
import kweb.a
import kweb.div
import kweb.new
import kweb.plugins.fomanticUI.fomantic
import kweb.plugins.fomanticUI.fomanticUIPlugin
import javax.management.Query.div

fun main() {
    Kweb(port = 3784, plugins = listOf(fomanticUIPlugin)) {
        doc.body.new {

        }
    }
}
