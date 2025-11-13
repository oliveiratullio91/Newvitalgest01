package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R

class HemocentrosProximosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HemocentroAdapter
    private val listaHemocentros = mutableListOf<HemocentroItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hemocentros_proximos)

        // 🔹 Remove completamente a ActionBar (tira a faixa azul superior)
        supportActionBar?.hide()

        // 🔹 Define cores da status bar e navigation bar para combinar com o fundo
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        // 🔹 Ícones escuros (modo claro)
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

        recyclerView = findViewById(R.id.recyclerHemocentros)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)

        // Dados fictícios (pode ser substituído por dados do Firebase futuramente)
        listaHemocentros.add(
            HemocentroItem(
                "HEMOPE Recife",
                "Rua Joaquim Nabuco, 171 - Derby",
                "(81) 3182-4600"
            )
        )
        listaHemocentros.add(
            HemocentroItem(
                "Hemoar",
                "Av. Agamenon Magalhães, 1000",
                "(81) 3333-1234"
            )
        )

        adapter = HemocentroAdapter(listaHemocentros)
        recyclerView.adapter = adapter

        // Botão Voltar (no layout)
        btnVoltar.setOnClickListener {
            finish()
        }
    }
}

// Modelo de dados do hemocentro
data class HemocentroItem(
    val nome: String,
    val endereco: String,
    val telefone: String
)
