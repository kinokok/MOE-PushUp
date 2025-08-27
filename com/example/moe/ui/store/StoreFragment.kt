package com.example.moe.ui.store

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.moe.R
import com.example.moe.databinding.FragmentStoreBinding
import com.example.moe.ui.option.Option
import com.example.moe.ui.theme.StoreDialog
import com.example.moe.ui.utils.Utils
import com.google.android.material.tabs.TabLayout
import com.google.gson.reflect.TypeToken
import java.io.IOException

data class ReadmeData(
    var top: String,
    var name: String,
    var detail: String,
    var introduction: String,
    var isAchieved: Boolean
)


class StoreFragment : Fragment() {
    private lateinit var binding: FragmentStoreBinding
    private var selectedFolder: String? = null
    private var selectedItem: ReadmeData? = null

    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStoreBinding.inflate(inflater, container, false)
        val view = binding.root

        // sharedPreferences の初期化
        sharedPreferences = requireContext().getSharedPreferences("achieved_status", Context.MODE_PRIVATE)

        // タブの設定
        setupTabLayout()

        // 初期表示はすべてのアイテムを表示
        showStoreItems(null)

        // Update buttonのリスナー
        binding.updateButton.setOnClickListener {
            if (selectedFolder != null) {
                Option.THEME = selectedFolder.toString()
                binding.alreadyUpdateFrameLayout.visibility = View.VISIBLE
                binding.updateButton.visibility = View.GONE
            }
        }

        // buyUpdateButtonを押すとStoreDialogを開く
        binding.buyUpdateButton.setOnClickListener {
            val dialog = StoreDialog({ updateAchievedStatus() }, selectedItem?.name ?: "")
            dialog.show(parentFragmentManager, "StoreDialog")
        }


        return view
    }

    // `isAchieved` を更新してリストをリフレッシュ
    private fun updateAchievedStatus() {
        selectedItem?.let { item ->
            // Achieveステータスをtrueに更新
            item.isAchieved = true

            // 保存 (SharedPreferences経由で保存)
            Utils.saveAchievedStatus(requireContext(), selectedFolder!!, true)

            // リストをリロードしてソート
            showStoreItems(null) // タブの現在の表示状態に基づいてリストを更新する処理に変更
        }
    }

    private fun setupTabLayout() {
        // タブの追加
        val tabs = listOf("ALL", "UNLOCKED", "LOCKED")

        for (title in tabs) {
            val tab = binding.tabLayout.newTab()
            val customView = LayoutInflater.from(context).inflate(R.layout.custom_tab, null)
            customView.findViewById<TextView>(R.id.tabTitle).text = title
            tab.customView = customView
            binding.tabLayout.addTab(tab)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showStoreItems(null)  // 全てのアイテムを表示
                    1 -> showStoreItems(true)  // 達成済みアイテムのみ表示
                    2 -> showStoreItems(false) // 未達成アイテムのみ表示
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // 指定した条件に応じてアイテムを表示
    private fun showStoreItems(filterAchieved: Boolean?) {
        val assetManager = requireContext().assets
        val folderNames = assetManager.list("")?.filter {
            it != "geoid_map" && it != "webkit" && it != "images" &&
                    assetManager.list(it)?.isNotEmpty() == true
        } ?: listOf()

        binding.buyUpdateFrameLayout.visibility = View.GONE

        val itemList = folderNames.mapNotNull { folderName ->
            val readmeFileName = "$folderName/readme.json"
            val readmeData = Utils.loadDataFromAssetFile(
                requireContext(),
                readmeFileName,
                object : TypeToken<ReadmeData>() {}
            )
            readmeData?.let { data ->
                val isAchieved = loadAchievementStatus(folderName, data.isAchieved)
                data.isAchieved = isAchieved
                Pair(data, folderName)
            }
        }

        val sortedItems = when (filterAchieved) {
            null -> itemList.sortedWith(compareBy({ !it.first.isAchieved }, { it.first.name }))
            else -> itemList.filter { it.first.isAchieved == filterAchieved }
                .sortedBy { it.first.name }
        }

        binding.storeList.removeAllViews()

        var themeItemView: View? = null
        var themeData: ReadmeData? = null

        var firstItemView: View? = null
        var firstData: ReadmeData? = null
        var firstFolderName: String? = null

        for ((data, folderName) in sortedItems) {
            val itemView = layoutInflater.inflate(R.layout.item_store, binding.storeList, false)

            // Set `top` to `store_name`
            val nameTextView = itemView.findViewById<TextView>(R.id.store_name)
            nameTextView.text = data.top // Use 'top' for `store_name`

            // Set image view
            val imageView = itemView.findViewById<ImageView>(R.id.store_picture)
            val picPaths = listOf("$folderName/pic_front.png", "$folderName/pic_front.jpg")
            var imageLoaded = false
            for (picPath in picPaths) {
                try {
                    val inputStream = assetManager.open(picPath)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    imageView.setImageBitmap(bitmap)
                    inputStream.close()
                    imageLoaded = true
                    break
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            if (!imageLoaded) {
                imageView.setImageResource(R.drawable.pic_face_error)
            }

            itemView.findViewById<LinearLayout>(R.id.store_product).setOnClickListener {
                updateUIForSelectedFolder(data, folderName)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(10, 10, 10, 10)
            }
            itemView.layoutParams = params
            binding.storeList.addView(itemView)

            if (!data.isAchieved) {
                binding.buyUpdateFrameLayout.visibility = View.VISIBLE
            }

            if (firstItemView == null) {
                firstItemView = itemView
                firstData = data
                firstFolderName = folderName
            }

            if (folderName == Option.THEME) {
                themeItemView = itemView
                themeData = data
            }
        }

        themeData?.let {
            updateUIForSelectedFolder(it, Option.THEME)
        }

        if (themeData == null && firstData != null) {
            updateUIForSelectedFolder(firstData, firstFolderName!!)
        }
    }


    private fun updateUIForSelectedFolder(data: ReadmeData, folderName: String) {
        selectedFolder = folderName
        selectedItem = data

        binding.textProductName.text = data.name
        binding.textProductDetail.text = data.detail
        binding.textIntroduction.text = data.introduction

        try {
            val picCharacterPath = "$folderName/pic_character.png"
            val inputStream = requireContext().assets.open(picCharacterPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            binding.picCharacter.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            binding.picCharacter.setImageResource(R.drawable.pic_character_error)
        }

        if (folderName == Option.THEME) {
            binding.alreadyUpdateFrameLayout.visibility = View.VISIBLE
            binding.updateButton.visibility = View.GONE
        } else {
            binding.alreadyUpdateFrameLayout.visibility = View.GONE
            binding.updateButton.visibility = View.VISIBLE
        }

        if (!data.isAchieved) {
            binding.buyUpdateFrameLayout.visibility = View.VISIBLE
        } else {
            binding.buyUpdateFrameLayout.visibility = View.GONE
        }
    }


    private fun loadAchievementStatus(folderName: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(folderName, defaultValue)
    }

    private fun updateAchievementStatus(folderName: String, isAchieved: Boolean) {
        sharedPreferences.edit().putBoolean(folderName, isAchieved).apply()
    }
}
