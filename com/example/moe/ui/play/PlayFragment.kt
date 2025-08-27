package com.example.moe.ui.play

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.*
import androidx.core.animation.AnimatorInflater
import androidx.fragment.app.Fragment
import com.example.moe.R
import com.example.moe.databinding.FragmentPlayBinding
import com.example.moe.ui.option.Option
import com.example.moe.ui.theme.ResultDialog
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random


interface PlayFragmentListener {
    fun finishPlayFragment()
}

class PlayFragment : Fragment() {

    private lateinit var binding: FragmentPlayBinding
    private lateinit var doubleTapText: TextView
    private lateinit var startButton: ImageButton
    private lateinit var tapCountTextView: TextView
    private lateinit var timerTextView: TextView

    private var lastUserInteractionTime: Long = 0L
    private val tapTimeoutMillis: Long = 10000L
    private var lastLimitMessageTime: Long = 0L
    private val limitMessageInterval: Long = 10000L

    private lateinit var wallImage: ImageView

    private lateinit var deTapCountTextView: TextView
    private lateinit var deTimerTextView: TextView

    private var tapCount: Int = 0
    private var repLimit: Int = 0
    private var timeLimit: Int = 0
    private var timeCount: String = "00:00"

    private val maxLines = 5
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastTapTime: Long = 0
    private val tapInterval: Long = 500

    private var startTime: Long = 0L
    private var listener: PlayFragmentListener? = null

    private lateinit var dokiImage: ImageView
    private lateinit var dokiImage2: ImageView
    private lateinit var dokiImage3: ImageView
    private lateinit var dokiImage4: ImageView
    private lateinit var kyunImage: ImageView
    private lateinit var flameImage: ImageView
    private var isDokiVisible = false


    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var proximitySensorEventListener: SensorEventListener? = null

    private var isInfiniteMode: Boolean = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PlayFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement PlayFragmentListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPlayBinding.inflate(inflater, container, false)
        val view = binding.root

        val darkOverlay = view.findViewById<View>(R.id.dark_overlay)
        doubleTapText = view.findViewById(R.id.double_tap_text)
        startButton = view.findViewById(R.id.start_button)
        wallImage = view.findViewById(R.id.wall_image)

        // テーマ画像読み込み
        val picPlayPath = "${Option.THEME}/pic_play.png"
        try {
            val inputStream = requireContext().assets.open(picPlayPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            wallImage.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            wallImage.setImageResource(R.drawable.pic_play_error)
        }

        timeLimit = arguments?.getInt(ARG_TIME_LIMIT) ?: 0
        repLimit = arguments?.getInt(ARG_REP_LIMIT) ?: 0
        isInfiniteMode = (timeLimit == 0)

        timerTextView = view.findViewById(R.id.timer_text)
        tapCountTextView = view.findViewById(R.id.tap_count_text)
        tapCountTextView.text = if (isInfiniteMode) "0" else repLimit.toString()

        tapCount = if (isInfiniteMode) 0 else repLimit


        startButton.setOnClickListener {
            if (!isRunning) {
                startIdleTimeoutChecker()
                startTime = SystemClock.uptimeMillis()
                darkOverlay.visibility = View.GONE

                // 共通ランナブルの開始
                handler.post(timerRunnable)
                view.setOnTouchListener { _, event -> handleTouchEvent(event) }

                handler.post(runnable)
                isRunning = true
                startButton.visibility = View.INVISIBLE
            }
        }


        dokiImage = view.findViewById(R.id.dokiImage)
        dokiImage2 = view.findViewById(R.id.dokiImage2)
        dokiImage3 = view.findViewById(R.id.dokiImage3)
        dokiImage4 = view.findViewById(R.id.dokiImage4)
        kyunImage = view.findViewById(R.id.kyunImage)
        flameImage = view.findViewById(R.id.flameImage)

        // センサーの初期化
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

// オプションに応じてセンサーを有効化または無効化
        if (Option.SENSOR in 0..2) {
            toggleProximitySensor(Option.SENSOR != 0)
        }


        return view
    }

