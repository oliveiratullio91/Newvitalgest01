package com.example.newvitalgest01.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R

class ContatoInformacoesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contato_informacoes)

        supportActionBar?.hide()

        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val btnEmail = findViewById<Button>(R.id.btnEmail)
        val btnTelefone = findViewById<Button>(R.id.btnTelefone)
        val btnSite = findViewById<Button>(R.id.btnSite)
        val btnSobreApp = findViewById<Button>(R.id.btnSobreApp)
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        // Botão de envio de e-mail
        btnEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:contato@vitalgest.com")
                putExtra(Intent.EXTRA_SUBJECT, "Contato via aplicativo")
            }
            startActivity(Intent.createChooser(intent, "Escolha o aplicativo de e-mail"))
        }

        // Botão para ligação
        btnTelefone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:81999999999")
            }
            startActivity(intent)
        }

        // Botão para abrir o site
        btnSite.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://projeto-doacao-sangue.vercel.app/")
            }
            startActivity(intent)
        }

        // Botão Sobre o app (abre tela dedicada)
        btnSobreApp.setOnClickListener {
            val intent = Intent(this, SobreAppActivity::class.java)
            startActivity(intent)
        }

        // Botão Voltar
        btnVoltar.setOnClickListener {
            finish()
        }
    }
}