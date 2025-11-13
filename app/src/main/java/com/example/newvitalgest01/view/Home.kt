package com.example.newvitalgest01.view

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.MainActivity
import com.example.newvitalgest01.R
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

        // 1) Primeiro infla o layout e associa ao window (cria o DecorView)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) Agora é seguro mexer na status bar / navigation bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        // Ícones escuros na status bar (modo claro)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        supportActionBar?.hide()

        setupButtons()
        carregarNomeUsuario()
        carregarProximoAgendamento()
        carregarStatusElegibilidade()   // carrega o status ao abrir
    }

    override fun onResume() {
        super.onResume()
        // toda vez que voltar pra Home, recarrega nome, próximo agendamento e status
        carregarNomeUsuario()
        carregarProximoAgendamento()
        carregarStatusElegibilidade()   // atualiza status depois do quiz
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

        // Histórico de Doações
        binding.btHistorico.setOnClickListener {
            startActivity(Intent(this, HistoricoDoacoesActivity::class.java))
        }

        // Hemocentros Próximos
        binding.btClinicas.setOnClickListener {
            startActivity(Intent(this, HemocentrosProximosActivity::class.java))
        }

        // Contato e Informações
        binding.btContato.setOnClickListener {
            startActivity(Intent(this, ContatoInformacoesActivity::class.java))
        }

        // 🔹 Botão Sair → volta para a MainActivity (tela principal)
        binding.btSair.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(intent)
            finish() // fecha a Home pra não voltar com o botão de "voltar"
        }
    }

    // ---------------- NOME DO USUÁRIO ----------------

    private fun carregarNomeUsuario() {
        val usuario = auth.currentUser
        if (usuario == null) {
            // txtSaudacao já está "Bem-vindo(a)," no XML
            binding.txtNomeUsuario.text = "Usuário"
            return
        }

        firestore.collection("usuarios")
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { doc ->
                val nome = doc.getString("nome")
                binding.txtNomeUsuario.text = when {
                    !nome.isNullOrBlank() -> nome
                    !usuario.email.isNullOrBlank() -> usuario.email
                    else -> "Usuário"
                }
            }
            .addOnFailureListener {
                binding.txtNomeUsuario.text = "Usuário"
            }
    }

    // ---------------- STATUS DE ELEGIBILIDADE ----------------

    private fun carregarStatusElegibilidade() {
        // mesmo SharedPreferences usado na ElegibilidadeActivity
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val respondeuQuiz = prefs.getBoolean("elegibilidade_respondida", false)

        if (!respondeuQuiz) {
            // nunca respondeu o quiz
            binding.txtStatusElegibilidade.text = "Status: Não verificado"
            binding.txtStatusElegibilidade.setTextColor(Color.DKGRAY)
            return
        }

        val elegivel = prefs.getBoolean("elegivel_para_doar", false)

        if (elegivel) {
            binding.txtStatusElegibilidade.text = "Status: Elegível"
            binding.txtStatusElegibilidade.setTextColor(Color.parseColor("#4CAF50")) // verde
        } else {
            binding.txtStatusElegibilidade.text = "Status: Não Elegível"
            binding.txtStatusElegibilidade.setTextColor(Color.parseColor("#FF5252")) // vermelho
        }
    }

    // ---------------- PRÓXIMO AGENDAMENTO + STATUS ----------------

    private fun carregarProximoAgendamento() {
        val usuario = auth.currentUser
        if (usuario == null) {
            binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
            binding.txtStatusAgendamento.visibility = View.GONE
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
                    binding.txtStatusAgendamento.visibility = View.GONE
                } else {
                    val doc = snapshot.documents.first()
                    val hemocentro = doc.getString("hemocentro") ?: "Hemocentro"
                    val data = doc.getString("data") ?: ""
                    val hora = doc.getString("hora") ?: ""
                    val status = doc.getString("status") ?: ""

                    if (data.isBlank() || hora.isBlank()) {
                        binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
                        binding.txtStatusAgendamento.visibility = View.GONE
                    } else {
                        binding.txtProximoAgendamento.text =
                            "Próxima doação:\n$hemocentro\n$data às $hora"

                        // --------- STATUS NO CHIP (parte inferior direita) ---------
                        if (status.isBlank()) {
                            binding.txtStatusAgendamento.visibility = View.GONE
                        } else {
                            binding.txtStatusAgendamento.visibility = View.VISIBLE

                            when (status.lowercase()) {
                                "pendente" -> {
                                    binding.txtStatusAgendamento.text = "⏳ Pendente"
                                    binding.txtStatusAgendamento.setTextColor(
                                        Color.parseColor("#1976D2") // azul
                                    )
                                }
                                "confirmado", "confirmada" -> {
                                    binding.txtStatusAgendamento.text = "✅ Confirmado"
                                    binding.txtStatusAgendamento.setTextColor(
                                        Color.parseColor("#4CAF50") // verde
                                    )
                                }
                                "cancelado", "cancelada" -> {
                                    binding.txtStatusAgendamento.text = "❌ Cancelado"
                                    binding.txtStatusAgendamento.setTextColor(
                                        Color.parseColor("#F44336") // vermelho
                                    )
                                }
                                else -> {
                                    binding.txtStatusAgendamento.text = status
                                    binding.txtStatusAgendamento.setTextColor(
                                        Color.parseColor("#555555") // cinza neutro
                                    )
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
                binding.txtStatusAgendamento.visibility = View.GONE
            }
    }
}