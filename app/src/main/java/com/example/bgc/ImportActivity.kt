package com.example.bgc

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.bgc.databinding.ActivityImportBinding
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class ImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportBinding

    private val dbHandler = MyDBHandler(this, null, null, 1)
    var gamesIds: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.btnImportData.setOnClickListener{
            val fd = FileDownloader()
            fd.execute()
            val imm=getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.btnImportData.windowToken,0)
            binding.pbLoadingUser.visibility = View.VISIBLE
        }
    }

    private fun clearDescription(description: String): String{
        var result = ""
        var stop = false
        for (i in description)
            if(i == '&')
                stop = true
            else if (i == ';')
                stop = false
            else if (!stop)
                result += i
        return result
    }

    @SuppressLint("StaticFieldLeak")
    private inner class FileDownloader : AsyncTask<String, Int, String>() {
        
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val dd = DetailsDownloader()
            dd.execute()
        }

        override fun doInBackground(vararg params: String?): String {
            try {
                val username = binding.tvUsernameInput.text
                val url = URL("https://www.boardgamegeek.com/xmlapi2/collection?username=$username")
                val connection = url.openConnection()
                connection.connect()
                val lengthOfFile = connection.contentLength
                val isStream = url.openStream()
                val directory = File("$filesDir/XML")
                if (!directory.exists()) directory.mkdir()
                val fos = FileOutputStream("$directory/data.xml")
                val data = ByteArray(1024)
                var count: Int
                var total: Long = 0
                var progress = 0
                count = isStream.read(data)
                while (count != -1) {
                    total += count.toLong()
                    val progressTemp = total.toInt() * 100 / lengthOfFile
                    if (progressTemp % 10 == 0 && progress != progressTemp)
                        progress = progressTemp
                    fos.write(data, 0, count)
                    count = isStream.read(data)
                }
                isStream.close()
                fos.close()
                dbHandler.addUser(username.toString())
                findGame()
            } catch (e: MalformedURLException) {
                return "Malformed URL"
            } catch (e: FileNotFoundException) {
                return "File not found"
            } catch (e: IOException) {
                return "IO Exception"
            }
            return "success"
        }

        private fun findGame() {
            val inDir = File("$filesDir/XML")
            if (!inDir.exists()) inDir.mkdir()
            if (inDir.exists()) {
                val file = File(inDir, "data.xml")
                if (file.exists()) {
                    val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        file
                    )
                    xmlDoc.documentElement.normalize()
                    val items: NodeList = xmlDoc.getElementsByTagName("item")
                    for (i in 0 until items.length) {
                        val itemNode: Node = items.item(i)
                        if (itemNode.nodeType == Node.ELEMENT_NODE) {
                            val elem = itemNode as Element
                            val id = elem.getAttribute("objectid")
                            gamesIds.add(id)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class DetailsDownloader : AsyncTask<String, Int, String>() {

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(gamesIds.isNotEmpty()){
                Toast.makeText(applicationContext, "Dodano gry użytkownika ${binding.tvUsernameInput.text} do kolekcji", Toast.LENGTH_LONG).show()
                finish()
            }
            else{
                Toast.makeText(applicationContext, "Nie znaleziono użytkownika ${binding.tvUsernameInput.text}", Toast.LENGTH_LONG).show()
                binding.tvUsernameInput.setText("")
            }
            binding.pbLoadingUser.visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String {
            try {
                for (id in gamesIds){
                    val url = URL("https://www.boardgamegeek.com/xmlapi2/thing?id=$id&stats=1")
                    val connection = url.openConnection()
                    connection.connect()
                    val lengthOfFile = connection.contentLength
                    val isStream = url.openStream()
                    val directory = File("$filesDir/XML")
                    if (!directory.exists()) directory.mkdir()
                    val fos = FileOutputStream("$directory/game.xml")
                    val data = ByteArray(1024)
                    var count: Int
                    var total: Long = 0
                    var progress = 0
                    count = isStream.read(data)
                    while (count != -1) {
                        total += count.toLong()
                        val progressTemp = total.toInt() * 100 / lengthOfFile
                        if (progressTemp % 10 == 0 && progress != progressTemp)
                            progress = progressTemp
                        fos.write(data, 0, count)
                        count = isStream.read(data)
                    }
                    isStream.close()
                    fos.close()
                    val game = getDetails()
                    game.bggId = id.toLong()
                    if (!dbHandler.checkIfGameExistsBgg(id.toLong()))
                        dbHandler.addBoardGame(game)
                    dbHandler.addRank(game.rank,game.title.toString())
                }
            } catch (e: MalformedURLException) {
                return "Zły URL"
            } catch (e: FileNotFoundException) {
                return "Brak pliku"
            } catch (e: IOException) {
                return "Wyjątek IO"
            }
            return "success"
        }

        private fun getDetails(): Game {
            val game = Game()
            val inDir = File("$filesDir/XML")
            if (!inDir.exists()) inDir.mkdir()
            if (inDir.exists()) {
                val file = File(inDir, "game.xml")
                if (file.exists()) {
                    val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                        file
                    )
                    xmlDoc.documentElement.normalize()
                    val items: NodeList = xmlDoc.getElementsByTagName("item")
                    val ranks: NodeList = xmlDoc.getElementsByTagName("ranks")
                    for (i in 0 until items.length) {
                        val itemNode: Node = items.item(i)
                        if (itemNode.nodeType == Node.ELEMENT_NODE) {
                            val elem = itemNode as Element
                            when(elem.getAttribute("type")){
                                "boardgameexpansion" -> game.type = "dodatek"
                                "boardgame" -> game.type = "podstawowa"
                            }
                            val children = elem.childNodes
                            for (j in 0 until children.length) {
                                val node = children.item(j)
                                if (node is Element) {
                                    when (node.nodeName) {
                                        "thumbnail" -> {
                                            game.image = node.textContent
                                        }
                                        "description" -> {
                                            game.description = clearDescription(node.textContent)
                                        }
                                        "yearpublished" -> {
                                            game.year = node.getAttribute("value").toInt()
                                        }
                                        "name" -> {
                                            if (node.getAttribute("type") == "primary")
                                                game.originalTitle = node.getAttribute("value")
                                            game.title = game.originalTitle
                                        }

                                    }
                                }
                            }
                        }
                    }
                    for (i in 0 until ranks.length) {
                        val ranksNode: Node = ranks.item(i)
                        if (ranksNode.nodeType == Node.ELEMENT_NODE) {
                            val elem = ranksNode as Element
                            val children = elem.childNodes
                            for (j in 0 until children.length) {
                                val node = children.item(j)
                                if (node is Element) {
                                    when (node.nodeName) {
                                        "rank" -> {
                                            if (node.getAttribute("name") == "boardgame"){
                                                if (node.getAttribute("value") != "Not Ranked")
                                                    game.rank = node.getAttribute("value").toInt()
                                                else
                                                    game.rank = 0
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return game
        }
    }
}