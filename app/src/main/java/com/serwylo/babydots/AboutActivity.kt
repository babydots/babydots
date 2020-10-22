package com.serwylo.babydots

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import mehdi.sakout.fancybuttons.FancyButton

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<FancyButton>(R.id.button_github).setOnClickListener {
            val uri = Uri.parse(getString(R.string.url_github))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        val attribution = findViewById<TextView>(R.id.attribution)
        attribution.movementMethod = LinkMovementMethod.getInstance()

    }

}