package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R

class HistoricoDoacoesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtSemDoacoes: TextView
    private lateinit var btnVoltar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico_doacoes)

        // 🔹 Remove a ActionBar (faixa azul do topo)
        supportActionBar?.hide()

        // 🔹 Deixa status bar e navigation bar na cor do fundo
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        // 🔹 Ícones escuros na status bar (modo claro)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        recyclerView = findViewById(R.id.recyclerHistoricoDoacoes)
        txtSemDoacoes = findViewById(R.id.txtSemDoacoes)
        btnVoltar = findViewById(R.id.btnVoltar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Aqui você vai buscar os dados reais (Firebase, API, etc).
        // Por enquanto vou deixar uma lista vazia para mostrar a mensagem.
        val listaDoacoes = mutableListOf<DoacaoHistoricoItem>()

        // Se quiser testar com dados, descomenta:
        /*
        listaDoacoes.add(
            DoacaoHistoricoItem(
                hemocentro = "HEMOPE Recife",
                data = "10/11/2025",
                hora = "09:30",
                status = "Concluída"
            )
        )
        */

        if (listaDoacoes.isEmpty()) {
            recyclerView.visibility = View.GONE
            txtSemDoacoes.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            txtSemDoacoes.visibility = View.GONE
            recyclerView.adapter = HistoricoAdapter(listaDoacoes)
        }

        btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// Modelo de dados da doação
data class DoacaoHistoricoItem(
    val hemocentro: String,
    val data: String,
    val hora: String,
    val status: String
)

// Adapter simples para exibir o histórico
class HistoricoAdapter(
    private val itens: List<DoacaoHistoricoItem>
) : RecyclerView.Adapter<HistoricoAdapter.HistoricoViewHolder>() {

    class HistoricoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtLinha1: TextView = itemView.findViewById(android.R.id.text1)
        val txtLinha2: TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoricoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return HistoricoViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoricoViewHolder, position: Int) {
        val item = itens[position]
        holder.txtLinha1.text = "${item.data} ${item.hora} - ${item.hemocentro}"
        holder.txtLinha2.text = "Status: ${item.status}"
    }

    override fun getItemCount(): Int = itens.size
}