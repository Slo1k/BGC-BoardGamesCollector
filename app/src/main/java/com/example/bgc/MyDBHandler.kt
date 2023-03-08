package com.example.bgc

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyDBHandler(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int): SQLiteOpenHelper(context, DATABASE_NAME, factory, 1) {
    companion object {
        private const val DATABASE_NAME = "boardgames.db"
        const val TABLE_GAMES = "games"
        const val GAME_TITLE = "title"
        const val GAME_ORIGINAL_TITLE = "originalTitle"
        const val GAME_YEAR = "year"
        const val GAME_DESCRIPTION = "description"
        const val GAME_BGG_ID = "bggId"
        const val GAME_RANK = "rank"
        const val GAME_TYPE = "type"
        const val GAME_IMAGE = "image"

        const val TABLE_RANKING = "ranking"
        const val RANKING_ID = "_id"
        const val RANKING_DATE = "date"
        const val RANKING_RANK = "rank"
        const val RANKING_TITLE = "title"

        const val TABLE_USER = "user"
        const val USER_USERNAME = "username"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableGames = ("CREATE TABLE $TABLE_GAMES (" +
                "$GAME_TITLE TEXT PRIMARY KEY, " +
                "$GAME_ORIGINAL_TITLE TEXT, " +
                "$GAME_YEAR INTEGER, " +
                "$GAME_DESCRIPTION TEXT, " +
                "$GAME_BGG_ID INTEGER, " +
                "$GAME_RANK INTEGER, " +
                "$GAME_TYPE TEXT, " +
                "$GAME_IMAGE TEXT)")
        db?.execSQL(createTableGames)

        val createTableRanks = ("CREATE TABLE $TABLE_RANKING (" +
                "$RANKING_ID INTEGER PRIMARY KEY, " +
                "$RANKING_DATE TEXT, " +
                "$RANKING_RANK INTEGER, " +
                "$RANKING_TITLE TEXT, " +
                "FOREIGN KEY ($RANKING_TITLE) REFERENCES $TABLE_GAMES($GAME_TITLE))")
        db?.execSQL(createTableRanks)

        val createTableUser = ("CREATE TABLE $TABLE_USER (" +
                "$USER_USERNAME TEXT PRIMARY KEY)")
        db?.execSQL(createTableUser)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GAMES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_RANKING")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        onCreate(db)
    }

    fun clearData(){
        val db = this.readableDatabase
        db.execSQL("DELETE FROM $TABLE_GAMES")
        db.execSQL("DELETE FROM $TABLE_RANKING")
        db.execSQL("DELETE FROM $TABLE_USER")
        db.close()
    }

    @SuppressLint("Range")
    fun readBoardGames(sort: String?): MutableList<Game>{
        var column: String? = null
        when(sort){
            "ranking" -> column = GAME_RANK
            "tytuł" -> column = GAME_TITLE
            "data wydania" -> column = GAME_YEAR
        }
        val list: MutableList<Game> = ArrayList()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_GAMES WHERE $GAME_TYPE = 'podstawowa' ORDER BY $column"
        val result = db.rawQuery(query, null)
        if (result.moveToFirst()){
            do {
                val game = Game()
                game.title = result.getString(result.getColumnIndex(GAME_TITLE))
                game.description = result.getString(result.getColumnIndex(GAME_DESCRIPTION))
                game.image = result.getString(result.getColumnIndex(GAME_IMAGE))
                game.rank = result.getInt(result.getColumnIndex(GAME_RANK))
                game.year = result.getInt(result.getColumnIndex(GAME_YEAR))
                game.type = result.getString(result.getColumnIndex(GAME_TYPE))
                list.add(game)
            } while(result.moveToNext())
            result.close()
        }
        db.close()
        return list
    }

    @SuppressLint("Range")
    fun readBoardGamesExpansions(sort: String?): MutableList<Game> {
        var column: String? = null
        when(sort){
            "tytuł" -> column = GAME_TITLE
            "data wydania" -> column = GAME_YEAR
        }
        val list: MutableList<Game> = ArrayList()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_GAMES WHERE $GAME_TYPE = 'dodatek' ORDER BY $column"
        val result = db.rawQuery(query, null)
        if (result.moveToFirst()){
            do {
                val game = Game()
                game.title = result.getString(result.getColumnIndex(GAME_TITLE))
                game.description = result.getString(result.getColumnIndex(GAME_DESCRIPTION))
                game.image = result.getString(result.getColumnIndex(GAME_IMAGE))
                game.rank = result.getInt(result.getColumnIndex(GAME_RANK))
                game.year = result.getInt(result.getColumnIndex(GAME_YEAR))
                game.type = result.getString(result.getColumnIndex(GAME_TYPE))
                list.add(game)
            } while(result.moveToNext())
            result.close()
        }
        db.close()
        return list
    }

    @SuppressLint("Range")
    fun readGamesIds(): MutableList<Int>{
        val games: MutableList<Int> = ArrayList()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_GAMES"
        val result = db.rawQuery(query, null)
        if (result.moveToFirst()){
            do {
                games.add(result.getInt(result.getColumnIndex(GAME_BGG_ID)))
            } while(result.moveToNext())
            result.close()
        }
        db.close()
        return games
    }

    fun getGamesCount(): String{
        val db = this.readableDatabase
        var countGames = "0"

        if (db.rawQuery("SELECT * FROM $TABLE_GAMES", null).count > 0){
            val query = "SELECT * FROM $TABLE_GAMES WHERE $GAME_TYPE = 'podstawowa'"
            val result = db.rawQuery(query, null)
            countGames = result.count.toString()
        }
        db.close()
        return countGames
    }

    fun getExtensionsCount(): String {
        val db = this.readableDatabase
        var countExtensions = "0"

        if (db.rawQuery("SELECT * FROM $TABLE_GAMES", null).count > 0){
            val query = "SELECT * FROM $TABLE_GAMES WHERE $GAME_TYPE = 'dodatek'"
            val result = db.rawQuery(query, null)
            countExtensions = result.count.toString()
        }

        db.close()
        return countExtensions
    }

    @SuppressLint("Range")
    fun getLastSyncDate(): String {
        val db = this.readableDatabase
        var lastSync = "XXXX-XX-XX XX:XX:XX"

        if (db.rawQuery("SELECT * FROM $TABLE_RANKING", null).count > 0){
            val query = "SELECT MAX($RANKING_DATE) AS MAKS FROM $TABLE_RANKING"
            val result = db.rawQuery(query, null)
            if (result.moveToFirst())
                lastSync = result.getString(result.getColumnIndex("MAKS"))
        }

        db.close()
        return lastSync
    }

    @SuppressLint("Range")
    fun getUsername(): String {
        val db = this.readableDatabase
        var username = ""

        if (db.rawQuery("SELECT * FROM $TABLE_USER", null).count > 0){
            val query = "SELECT $USER_USERNAME from $TABLE_USER"
            val result = db.rawQuery(query, null)
            if (result.moveToFirst())
                username = result.getString(result.getColumnIndex(USER_USERNAME))
        }

        db.close()
        return username
    }

    fun addBoardGame(game: Game){
        val values = ContentValues()
        values.put(GAME_TITLE, game.title)
        values.put(GAME_ORIGINAL_TITLE, game.originalTitle)
        values.put(GAME_YEAR, game.year)
        values.put(GAME_DESCRIPTION, game.description)
        values.put(GAME_RANK, game.rank)
        values.put(GAME_BGG_ID, game.bggId)
        values.put(GAME_TYPE,game.type)
        values.put(GAME_IMAGE, game.image)
        val db = this.writableDatabase
        db.insert(TABLE_GAMES, null, values)
        db.close()
    }

    @SuppressLint("Range")
    fun findBoardGameFromBggId(bggId: Long): String{
        var title = ""
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_GAMES WHERE $GAME_BGG_ID = $bggId"
        val result = db.rawQuery(query, null)
        if (result.moveToFirst()){
            title = result.getString(result.getColumnIndex(GAME_TITLE))
            result.close()
        }
        db.close()

        return title
    }

    fun updateGameRank(bggId: Long, newRank: Int){
        val db = this.readableDatabase
        val values = ContentValues()
        values.put(GAME_RANK, newRank)
        val query = "SELECT * FROM $TABLE_GAMES WHERE $GAME_BGG_ID = $bggId"
        val result = db.rawQuery(query, null)
        if (result.moveToFirst()){
            db.update(TABLE_GAMES, values, "$GAME_BGG_ID = $bggId", null)
        }
        result.close()
        db.close()
    }

    fun checkIfGameExistsBgg(bggId: Long): Boolean{
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_GAMES WHERE $GAME_BGG_ID = $bggId"
        val result = db.rawQuery(query, null)
        if (result.moveToFirst()){
            db.close()
            return true
        }
        db.close()
        return false
    }

    fun addRank(rank: Int, title: String){
        val values = ContentValues()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        values.put(RANKING_DATE, LocalDateTime.now().format(formatter))
        values.put(RANKING_RANK, rank)
        values.put(RANKING_TITLE, title)
        val db = this.writableDatabase
        db.insert(TABLE_RANKING, null, values)
        db.close()
    }

    fun addUser(user: String){
        val value = ContentValues()
        value.put(USER_USERNAME, user)
        val db = this.writableDatabase
        db.insert(TABLE_USER, null, value)
        db.close()
    }

    @SuppressLint("Range")
    fun getRanking(title: String): MutableList<String> {
        val ranking: MutableList<String> = ArrayList()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_RANKING WHERE $RANKING_TITLE = ? ORDER BY $RANKING_DATE DESC"
        val result = db.rawQuery(query, arrayOf(title))
        if (result.moveToFirst()){
            do {
                val rank: String = if(result.getInt(result.getColumnIndex(RANKING_RANK)) == -1)
                    "Brak danych"
                else
                    result.getInt(result.getColumnIndex(RANKING_RANK)).toString()
                val line = "[" + result.getString(result.getColumnIndex(RANKING_DATE)) +
                        "]: " + rank
                ranking.add(line)
            } while(result.moveToNext())
            result.close()
        }
        db.close()
        return ranking
    }
}