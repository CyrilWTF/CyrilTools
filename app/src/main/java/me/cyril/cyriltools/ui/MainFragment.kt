package me.cyril.cyriltools.ui

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.cyril.cyriltools.*
import me.cyril.cyriltools.adapter.MainAdapter
import me.cyril.cyriltools.base.BaseFragment

class MainFragment : BaseFragment() {

    override val resLayout: Int
        get() = R.layout.fragment_main
    override val resMenu: Int
        get() = NO_MENU

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = mActivity ?: view.context
        val recyclerView = view.findViewById<RecyclerView>(R.id.list_main)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = MainAdapter(
                context,
                arrayListOf(
                    FEATURE_PICKER,
                    FEATURE_LEVEL,
                    FEATURE_QR_CODE,
                    FEATURE_RULER,
                    FEATURE_TEXT_CONVERT,
                    FEATURE_MISCELLANEOUS
                ),
                R.layout.list_item_main
            )
        }
    }

}
