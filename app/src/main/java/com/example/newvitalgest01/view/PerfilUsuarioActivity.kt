package com.example.newvitalgest01.view

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityPerfilUsuarioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PerfilUsuarioActivity : BaseActivity() {

    private lateinit var binding: ActivityPerfilUsuarioBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var sexoUsuario: String? = null
    private var uidUsuario: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPerfilUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar / nav bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        supportActionBar?.hide()

        val usuario = auth.currentUser
        if (usuario == null) {
            finish()
            return
        }
        uidUsuario = usuario.uid

        setupButtons()
        carregarDadosUsuario()
        carregarStatusElegibilidade()
    }

    override fun onResume() {
        super.onResume()
        // Se o usuário editar os dados e voltar, recarrega tudo
        carregarDadosUsuario()
        carregarStatusElegibilidade()
    }

    private fun setupButtons() {
        binding.btnFecharPerfil.setOnClickListener {
            finish()
        }

        binding.btnEditarDados.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    EditarDadosActivity::class.java
                )
            )
        }
    }

    // ---------------- DADOS DO USUÁRIO (Firestore) ----------------

    private fun carregarDadosUsuario() {
        val uid = uidUsuario ?: return

        firestore.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val nome = doc.getString("nome") ?: "Usuário"
                val email = doc.getString("email") ?: "-"
                val telefone = doc.getString("telefone") ?: "-"
                val dataNascimento = doc.getString("dataNascimento") ?: ""
                val sexo = doc.getString("sexo") ?: ""
                val peso = doc.getString("peso") ?: ""
                val cidade = doc.getString("cidade") ?: ""
                val estado = doc.getString("estado") ?: ""
                val tipoSanguineo = doc.getString("tipoSanguineo") ?: ""

                sexoUsuario = sexo

                binding.txtPerfilNome.text = nome
                binding.txtPerfilEmail.text = email
                binding.txtPerfilTelefone.text = telefone.ifBlank { "-" }
                binding.txtPerfilSexo.text = when (sexo.lowercase(Locale.getDefault())) {
                    "masculino" -> "Masculino"
                    "feminino" -> "Feminino"
                    else -> "-"
                }

                binding.txtPerfilNascimento.text =
                    if (dataNascimento.isNotBlank()) dataNascimento else "-"

                val idade = calcularIdade(dataNascimento)
                binding.txtPerfilIdade.text = idade?.let { "$it anos" } ?: "-"

                binding.txtPerfilPeso.text =
                    if (peso.isNotBlank()) "$peso kg" else "-"

                binding.txtPerfilCidadeEstado.text = when {
                    cidade.isNotBlank() && estado.isNotBlank() -> "$cidade / $estado"
                    cidade.isNotBlank() -> cidade
                    estado.isNotBlank() -> estado
                    else -> "-"
                }

                binding.txtPerfilTipoSanguineo.text =
                    if (tipoSanguineo.isNotBlank()) tipoSanguineo else "Não informado"

                // Depois de ter sexo e dados, carrega resumo de doações
                carregarResumoDoacoes()
            }
    }

    // ---------------- STATUS DE ELEGIBILIDADE (SharedPreferences) ----------------

    private fun carregarStatusElegibilidade() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val respondeuQuiz = prefs.getBoolean("elegibilidade_respondida", false)

        if (!respondeuQuiz) {
            binding.txtPerfilStatusElegibilidade.text = "Status: Não verificado"
            binding.txtPerfilStatusElegibilidade.setTextColor(Color.DKGRAY)
            return
        }

        val elegivel = prefs.getBoolean("elegivel_para_doar", false)

        if (elegivel) {
            binding.txtPerfilStatusElegibilidade.text = "Status: Elegível"
            binding.txtPerfilStatusElegibilidade.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            binding.txtPerfilStatusElegibilidade.text = "Status: Não Elegível"
            binding.txtPerfilStatusElegibilidade.setTextColor(Color.parseColor("#FF5252"))
        }
    }

    // ---------------- RESUMO DE DOAÇÕES (vidas salvas + próxima data) ----------------

    private fun carregarResumoDoacoes() {
        val uid = uidUsuario ?: return
        val sexoLocal = sexoUsuario ?: ""

        firestore.collection("usuarios")
            .document(uid)
            .collection("agendamentos")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    binding.txtPerfilVidasSalvas.text = "Nenhuma doação registrada ainda."
                    binding.txtPerfilProximaDoacao.text =
                        "Próxima doação: ainda não foi possível calcular."
                    return@addOnSuccessListener
                }

                var qtdRealizadas = 0
                var ultimaDoacaoMillis: Long? = null

                val locale = Locale.getDefault()
                // Agora inclui também "concluído"/"concluido"
                val statusRealizados = setOf(
                    "realizada",
                    "concluida",
                    "concluído",
                    "concluido",
                    "efetivada"
                )

                snapshot.documents.forEach { doc ->
                    val status = (doc.getString("status") ?: "")
                        .lowercase(locale)

                    if (status in statusRealizados) {
                        qtdRealizadas++

                        val data = doc.getString("data")
                        val hora = doc.getString("hora")
                        val millis = parseDataHoraMillis(data, hora)

                        if (millis != null) {
                            if (ultimaDoacaoMillis == null || millis > ultimaDoacaoMillis!!) {
                                ultimaDoacaoMillis = millis
                            }
                        }
                    }
                }

                // Vidas salvas (4 por doação)
                if (qtdRealizadas > 0) {
                    val vidas = qtdRealizadas * 4
                    binding.txtPerfilVidasSalvas.text =
                        "Você já ajudou a salvar aproximadamente $vidas vidas."
                } else {
                    binding.txtPerfilVidasSalvas.text = "Nenhuma doação concluída registrada ainda."
                }

                // Próxima data em que pode doar de novo
                if (ultimaDoacaoMillis != null) {
                    val intervaloDias =
                        if (sexoLocal.lowercase(locale) == "feminino") 90 else 60

                    val proxima = ultimaDoacaoMillis!! + intervaloDias * 24L * 60L * 60L * 1000L
                    val sdfOut = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                    val dataStr = sdfOut.format(Date(proxima))

                    binding.txtPerfilProximaDoacao.text =
                        "Você poderá doar novamente a partir de $dataStr."
                } else {
                    binding.txtPerfilProximaDoacao.text =
                        "Próxima doação: ainda não foi possível calcular."
                }
            }
            .addOnFailureListener {
                binding.txtPerfilVidasSalvas.text = "Não foi possível carregar o histórico."
                binding.txtPerfilProximaDoacao.text =
                    "Próxima doação: ainda não foi possível calcular."
            }
    }

    // ---------------- UTILITÁRIOS ----------------

    private fun calcularIdade(dataNascimento: String?): Int? {
        if (dataNascimento.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            val date = sdf.parse(dataNascimento) ?: return null

            val nasc = Calendar.getInstance()
            nasc.time = date

            val hoje = Calendar.getInstance()
            var idade = hoje.get(Calendar.YEAR) - nasc.get(Calendar.YEAR)

            if (hoje.get(Calendar.DAY_OF_YEAR) < nasc.get(Calendar.DAY_OF_YEAR)) {
                idade--
            }

            idade
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDataHoraMillis(dataStr: String?, horaStr: String?): Long? {
        if (dataStr.isNullOrBlank() || horaStr.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            val date = sdf.parse("$dataStr $horaStr")
            date?.time
        } catch (e: Exception) {
            null
        }
    }
}