package me.cyril.cyriltools.base

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import me.cyril.cyriltools.ui.MainActivity

abstract class BaseFragment : Fragment() {

    protected abstract val resLayout: Int
    protected abstract val resMenu: Int

    protected var mActivity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as? MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(resLayout, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (resMenu != -1) {
            inflater.inflate(resMenu, menu)
        }
    }

    protected fun <T : View> bindView(@IdRes idRes: Int): Lazy<T?> =
        lazy { view?.findViewById(idRes) }

}