package me.cyril.cyriltools.base

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T>(context: Context, data: ArrayList<T>, @LayoutRes resItemLayout: Int) :
    RecyclerView.Adapter<BaseAdapter.ViewHolder>() {

    protected val mContext = context
    protected val mData = data

    private val mResItemLayout = resItemLayout

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(mContext).inflate(mResItemLayout, parent, false)
        val viewHolder = ViewHolder(itemView)
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemViewClick(itemView, position)
            }
        }
        return viewHolder
    }

    override fun getItemCount() = mData.size

    protected open fun onItemViewClick(itemView: View, position: Int) {}

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val mView = SparseArray<View>()

        @Suppress("UNCHECKED_CAST")
        fun <T : View> getView(@IdRes resId: Int): T? {
            var view = mView[resId]
            if (view == null) {
                view = itemView.findViewById(resId)
                mView.put(resId, view)
            }
            return view as? T
        }
    }

}