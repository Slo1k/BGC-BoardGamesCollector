package com.example.bgc

import android.R
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.text.HtmlCompat
import com.example.bgc.databinding.ActivityExpansionsListBinding
import com.squareup.picasso.Picasso

class ExpansionsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpansionsListBinding

    private val dbHandler = MyDBHandler(this, null, null, 1)
    private var sortColumn = "title"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpansionsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayData()

        val options = arrayOf("tytuł", "data wydania")
        binding.ssExpansions.adapter = ArrayAdapter<String>(
            this,
            R.layout.simple_list_item_1,
            options
        )

        binding.ssExpansions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                sortColumn = options[position]
                displayData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onRestart() {
        super.onRestart()
        displayData()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun displayData() {
        val data = dbHandler.readBoardGamesExpansions(sortColumn)
        binding.tlExpansions.removeAllViews()
        val displayMetrics = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        for(i in 0 until data.size) {
            val verticalLayout = LinearLayout(this)
            val tableRow = TableRow(this)
            val titleView = TextView(this)
            val rankView = TextView(this)
            val descriptionView = TextView(this)
            val imageView = ImageView(this)
            val params = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT
            )

            params.setMargins(10, 10, 10, 10)
            rankView.layoutParams = params
            imageView.layoutParams = params
            verticalLayout.layoutParams = params
            verticalLayout.orientation = LinearLayout.VERTICAL
            titleView.textSize = 15F
            rankView.textSize = 15F
            tableRow.minimumHeight = 250
            rankView.width = (screenWidth*0.1).toInt()
            descriptionView.width = (screenWidth * 0.65).toInt()
            titleView.width = (screenWidth * 0.65).toInt()
            rankView.gravity = Gravity.CENTER
            binding.tlExpansions.gravity = Gravity.CENTER
            tableRow.setBackgroundResource(com.example.bgc.R.drawable.row_border)
            tableRow.addView(rankView)
            tableRow.addView(imageView)
            verticalLayout.addView(titleView)
            verticalLayout.addView(descriptionView)
            tableRow.addView(verticalLayout)

            val title = "<b>${data[i].title} </b> (${data[i].year})"
            titleView.text = Html.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY)
            rankView.text = data[i].rank.toString()
            when {
                data[i].description == null -> descriptionView.text = "Brak opisu"
                data[i].description?.length!! > 200 -> descriptionView.text = data[i].description?.substring(0, 200) + "..."
                else -> descriptionView.text = data[i].description
            }
            Picasso.get().load(data[i].image).resize(200, 0).into(imageView)

            binding.tlExpansions.addView(tableRow)

        }
    }
}