    private fun toggleProximitySensor(enable: Boolean) {
        if (enable && proximitySensor != null) {
            proximitySensorEventListener = object : SensorEventListener {

                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                        val distanceThreshold = when (Option.SENSOR) {
                            1 -> 2.0f // 2cm以内
                            2 -> 5.0f // 5cm以内
                            else -> Float.MAX_VALUE
                        }

                        if (event.values[0] < distanceThreshold) {
                            handleTouchEvent(null)
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // 必要に応じて処理を追加
                }
            }

            sensorManager.registerListener(
                proximitySensorEventListener,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else if (proximitySensorEventListener != null) {
            sensorManager.unregisterListener(proximitySensorEventListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragmentが破棄される際にセンサーを無効化
        toggleProximitySensor(false)
        handler.removeCallbacks(runnable)
    }

    //ドキ文字
    private var fadeOutHandler: Handler? = null

    private fun showImages() {
        // 古い postDelayed がある場合はキャンセル
        fadeOutHandler?.removeCallbacksAndMessages(null)
        fadeOutHandler = Handler(Looper.getMainLooper())

        if (!isRunning) return

        resetImages()
        val rootView = view ?: return
        val screenWidth = rootView.width
        val screenHeight = rootView.height

        val density = resources.displayMetrics.density
        val marginHorizontal = (50 * density).toInt() // 左右50dp
        val marginBottom = (250 * density).toInt()    // 下250dp

        // ランダムな座標を生成する関数
        fun generateRandomPosition(): Pair<Float, Float> {
            val randomX = (marginHorizontal..(screenWidth - marginHorizontal)).random()
            val randomY = (0..(screenHeight - marginBottom)).random()
            return Pair(randomX.toFloat(), randomY.toFloat())
        }

        // 位置が重ならないようにチェックする関数
        fun isOverlapping(pos1: Pair<Float, Float>, pos2: Pair<Float, Float>, imageSize: Int): Boolean {
            val dx = pos1.first - pos2.first
            val dy = pos1.second - pos2.second
            return Math.sqrt((dx * dx + dy * dy).toDouble()) < imageSize
        }

        // 位置設定のための関数
        fun setPosition(imageView: ImageView, position: Pair<Float, Float>) {
            imageView.x = position.first
            imageView.y = position.second
            imageView.visibility = View.VISIBLE
        }

        // 各画像の大きさを仮に取得
        val imageSize = 300
        val dokiImages = listOf(dokiImage, dokiImage2, dokiImage3, dokiImage4)

        // 各画像の位置を設定
        val positions = mutableListOf<Pair<Float, Float>>()
        for (i in dokiImages.indices) {
            var position: Pair<Float, Float>
            do {
                position = generateRandomPosition()
            } while (positions.any { isOverlapping(it, position, imageSize) })
            positions.add(position)
            setPosition(dokiImages[i], position)
        }

        // キュンの位置（重ならないようにチェック）
        var kyunPosition: Pair<Float, Float>
        do {
            kyunPosition = generateRandomPosition()
        } while (positions.any { isOverlapping(it, kyunPosition, imageSize) })

        setPosition(kyunImage, kyunPosition)

        // フェードアウトのランダムなディレイ時間（0.3秒〜1.0秒の間）
        val fadeOutDelays = listOf(
            (300..600).random().toLong(),
            (600..800).random().toLong(),
            (500..700).random().toLong(),
            (700..900).random().toLong(),
            (800..1000).random().toLong()
        )

        // dokiImages のフェードアウト処理
        dokiImages.forEachIndexed { index, imageView ->
            fadeOutHandler?.postDelayed({
                imageView.visibility = View.INVISIBLE
            }, fadeOutDelays[index])
        }

        // kyunImage のフェードアウト処理
        fadeOutHandler?.postDelayed({
            kyunImage.visibility = View.INVISIBLE
        }, fadeOutDelays.last())

        // flameImage の表示処理
        flameImage.visibility = View.VISIBLE

        // アニメーションを適用
        context?.let { ctx ->
            val flameAnimator = AnimatorInflater.loadAnimator(ctx, R.animator.flame_animation)
            flameAnimator.setTarget(flameImage)
            flameAnimator.start()
        }

        // 0.5秒後にflameImageを非表示にする
        fadeOutHandler?.postDelayed({
            flameImage.visibility = View.INVISIBLE
        }, 500)  // 0.5秒後に非表示
    }

    private fun resetImages() {
        // すべての画像を一括でリセット
        val allImages = listOf(dokiImage, dokiImage2, dokiImage3, dokiImage4, kyunImage, flameImage)
        allImages.forEach { imageView ->
            imageView.clearAnimation()  // アニメーションのクリア
            imageView.visibility = View.INVISIBLE
            imageView.alpha = 1f  // 透明度リセット
        }

        fadeOutHandler?.removeCallbacksAndMessages(null)
        isDokiVisible = false
    }


    private val runnable: Runnable = Runnable {
    }

    private fun updateTapCount() {
        tapCountTextView.text = tapCount.toString()
    }

    //json読み取り
    private fun loadFromJson(keys: List<String>): Map<String, Any?> {
        val themeFolderName = Option.THEME
        val readmePath = "$themeFolderName/readme.json"
        val inputStream = requireContext().assets.open(readmePath)
        val jsonStr = inputStream.bufferedReader().use { it.readText() }
        inputStream.close()

        val jsonObject = JSONObject(jsonStr)
        val result = mutableMapOf<String, Any?>()

        for (key in keys) {
            if (jsonObject.has(key)) {
                when (val value = jsonObject.get(key)) {
                    is org.json.JSONArray -> {
                        val list = List(value.length()) { index -> value.getString(index) }
                        result[key] = list
                    }
                    else -> result[key] = value
                }
            } else {
                result[key] = null
            }
        }

        return result
    }

    //二度押し
    private fun showDoubleTapWarning() {
        (loadFromJson(listOf("cheat"))["cheat"] as? List<String>)?.random()?.let {
            addTextToLog(it)
        }
        doubleTapText.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            ObjectAnimator.ofFloat(doubleTapText, "alpha", 1f, 0f).apply {
                duration = 500
                start()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        doubleTapText.visibility = View.GONE
                        doubleTapText.alpha = 1f
                    }
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
            }

        }, 500)
    }

    //セリフボックス
    private fun addTextToLog(text: String) {
        val jsonData = loadFromJson(listOf("top"))
        val top = jsonData["top"] as? String ?: "Default Top"  // デフォルト値を設定

        val textView = TextView(context)
        textView.text = "$top：$text"  // "top: text" 形式に変更
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        textView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.textContainer.addView(textView, 0)
        if (binding.textContainer.childCount > maxLines) {
            binding.textContainer.removeViewAt(binding.textContainer.childCount - 1)
        }
        binding.scrollView.post {
            binding.scrollView.fullScroll(ScrollView.FOCUS_UP)
        }
    }

    private val timerRunnable = Runnable { handleTimer() }


    //PlayFragmentを終了
    fun finishPlayFragment(result: String) {
        val resTap = if (isInfiniteMode) "$tapCount" else (repLimit - tapCount).toString()

        // 無限モードでは必ず "bye" のリストを表示する
        val jsonData = loadFromJson(
            listOf(
                if (isInfiniteMode || result == "OK") "bye" else "byeN",
                "phrase"
            )
        )

        val byeList = jsonData[if (isInfiniteMode || result == "OK") "bye" else "byeN"] as? List<String> ?: emptyList()

        val phraseTemplate = (jsonData["phrase"] as? List<String>)?.getOrNull(2) ?: "～回腕立て伏せをしました。"
        val phraseMessage = phraseTemplate.replace("～", resTap)

        val randomMessage = byeList.filter { it.isNotBlank() }.randomOrNull() ?: "お疲れ様でした。"

        val resTime = if (isInfiniteMode) timeCount else {
            val elapsedMillis = SystemClock.uptimeMillis() - startTime
            val minutes = ((elapsedMillis / (1000 * 60)) % 60).toString().padStart(2, '0')
            val seconds = ((elapsedMillis / 1000) % 60).toString().padStart(2, '0')
            "$minutes:$seconds"
        }

        Option.COIN += resTap.toInt() * 10

        val dialog = ResultDialog.newInstance(resTap, resTime, randomMessage, phraseMessage)
        dialog.setOnDialogCloseListener(object : ResultDialog.OnDialogCloseListener {
            override fun onDialogClose() {
                parentFragmentManager.beginTransaction().remove(this@PlayFragment).commit()
            }
        })
        dialog.showNow(parentFragmentManager, "custom")

        listOf(runnable, timerRunnable).forEach { handler.removeCallbacks(it) }

        isRunning = false
        tapCount = 0
        updateTapCount()
    }


    //時間をチェック
    private fun startIdleTimeoutChecker() {
        lastLimitMessageTime = SystemClock.uptimeMillis()
        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTime = SystemClock.uptimeMillis()
                if (currentTime - lastUserInteractionTime >= tapTimeoutMillis) {
                    if (currentTime - lastLimitMessageTime >= limitMessageInterval) {

                        (loadFromJson(listOf("limit"))["limit"] as? List<String>)?.random()?.let {
                            addTextToLog(it)
                        }

                        lastLimitMessageTime = currentTime
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    // タップが行われた場合、このメソッドでユーザー操作時間を更新
    private fun updateLastUserInteractionTime() {
        lastUserInteractionTime = SystemClock.uptimeMillis()
    }


    //タッチイベント
    private fun handleTouchEvent(event: MotionEvent?): Boolean {
        val isProximityEvent = event == null

        if (!isRunning || (!isProximityEvent && event?.action != MotionEvent.ACTION_UP)) return true

        val currentTime = System.currentTimeMillis()
        updateLastUserInteractionTime()

        // ダブルタップ判定
        if (currentTime - lastTapTime <= tapInterval) {
            showDoubleTapWarning()
            return true
        }

        // 必要なデータの取得
        val jsonData = loadFromJson(listOf("lines", "phrase"))
        val lines = jsonData["lines"] as? List<String> ?: emptyList()
        val phrases = jsonData["phrase"] as? List<String> ?: listOf("～回", "残り～回")

        // 20%の確率でlinesの内容をログに追加
        lines.takeIf { it.isNotEmpty() && Math.random() < 0.20 }?.random()?.let {
            addTextToLog(it)
        }

        // カウント処理とメッセージ表示
        val message = if (isInfiniteMode) {
            tapCount++
            phrases[0]
        } else {
            tapCount--
            phrases[1]
        }.replace("～", tapCount.toString())

        addTextToLog(message)

        // 通常モードでカウントが0以下なら終了
        if (!isInfiniteMode && tapCount <= 0) {
            finishPlayFragment("OK")
            return true
        }

        // タップ数の更新とアニメーションの適用
        updateTapCount()
        lastTapTime = currentTime

        // Option.THEMEが"g"でない場合のみshowImagesを実行
        if (Option.THEME != "g") {
            startScaleAnimation(wallImage)
            showImages()
        }


        return true
    }

    // タイマーイベント
    private fun handleTimer() {
        if (isInfiniteMode) {
            // 無限モード: 時間表示を更新し続ける
            val elapsedMillis = SystemClock.uptimeMillis() - startTime
            val minutes = ((elapsedMillis / (1000 * 60)) % 60).toString().padStart(2, '0')
            val seconds = ((elapsedMillis / 1000) % 60).toString().padStart(2, '0')
            timeCount = "$minutes:$seconds"
            timerTextView.text = timeCount

            // 1秒ごとに再度更新
            handler.postDelayed(timerRunnable, 1000)

        } else {
            // 通常モード: カウントダウン処理
            if (timeLimit > 0) {
                val minutes = (timeLimit / 60).toString().padStart(2, '0')
                val seconds = (timeLimit % 60).toString().padStart(2, '0')
                timeCount = "$minutes:$seconds"
                timerTextView.text = timeCount
                timeLimit--

                // 1秒ごとに再度更新
                handler.postDelayed(timerRunnable, 1000)
            } else {
                // カウントダウン終了
                timeCount = "00:00"
                timerTextView.text = timeCount
                finishPlayFragment("NG")
            }
        }
    }


    private fun startScaleAnimation(view: View) {
        // 拡大アニメーションの設定
        val scaleUp = ScaleAnimation(
            0.5f, 2.0f,
            0.5f, 2.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
            fillAfter = true
        }
        // 縮小アニメーションの設定
        val scaleDown = ScaleAnimation(
            2.0f, 0.5f,
            2.0f, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
            fillAfter = true
        }
        // アニメーションセットの作成
        val animationSet = AnimationSet(true).apply {
            addAnimation(scaleUp)
            addAnimation(scaleDown)
        }
        view.startAnimation(animationSet)
    }

    //引数受け取り
    companion object {
        private const val ARG_TIME_LIMIT = "time_limit"
        private const val ARG_REP_LIMIT = "rep_limit"

        fun newInstance(timeLimit: Int, repLimit: Int): PlayFragment {
            val fragment = PlayFragment()
            val args = Bundle()
            args.putInt(ARG_TIME_LIMIT, timeLimit)
            args.putInt(ARG_REP_LIMIT, repLimit)
            fragment.arguments = args
            return fragment
        }
    }

}
