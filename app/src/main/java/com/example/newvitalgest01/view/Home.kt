package com.example.newvitalgest01.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.databinding.ActivityHomeBinding

class Home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // Obter nome do usuário do intent
        val nome = intent.getStringExtra("nome") ?: "Usuário"

        binding.txtNomeUsuario.text = "Bem vindo(a), $nome"

        // Configurar os botões
        setupButtons(nome)

        // Carregar próximo agendamento
        carregarProximoAgendamento()
    }

    private fun setupButtons(nome: String?) {
        // Botão Doar Sangue - vai para Agendamento
        binding.btDoarSangue.setOnClickListener {
            val intent = Intent(this, Agendamento::class.java)
            intent.putExtra("nome", nome)
            startActivity(intent)
        }

        // Botão Histórico - funcionalidade em desenvolvimento
        binding.btHistorico.setOnClickListener {
            Toast.makeText(this, "Histórico em desenvolvimento", Toast.LENGTH_SHORT).show()
        }

        // Botão Clínicas - funcionalidade em desenvolvimento
        binding.btClinicas.setOnClickListener {
            Toast.makeText(this, "Clínicas em desenvolvimento", Toast.LENGTH_SHORT).show()
        }

        // Botão Contato - funcionalidade em desenvolvimento
        binding.btContato.setOnClickListener {
            Toast.makeText(this, "Contato em desenvolvimento", Toast.LENGTH_SHORT).show()
        }

        // Botão Elegibilidade - vai para ElegibilidadeActivity
        binding.btElegibilidade.setOnClickListener {
            val intent = Intent(this, ElegibilidadeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun carregarProximoAgendamento() {
        val sharedPref = getSharedPreferences("agendamentos", MODE_PRIVATE)
        val count = sharedPref.getInt("count", 0)

        if (count > 0) {
            // Buscar o último agendamento (mais recente)
            val hemocentro = sharedPref.getString("agendamento_${count}_hemocentro", "")
            val data = sharedPref.getString("agendamento_${count}_data", "")
            val hora = sharedPref.getString("agendamento_${count}_hora", "")

            if (!hemocentro.isNullOrEmpty() && !data.isNullOrEmpty() && !hora.isNullOrEmpty()) {
                binding.txtProximoAgendamento.text = "Próxima doação:\n$hemocentro\n$data às $hora"
            } else {
                binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
            }
        } else {
            binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
        }
    }
}