package com.example.newvitalgest01.view

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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

        // Configura o ActionBar com o botão de voltar (seta no topo)
        supportActionBar?.apply {
            title = "Hemocentros Próximos"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back) // ícone personalizado
            elevation = 8f // leve sombra
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

    // Ação do botão de voltar da ActionBar (seta)
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// Modelo de dados do hemocentro
data class HemocentroItem(
    val nome: String,
    val endereco: String,
    val telefone: String
)