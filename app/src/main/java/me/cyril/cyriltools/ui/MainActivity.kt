package me.cyril.cyriltools.ui

import android.os.Bundle
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import me.cyril.cyriltools.R

class MainActivity : AppCompatActivity() {

    private val navController by lazy {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHostFragment.findNavController()
    }
    private var coordinatorLayout: CoordinatorLayout? = null
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        coordinatorLayout = findViewById(R.id.main_container)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController)
    }

    override fun onDestroy() {
        super.onDestroy()
        coordinatorLayout = null
        toolbar = null
    }

    override fun onNavigateUp() = navController.navigateUp()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    fun snack(message: String) {
        coordinatorLayout?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }

    @Suppress("ClickableViewAccessibility")
    fun setToolbarTouchBehavior(run: () -> Unit = {}) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                run()
                return super.onDoubleTap(e)
            }
        })
        toolbar?.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
    }

}
