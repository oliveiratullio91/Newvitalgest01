package com.example.newvitalgest01.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.R

class ContatoInformacoesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contato_informacoes)

        // Configura o ActionBar com título e botão de voltar (seta no topo)
        supportActionBar?.apply {
            title = "Contato e Informações"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back) // ícone da seta
            elevation = 8f // sombra sutil para dar destaque
        }

        val btnEmail = findViewById<Button>(R.id.btnEmail)
        val btnTelefone = findViewById<Button>(R.id.btnTelefone)
        val btnSite = findViewById<Button>(R.id.btnSite)
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

        // Botão Voltar (no layout)
        btnVoltar.setOnClickListener {
            finish()
        }
    }

    // Função da seta de voltar no ActionBar (caso esteja aparecendo)
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}