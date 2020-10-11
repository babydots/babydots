package com.serwylo.babydots

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {

    private lateinit var dots: AnimatedDots

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        dots = findViewById(R.id.dots)

        dots.setOnClickListener {
            dots.restartDots()
        }

    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_colour -> dots.changeColour()
            R.id.menu_size -> dots.changeSize()
            R.id.menu_speed -> dots.changeSpeed()
        }
        return false
    }
}