package me.cyril.cyriltools.adapter

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.util.forEach
import me.cyril.cyriltools.R
import me.cyril.cyriltools.base.BaseAdapter

class AppListAdapter(
    context: Context,
    data: ArrayList<PackageInfo>, @LayoutRes resItemLayout: Int
) : BaseAdapter<PackageInfo>(context, data, resItemLayout) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_NORMAL = 1
    }

    private val mManager = mContext.packageManager
    private val mSelected = SparseBooleanArray()
    private val mDataBackup = ArrayList<PackageInfo>()
    private var isAllSelected = false

    init {
        for (i in 0 until itemCount) {
            mSelected.put(i, false)
        }
        // don't use foreach to remove element
        val iterator = mData.iterator()
        while (iterator.hasNext()) {
            val info = iterator.next()
            if (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                mDataBackup.add(info)
                iterator.remove()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val headerView =
                LayoutInflater.from(mContext).inflate(R.layout.list_header_app, parent, false)
            val viewHolder = ViewHolder(headerView)
            viewHolder.getView<SwitchCompat>(R.id.switch_system_app)?.setOnCheckedChangeListener { _, _ ->
                swapDataSet()
                notifyDataSetChanged()
            }
            viewHolder
        } else {
            super.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position > 0) {
            val realPosition = position - 1
            holder.getView<ImageView>(R.id.app_icon)
                ?.setImageDrawable(mData[realPosition].applicationInfo.loadIcon(mManager))
            holder.getView<TextView>(R.id.app_label)?.text =
                mData[realPosition].applicationInfo.loadLabel(mManager)
            holder.getView<TextView>(R.id.app_pkg)?.text = mData[realPosition].packageName
            holder.itemView.isSelected = mSelected[realPosition]
        }
    }

    override fun getItemCount() = super.getItemCount() + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_NORMAL
    }

    override fun onItemViewClick(itemView: View, position: Int) {
        if (position > 0) {
            mSelected.put(position - 1, !mSelected[position - 1])
            notifyItemChanged(position)
        }
    }

    fun selectAll() {
        isAllSelected = !isAllSelected
        for (i in 0 until itemCount) {
            mSelected.put(i, isAllSelected)
        }
        notifyDataSetChanged()
    }

    fun commit(): HashSet<PackageInfo> {
        val selected = HashSet<PackageInfo>()
        mSelected.forEach { key, value ->
            if (value) {
                selected.add(mData[key])
            }
        }
        return selected
    }

    private fun swapDataSet() {
        val temp = ArrayList(mData)
        with(mData) {
            clear()
            addAll(mDataBackup)
        }
        with(mDataBackup) {
            clear()
            addAll(temp)
        }
        temp.clear()
        mSelected.clear()
        for (i in 0 until itemCount) {
            mSelected.put(i, false)
        }
    }

}