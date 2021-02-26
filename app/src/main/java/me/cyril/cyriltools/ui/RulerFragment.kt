package me.cyril.cyriltools.ui

import android.view.MenuItem
import me.cyril.cyriltools.R
import me.cyril.cyriltools.base.BaseFragment

class RulerFragment : BaseFragment() {

    override val resLayout: Int
        get() = R.layout.fragment_ruler
    override val resMenu: Int
        get() = R.menu.menu_ruler

    private val mRuler by bindView<RulerView>(R.id.ruler)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_reset -> mRuler?.reset()
        }
        return true
    }

}
