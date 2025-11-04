package com.example.newvitalgest01.view

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.R

class HistoricoDoacoesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico_doacoes)

        supportActionBar?.apply {
            title = "Histórico de Doações"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back) // seta personalizada
        }

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        // Botão Voltar (no layout)
        btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

data class DoacaoHistoricoItem(
    val hemocentro: String,
    val data: String,
    val hora: String,
    val status: String
)