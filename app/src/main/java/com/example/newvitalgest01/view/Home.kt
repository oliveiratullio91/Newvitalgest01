package com.example.newvitalgest01.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupButtons()
        carregarNomeUsuario()
        carregarProximoAgendamento()
    }

    override fun onResume() {
        super.onResume()
        // toda vez que voltar pra Home, recarrega nome e próximo agendamento
        carregarNomeUsuario()
        carregarProximoAgendamento()
    }

    // ---------------- BOTÕES ----------------

    private fun setupButtons() {
        binding.btElegibilidade.setOnClickListener {
            startActivity(Intent(this, ElegibilidadeActivity::class.java))
        }

        binding.btDoarSangue.setOnClickListener {
            startActivity(Intent(this, Agendamento::class.java))
        }

        binding.btMeusAgendamentos.setOnClickListener {
            startActivity(Intent(this, MeusAgendamentosActivity::class.java))
        }

        // 👉 Histórico de Doações
        binding.btHistorico.setOnClickListener {
            startActivity(Intent(this, HistoricoDoacoesActivity::class.java))
        }

        // 👉 Hemocentros Próximos
        binding.btClinicas.setOnClickListener {
            startActivity(Intent(this, HemocentrosProximosActivity::class.java))
        }

        // 👉 Contato e Informações
        binding.btContato.setOnClickListener {
            startActivity(Intent(this, ContatoInformacoesActivity::class.java))
        }
    }

    // ---------------- NOME DO USUÁRIO ----------------

    private fun carregarNomeUsuario() {
        val usuario = auth.currentUser
        if (usuario == null) {
            binding.txtNomeUsuario.text = "Bem vindo(a), Usuário"
            return
        }

        firestore.collection("usuarios")
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { doc ->
                val nome = doc.getString("nome")
                binding.txtNomeUsuario.text = if (!nome.isNullOrBlank()) {
                    "Bem vindo(a), $nome"
                } else {
                    "Bem vindo(a), ${usuario.email ?: "Usuário"}"
                }
            }
            .addOnFailureListener {
                binding.txtNomeUsuario.text = "Bem vindo(a), Usuário"
            }
    }

    // ---------------- PRÓXIMO AGENDAMENTO ----------------

    private fun carregarProximoAgendamento() {
        val usuario = auth.currentUser
        if (usuario == null) {
            binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
            return
        }

        firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
                } else {
                    val doc = snapshot.documents.first()
                    val hemocentro = doc.getString("hemocentro") ?: "Hemocentro"
                    val data = doc.getString("data") ?: ""
                    val hora = doc.getString("hora") ?: ""

                    if (data.isBlank() || hora.isBlank()) {
                        binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
                    } else {
                        binding.txtProximoAgendamento.text =
                            "Próxima doação:\n$hemocentro\n$data às $hora"
                    }
                }
            }
            .addOnFailureListener {
                binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
            }
    }
}