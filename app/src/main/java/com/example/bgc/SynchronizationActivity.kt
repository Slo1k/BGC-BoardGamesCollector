package com.example.bgc

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.bgc.databinding.ActivitySynchronizationBinding
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
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

class SynchronizationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySynchronizationBinding

    private val dbHandler = MyDBHandler(this, null, null, 1)
    var gamesIds: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySynchronizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ("Data ostatniej synchronizacji:\n" + dbHandler.getLastSyncDate()).also { binding.tvLastSyncDate.text = it }

        binding.btnUpdate.setOnClickListener{
            if (getDateDifference(dbHandler.getLastSyncDate(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).toLong() < 24){
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setMessage("Czy chcesz dokonać synchronizacji mimo że od ostatniej synchronizacji nie minęły 24 godziny?")
                builder.setPositiveButton("Tak") { _, _ ->
                    run {
                        val fd = FileDownloader()
                        fd.execute()
                        binding.pbLoadingUpdate.visibility = View.VISIBLE
                    }
                }
                builder.setNegativeButton("Nie") { _, _ -> }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            } else {
                val fd = FileDownloader()
                fd.execute()
                binding.pbLoadingUpdate.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ("Data ostatniej synchronizacji:\n" + dbHandler.getLastSyncDate()).also { binding.tvLastSyncDate.text = it }
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
                val username = dbHandler.getUsername()
                val url = URL("https://www.boardgamegeek.com/xmlapi2/collection?username=$username")
                val connection = url.openConnection()
                connection.connect()
                val lengthOfFile = connection.contentLength
                val isStream = url.openStream()
                val directory = File("$filesDir/XML")
                if (!directory.exists()) directory.mkdir()
                val fos = FileOutputStream("$directory/data.xml")
                val data = ByteArray(1024)
                var count = 0
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
                findGame()
            } catch (e: MalformedURLException) {
                return "Zły URL"
            } catch (e: FileNotFoundException) {
                return "Brak pliku"
            } catch (e: IOException) {
                return "Wyjątek IO"
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
            displayToast()
            binding.pbLoadingUpdate.visibility = View.GONE
            finish()
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
                    var count = 0
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
                    val game = findDetails()
                    game.bggId = id.toLong()
                    if (!dbHandler.checkIfGameExistsBgg(id.toLong()))
                        dbHandler.addBoardGame(game)
                    else {
                        dbHandler.updateGameRank(id.toLong(), game.rank)
                    }
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

        private fun findDetails(): Game {
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

    fun getDateDifference(fromDate: String, toDate: String, formatter: String= "yyyy-MM-dd HH:mm:ss" , locale: Locale = Locale.getDefault()): String{

        val fmt = SimpleDateFormat(formatter, locale)
        val bgn = fmt.parse(fromDate)
        val end = fmt.parse(toDate)

        val milliseconds = end.time - bgn.time
        val hours = milliseconds / 1000 / 3600

        return hours.toString()
    }

    fun displayToast(){
        Toast.makeText(this, "Pobrano aktualny ranking", Toast.LENGTH_LONG).show()
    }
}