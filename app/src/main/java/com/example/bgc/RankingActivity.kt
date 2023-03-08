package com.example.bgc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.TableRow
import android.widget.TextView
import com.example.bgc.databinding.ActivityRankingBinding

class RankingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRankingBinding
    private val dbHandler = MyDBHandler(this, null, null, 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gameTitle = intent.extras?.getString("Game")

        ("Historia rankingu dla gry:\n$gameTitle").also { binding.tvGameTitle.text = it }

        val ranking = gameTitle?.let { dbHandler.getRanking(it) }
        if (ranking != null) {
            for (i in 0 until ranking.size){
                val tableRow = TableRow(this)
                val dateView = TextView(this)

                dateView.text = ranking[i]
                dateView.textSize = 20F
                tableRow.gravity = Gravity.CENTER
                tableRow.setBackgroundResource(R.drawable.row_border)
                tableRow.addView(dateView)
                binding.tlRanking.addView(tableRow)
            }
        }
    }
}