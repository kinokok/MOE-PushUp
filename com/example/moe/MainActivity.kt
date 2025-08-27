package com.example.moe

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.moe.databinding.ActivityMainBinding
import com.example.moe.ui.memory.MemoryCalendarFragment
import com.example.moe.ui.play.PlayFragment
import com.example.moe.ui.play.PlayFragmentListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.moe.ui.theme.StartDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.moe.ui.theme.HowDialog
import com.example.moe.ui.option.Option

class MainActivity : AppCompatActivity(), PlayFragmentListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var howText: TextView
    private lateinit var coinText: TextView
    private lateinit var coinLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // グローバル変数の値をロード
        Option.loadOptions(this)

        floatingActionButton = findViewById(R.id.play_button)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)

        howText = findViewById(R.id.how_text)
        coinText = findViewById(R.id.coin_text)
        coinLayout = findViewById(R.id.coin_Layout)

        bottomNavigationView.itemIconTintList = null

        floatingActionButton.setOnClickListener {
            val startDialog = StartDialogFragment()
            startDialog.showNow(supportFragmentManager, "custom")
        }

        // how_button
        val howButton: ImageButton = findViewById(R.id.how_button)
        howButton.setOnClickListener {
            val dialog = HowDialog()
            dialog.showNow(supportFragmentManager, "custom")
        }

        val navController = findNavController(R.id.host_fragment_activity_main)
        bottomNavigationView.setupWithNavController(navController)


        // グラデーションの向きを設定
        val gradationOrientation = GradientDrawable.Orientation.RIGHT_LEFT
        // グラデーションの色を作成
        val c1 = Color.parseColor("#000000")
        val c2 = Color.parseColor("#0083FF")
        val colors: IntArray = intArrayOf(c1, c2)
        val gradientDrawable = GradientDrawable(gradationOrientation, colors)
        window?.setBackgroundDrawable(gradientDrawable)


// フラグメントのライフサイクルを監視してBottomNavigationViewとFloatingActionButtonを制御する
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                super.onFragmentStarted(fm, f)
                when (f) {
                    is PlayFragment -> {
                        setBottomNavigationViewEnabled(bottomNavigationView, false)
                        updateFloatingActionButtonForPlayFragment()
                    }
                    is MemoryCalendarFragment -> {
                        setBottomNavigationViewEnabled(bottomNavigationView, false)
                        howButton.visibility = View.INVISIBLE
                        howText.visibility = View.INVISIBLE
                        coinLayout.visibility = View.INVISIBLE
                        floatingActionButton.visibility = View.INVISIBLE
                    }
                    else -> {
                        setBottomNavigationViewEnabled(bottomNavigationView, true)
                        resetFloatingActionButton()
                        howButton.visibility = View.VISIBLE
                        howText.visibility = View.VISIBLE
                        coinLayout.visibility = View.VISIBLE
                        floatingActionButton.visibility = View.VISIBLE

                    }
                }
            }
        }, true)


        // バックボタンを無効にする
        onBackPressedDispatcher.addCallback(this) {
            // ここに空のコールバックを追加
        }

        updateCoinText()
    }

    override fun onPause() {
        super.onPause()

        // グローバル変数の値を保存
        Option.saveOptions(this)
    }

    private fun setBottomNavigationViewEnabled(bottomNavigationView: BottomNavigationView, enabled: Boolean) {
        for (i in 0 until bottomNavigationView.menu.size()) {
            bottomNavigationView.menu.getItem(i).isEnabled = enabled
        }
    }

    private fun resetFloatingActionButton() {
        floatingActionButton.setImageResource(R.drawable.ic_play_start)  // デフォルトの画像リソースを指定
        floatingActionButton.setOnClickListener {
            val startDialog = StartDialogFragment()
            startDialog.showNow(supportFragmentManager, "custom")
        }
    }

    private fun updateFloatingActionButtonForPlayFragment() {
        floatingActionButton.setImageResource(R.drawable.ic_play_stop)  // 変更したい画像リソースを指定
        floatingActionButton.setOnClickListener {
            finishPlayFragment()
        }
    }

    override fun finishPlayFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.host_fragment_activity_main)
        if (fragment is PlayFragment) {
            fragment.finishPlayFragment("NG")

            updateCoinText()
        }
    }

    private fun updateCoinText() {
        coinText.text = Option.COIN.toString()
    }
}
