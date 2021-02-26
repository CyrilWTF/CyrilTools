package me.cyril.cyriltools.ui

import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import me.cyril.cyriltools.R
import me.cyril.cyriltools.adapter.AppListAdapter
import me.cyril.cyriltools.base.BaseFragment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.Collator
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

class AppListFragment : BaseFragment(), CoroutineScope {

    override val resLayout: Int
        get() = R.layout.fragment_app_list
    override val resMenu: Int
        get() = R.menu.menu_app_list
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + SupervisorJob()

    private val mAppList = ArrayList<PackageInfo>()
    private val mAdapter by lazy {
        AppListAdapter(
            mActivity!!,
            mAppList,
            R.layout.list_item_app
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = mActivity ?: view.context
        val packageManager = context.packageManager
        val recyclerView = view.findViewById<RecyclerView>(R.id.app_list)
        launch(coroutineContext) {
            mAppList.addAll(packageManager.getInstalledPackages(0))
            mAppList.sortWith { o1, o2 ->
                val collator = Collator.getInstance(Locale.CHINA)
                val str1 = o1.applicationInfo.loadLabel(packageManager).toString()
                val str2 = o2.applicationInfo.loadLabel(packageManager).toString()
                collator.compare(str1, str2)
            }
            withContext(Dispatchers.Main) {
                val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
                progressBar.visibility = View.GONE
                recyclerView.adapter = mAdapter
            }
        }
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        }

        mActivity?.setToolbarTouchBehavior {
            val manager = recyclerView.layoutManager as? LinearLayoutManager
            manager?.run {
                if (findFirstCompletelyVisibleItemPosition() != 0) {
                    recyclerView.scrollToPosition(0)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        mActivity?.setToolbarTouchBehavior()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_all -> mAdapter.selectAll()
            R.id.menu_commit -> {
                val dir = mActivity?.getExternalFilesDir("APKBackup")
                try {
                    launch(coroutineContext) {
                        for (info in mAdapter.commit()) {
                            val path = info.applicationInfo.sourceDir
                            val name = info.packageName
                            val source = File(path)

                            if (source.exists()) {
                                withContext(Dispatchers.IO) {
                                    val inputStream = FileInputStream(source)
                                    val outputStream = FileOutputStream(File("$dir/$name.apk"))

                                    val buffer = ByteArray(1024)
                                    var byteRead: Int
                                    do {
                                        byteRead = inputStream.read(buffer)
                                        if (byteRead == -1) {
                                            break
                                        }
                                        outputStream.write(buffer, 0, byteRead)
                                    } while (true)

                                    with(outputStream) {
                                        flush()
                                        close()
                                    }
                                    inputStream.close()
                                }
                            }
                        }
                        withContext(Dispatchers.Main) { mActivity?.snack("Task complete.") }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

}
