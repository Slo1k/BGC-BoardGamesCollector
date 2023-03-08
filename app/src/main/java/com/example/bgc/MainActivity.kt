package com.example.bgc

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.bgc.databinding.ActivityMainBinding
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val dbHandler = MyDBHandler(this, null, null, 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (dbHandler.getUsername().isEmpty()){
            val intent = Intent(this, ImportActivity::class.java)
            startActivity(intent)
        }

        binding.btnGamesList.setOnClickListener {
            val intent = Intent(this, GamesListActivity::class.java)
            startActivity(intent)
        }

        binding.btnExpansionsList.setOnClickListener {
            val intent = Intent(this, ExpansionsListActivity::class.java)
            startActivity(intent)
        }

        binding.btnSync.setOnClickListener {
            val intent = Intent(this, SynchronizationActivity::class.java)
            startActivity(intent)
        }

        binding.btnClearData.setOnClickListener {
            dbHandler.clearData()
            finish()
            exitProcess(0)
        }

        getUserInfo()
    }

    override fun onResume() {
        super.onResume()
        getUserInfo()
    }

    fun getUserInfo(){
        ("Witaj " + dbHandler.getUsername() + "!").also { binding.tvUsername.text = it }
        ("Liczba posiadanych gier:\n" + dbHandler.getGamesCount()).also { binding.tvGamesCount.text = it }
        ("Liczba posiadanych dodatków:\n" + dbHandler.getExtensionsCount()).also { binding.tvExpansionsCount.text = it }
        ("Data ostaniej synchronizacji z BGG:\n" + dbHandler.getLastSyncDate()).also { binding.tvSyncDate.text = it }
    }
}