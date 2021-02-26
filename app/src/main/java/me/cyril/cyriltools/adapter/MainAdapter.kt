package me.cyril.cyriltools.adapter

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.navigation.Navigation
import me.cyril.cyriltools.R
import me.cyril.cyriltools.base.BaseAdapter
import me.cyril.cyriltools.ui.MainActivity

class MainAdapter(context: Context, data: ArrayList<String>, @LayoutRes resItemLayout: Int): BaseAdapter<String>(context, data, resItemLayout) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.getView<TextView>(R.id.feature_name)?.text = mData[position]
    }

    override fun onItemViewClick(itemView: View, position: Int) {
        if (mContext is MainActivity) {
            val action = when(position) {
                0 -> R.id.action_fragment_app_list
                1 -> R.id.action_fragment_level
                2 -> R.id.action_fragment_qr_code
                3 -> R.id.action_fragment_ruler
                4 -> R.id.action_fragment_convert
                5 -> R.id.action_fragment_misc
                else -> return
            }
            Navigation.findNavController(itemView).navigate(action)
        }
    }

}