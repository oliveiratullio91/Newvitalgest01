package com.example.newvitalgest01.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.MainActivity
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Home : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar clara
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        supportActionBar?.hide()

        setupButtons()
        configurarTooltipTipoSanguineo()
    }

    override fun onResume() {
        super.onResume()
        carregarNomeUsuario()
        carregarStatusElegibilidade()
        carregarResumoEstatisticas()
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

        binding.btHistorico.setOnClickListener {
            startActivity(Intent(this, HistoricoDoacoesActivity::class.java))
        }

        binding.btClinicas.setOnClickListener {
            startActivity(Intent(this, HemocentrosProximosActivity::class.java))
        }

        binding.btContato.setOnClickListener {
            startActivity(Intent(this, ContatoInformacoesActivity::class.java))
        }

        binding.btPerfilUsuario.setOnClickListener {
            startActivity(Intent(this, PerfilUsuarioActivity::class.java))
        }

        binding.btSair.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(intent)
            finish()
        }
    }

    // ---------------- TOOLTIP DO ÍCONE ----------------

    private fun configurarTooltipTipoSanguineo() {
        val tooltipText = "Seu tipo sanguíneo"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.iconTipoSanguineo.tooltipText = tooltipText
        } else {
            binding.iconTipoSanguineo.setOnLongClickListener {
                android.widget.Toast.makeText(this, tooltipText, android.widget.Toast.LENGTH_SHORT)
                    .show()
                true
            }
        }
    }

    // ---------------- NOME DO USUÁRIO + TIPO SANGUÍNEO ----------------

    private fun carregarNomeUsuario() {
        val usuario = auth.currentUser
        if (usuario == null) {
            binding.txtNomeUsuario.text = "Usuário"
            binding.iconTipoSanguineo.text = "?"
            aplicarCorTipoSanguineo(null)
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

                val tipo = doc.getString("tipoSanguineo")
                if (!tipo.isNullOrBlank()) {
                    val tipoFormatado = tipo.uppercase(Locale.getDefault())
                    binding.iconTipoSanguineo.text = tipoFormatado
                    aplicarCorTipoSanguineo(tipoFormatado)
                    animarIconeTipoSanguineo()
                } else {
                    binding.iconTipoSanguineo.text = "?"
                    aplicarCorTipoSanguineo(null)
                }
            }
            .addOnFailureListener {
                binding.txtNomeUsuario.text = "Usuário"
                binding.iconTipoSanguineo.text = "?"
                aplicarCorTipoSanguineo(null)
            }
    }

    // ---------------- COR DO ÍCONE PELO TIPO ----------------

    private fun aplicarCorTipoSanguineo(tipo: String?) {
        val cor = when (tipo) {
            "O+" -> Color.parseColor("#C62828") // vermelho forte
            "O-" -> Color.parseColor("#8E0000") // vermelho escuro
            "A+" -> Color.parseColor("#AD1457") // magenta
            "A-" -> Color.parseColor("#6A1B9A") // roxo
            "B+" -> Color.parseColor("#1565C0") // azul
            "B-" -> Color.parseColor("#2E7D32") // verde
            "AB+" -> Color.parseColor("#4527A0") // roxo profundo
            "AB-" -> Color.parseColor("#00897B") // teal
            else -> ContextCompat.getColor(this, R.color.vermelho_primario)
        }

        binding.iconTipoSanguineo.backgroundTintList = ColorStateList.valueOf(cor)
    }

    // ---------------- ANIMAÇÃO SUAVE NO ÍCONE ----------------

    private fun animarIconeTipoSanguineo() {
        val scaleUpX = ObjectAnimator.ofFloat(binding.iconTipoSanguineo, View.SCALE_X, 1f, 1.15f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.iconTipoSanguineo, View.SCALE_Y, 1f, 1.15f)
        val scaleDownX = ObjectAnimator.ofFloat(binding.iconTipoSanguineo, View.SCALE_X, 1.15f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.iconTipoSanguineo, View.SCALE_Y, 1.15f, 1f)

        scaleUpX.duration = 160
        scaleUpY.duration = 160
        scaleDownX.duration = 160
        scaleDownY.duration = 160

        val upSet = AnimatorSet().apply { playTogether(scaleUpX, scaleUpY) }
        val downSet = AnimatorSet().apply { playTogether(scaleDownX, scaleDownY) }

        AnimatorSet().apply {
            playSequentially(upSet, downSet)
            start()
        }
    }

    // ---------------- STATUS DE ELEGIBILIDADE ----------------

    private fun carregarStatusElegibilidade() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val respondeuQuiz = prefs.getBoolean("elegibilidade_respondida", false)

        if (!respondeuQuiz) {
            binding.txtStatusElegibilidade.text = "Status: Não verificado"
            binding.txtStatusElegibilidade.setTextColor(Color.DKGRAY)
            return
        }

        val elegivel = prefs.getBoolean("elegivel_para_doar", false)

        if (elegivel) {
            binding.txtStatusElegibilidade.text = "Status: Elegível"
            binding.txtStatusElegibilidade.setTextColor(
                ContextCompat.getColor(this, R.color.verde_sucesso)
            )
        } else {
            binding.txtStatusElegibilidade.text = "Status: Não elegível"
            binding.txtStatusElegibilidade.setTextColor(Color.RED)
        }
    }

    // ---------------- RESUMO ESTATÍSTICAS ----------------

    private fun carregarResumoEstatisticas() {
        val usuario = auth.currentUser ?: return

        firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    binding.txtTotalDoacoes.text = "0"
                    binding.txtVidasImpactadas.text = "0"
                    binding.txtAgendamentosPendentes.text = "0"
                    return@addOnSuccessListener
                }

                var concluidas = 0
                var pendentes = 0

                val locale = Locale("pt", "BR")

                val statusConcluidos = setOf(
                    "realizada",
                    "concluida",
                    "concluído",
                    "concluido",
                    "efetivada"
                )

                snapshot.documents.forEach { doc ->
                    val status = (doc.getString("status") ?: "")
                        .trim()
                        .lowercase(locale)

                    when {
                        status in statusConcluidos -> concluidas++
                        status == "pendente" -> pendentes++
                        // cancelado / expirado / etc. são ignorados
                    }
                }

                // total de doações realizadas
                binding.txtTotalDoacoes.text = concluidas.toString()

                // vidas impactadas (4 por doação como média)
                binding.txtVidasImpactadas.text = (concluidas * 4).toString()

                // agendamentos com status exatamente "pendente"
                binding.txtAgendamentosPendentes.text = pendentes.toString()
            }
    }

    // ---------------- PRÓXIMO AGENDAMENTO ----------------

    private fun carregarProximoAgendamento() {
        val usuario = auth.currentUser ?: run {
            binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
            binding.txtStatusAgendamento.visibility = View.GONE
            return
        }

        val uid = usuario.uid
        val locale = Locale("pt", "BR")

        val statusPendentes = setOf("agendado", "confirmado", "pendente")

        firestore.collection("usuarios")
            .document(uid)
            .collection("agendamentos")
            .orderBy("data", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
                    binding.txtStatusAgendamento.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val hoje = Calendar.getInstance()
                hoje.set(Calendar.HOUR_OF_DAY, 0)
                hoje.set(Calendar.MINUTE, 0)
                hoje.set(Calendar.SECOND, 0)
                hoje.set(Calendar.MILLISECOND, 0)
                val hojeMillis = hoje.timeInMillis

                val sdfData = SimpleDateFormat("dd/MM/yyyy", locale)
                val sdfHora = SimpleDateFormat("HH:mm", locale)

                var melhorAgendamentoDoc: com.google.firebase.firestore.DocumentSnapshot? = null
                var menorDiferenca: Long? = null

                for (doc in snapshot.documents) {
                    val status = (doc.getString("status") ?: "").lowercase(locale)
                    if (status !in statusPendentes) continue

                    val dataStr = doc.getString("data") ?: continue
                    val horaStr = doc.getString("hora") ?: "00:00"

                    val data = try {
                        sdfData.parse(dataStr)
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    val hora = try {
                        sdfHora.parse(horaStr)
                    } catch (_: Exception) {
                        null
                    } ?: Date(0)

                    val cal = Calendar.getInstance()
                    cal.time = data
                    val calHora = Calendar.getInstance()
                    calHora.time = hora

                    cal.set(Calendar.HOUR_OF_DAY, calHora.get(Calendar.HOUR_OF_DAY))
                    cal.set(Calendar.MINUTE, calHora.get(Calendar.MINUTE))
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val agendamentoMillis = cal.timeInMillis

                    if (agendamentoMillis < hojeMillis) continue

                    val diff = agendamentoMillis - hojeMillis

                    if (menorDiferenca == null || diff < menorDiferenca!!) {
                        menorDiferenca = diff
                        melhorAgendamentoDoc = doc
                    }
                }

                if (melhorAgendamentoDoc == null) {
                    binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
                    binding.txtStatusAgendamento.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val hemocentro = melhorAgendamentoDoc.getString("hemocentro") ?: "Hemocentro"
                val cidade = melhorAgendamentoDoc.getString("cidade") ?: ""
                val estado = melhorAgendamentoDoc.getString("estado") ?: ""
                val dataStr = melhorAgendamentoDoc.getString("data") ?: ""
                val horaStr = melhorAgendamentoDoc.getString("hora") ?: ""

                val localTexto = when {
                    cidade.isNotBlank() && estado.isNotBlank() -> "$cidade / $estado"
                    cidade.isNotBlank() -> cidade
                    estado.isNotBlank() -> estado
                    else -> ""
                }

                val linhaLocal = if (localTexto.isNotBlank()) " - $localTexto" else ""

                binding.txtProximoAgendamento.text =
                    "📅 $dataStr às $horaStr\n$hemocentro$linhaLocal"

                val statusOriginal =
                    (melhorAgendamentoDoc.getString("status") ?: "").lowercase(locale)
                binding.txtStatusAgendamento.visibility = View.VISIBLE

                when (statusOriginal) {
                    "agendado" -> {
                        binding.txtStatusAgendamento.text = "Agendamento pendente de confirmação"
                        binding.txtStatusAgendamento.setTextColor(Color.parseColor("#FFA000"))
                    }
                    "confirmado" -> {
                        binding.txtStatusAgendamento.text = "Agendamento confirmado"
                        binding.txtStatusAgendamento.setTextColor(
                            ContextCompat.getColor(this, R.color.verde_sucesso)
                        )
                    }
                    "pendente" -> {
                        binding.txtStatusAgendamento.text = "Agendamento pendente"
                        binding.txtStatusAgendamento.setTextColor(Color.parseColor("#FFA000"))
                    }
                    else -> {
                        binding.txtStatusAgendamento.text =
                            "Status do agendamento: $statusOriginal"
                        binding.txtStatusAgendamento.setTextColor(
                            Color.parseColor("#555555")
                        )
                    }
                }
            }
            .addOnFailureListener {
                binding.txtProximoAgendamento.text = "📅 Nenhum agendamento futuro"
                binding.txtStatusAgendamento.visibility = View.GONE
            }
    }
}