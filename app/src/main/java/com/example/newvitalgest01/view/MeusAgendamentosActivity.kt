package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityMeusAgendamentosBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MeusAgendamentosActivity : BaseActivity() {

    private lateinit var binding: ActivityMeusAgendamentosBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val listaAgendamentosAtivos = mutableListOf<AgendamentoItem>()
    private val listaAgendamentosCancelados = mutableListOf<AgendamentoItem>()
    private val listaAgendamentosHistorico = mutableListOf<AgendamentoItem>()
    private lateinit var adapter: MeusAgendamentosAdapter

    private enum class AbaAgendamento { ATIVOS, CANCELADOS, HISTORICO }
    private var abaAtual: AbaAgendamento = AbaAgendamento.ATIVOS

    private val formatoDataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeusAgendamentosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Remove ActionBar
        supportActionBar?.hide()

        // Status/navigation bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

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

        setupRecycler()
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        carregarAgendamentos()
    }

    private fun setupRecycler() {
        adapter = MeusAgendamentosAdapter(this, emptyList())
        binding.recyclerMeusAgendamentos.layoutManager = LinearLayoutManager(this)
        binding.recyclerMeusAgendamentos.adapter = adapter
    }

    private fun setupUI() {
        // Botão voltar (bottom bar)
        binding.btnVoltar.setOnClickListener {
            finish()
        }

        // Aba padrão: Ativos
        binding.btnTabAtivos.isChecked = true
        abaAtual = AbaAgendamento.ATIVOS
        atualizarEstiloAbas() // aplica estilos iniciais

        // Listener das "abas"
        binding.toggleGrupoAgendamentos.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            abaAtual = when (checkedId) {
                binding.btnTabAtivos.id -> AbaAgendamento.ATIVOS
                binding.btnTabCancelados.id -> AbaAgendamento.CANCELADOS
                binding.btnTabHistorico.id -> AbaAgendamento.HISTORICO
                else -> AbaAgendamento.ATIVOS
            }

            atualizarEstiloAbas()
            atualizarListaPorAba()
        }
    }

    private fun carregarAgendamentos() {
        val usuario = auth.currentUser
        if (usuario == null) {
            binding.txtMensagemVazio.text = "Você precisa estar logado para ver seus agendamentos."
            binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
            binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
            adapter.atualizarLista(emptyList())
            return
        }

        firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                listaAgendamentosAtivos.clear()
                listaAgendamentosCancelados.clear()
                listaAgendamentosHistorico.clear()

                if (snapshot.isEmpty) {
                    binding.txtMensagemVazio.text = "Você ainda não possui agendamentos."
                    binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
                    adapter.atualizarLista(emptyList())
                } else {
                    val agora = Date()

                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val hemocentro = doc.getString("hemocentro") ?: "Hemocentro"
                        val data = doc.getString("data") ?: ""
                        val hora = doc.getString("hora") ?: ""
                        val endereco = doc.getString("endereco") ?: ""
                        val telefone = doc.getString("telefone") ?: ""
                        val statusOriginal = doc.getString("status") ?: "Pendente"

                        val item = AgendamentoItem(
                            id = id,
                            hemocentro = hemocentro,
                            data = data,
                            hora = hora,
                            endereco = endereco,
                            telefone = telefone,
                            status = statusOriginal
                        )

                        val statusNormalizado = statusOriginal.trim()

                        // Verifica se data/hora já passou
                        val dataHoraStr = "$data $hora"
                        val dataAgendada = try {
                            formatoDataHora.parse(dataHoraStr)
                        } catch (e: Exception) {
                            null
                        }
                        val expirado = dataAgendada != null && dataAgendada.before(agora)

                        when {
                            statusNormalizado.equals("Cancelado", ignoreCase = true) -> {
                                listaAgendamentosCancelados.add(item)
                            }
                            statusNormalizado.equals("Concluído", ignoreCase = true) || expirado -> {
                                // Histórico: Concluídos ou data passada (expirado)
                                listaAgendamentosHistorico.add(item)
                            }
                            else -> {
                                // Ativos: Pendente / Confirmado no futuro
                                listaAgendamentosAtivos.add(item)
                            }
                        }
                    }

                    atualizarListaPorAba()
                }
            }
            .addOnFailureListener {
                binding.txtMensagemVazio.text = "Erro ao carregar agendamentos."
                binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
                binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
                adapter.atualizarLista(emptyList())
            }
    }

    private fun atualizarListaPorAba() {
        when (abaAtual) {
            AbaAgendamento.ATIVOS -> {
                if (listaAgendamentosAtivos.isEmpty()) {
                    binding.txtMensagemVazio.text = "Você ainda não possui agendamentos ativos."
                    binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
                    adapter.atualizarLista(emptyList())
                } else {
                    binding.txtMensagemVazio.visibility = android.view.View.GONE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.VISIBLE
                    adapter.atualizarLista(listaAgendamentosAtivos)
                    animarLista()
                }
            }

            AbaAgendamento.CANCELADOS -> {
                if (listaAgendamentosCancelados.isEmpty()) {
                    binding.txtMensagemVazio.text = "Você ainda não possui agendamentos cancelados."
                    binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
                    adapter.atualizarLista(emptyList())
                } else {
                    binding.txtMensagemVazio.visibility = android.view.View.GONE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.VISIBLE
                    adapter.atualizarLista(listaAgendamentosCancelados)
                    animarLista()
                }
            }

            AbaAgendamento.HISTORICO -> {
                if (listaAgendamentosHistorico.isEmpty()) {
                    binding.txtMensagemVazio.text = "Você ainda não possui histórico de agendamentos."
                    binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
                    adapter.atualizarLista(emptyList())
                } else {
                    binding.txtMensagemVazio.visibility = android.view.View.GONE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.VISIBLE
                    adapter.atualizarLista(listaAgendamentosHistorico)
                    animarLista()
                }
            }
        }
    }

    /**
     * Estilo dinâmico das abas (Ativos / Cancelados / Histórico)
     * Deixa bem legível e com botões alinhados.
     */
    private fun atualizarEstiloAbas() {
        fun estilizarBotao(botao: MaterialButton, selecionado: Boolean) {
            val vermelho = ContextCompat.getColor(this, R.color.vermelho_primario)
            val fundoClaro = ContextCompat.getColor(this, R.color.fundo_claro)
            val branco = ContextCompat.getColor(this, android.R.color.white)

            // Remove insets que podem desalinhar altura/contorno
            botao.insetTop = 0
            botao.insetBottom = 0

            if (selecionado) {
                // SELECIONADO: fundo vermelho, texto branco, sem borda
                botao.setBackgroundColor(vermelho)
                botao.setTextColor(branco)
                botao.strokeColor = null
            } else {
                // NÃO selecionado: fundo claro, texto vermelho, borda vermelha
                botao.setBackgroundColor(fundoClaro)
                botao.setTextColor(vermelho)
                botao.strokeColor =
                    android.content.res.ColorStateList.valueOf(vermelho)
            }
        }

        estilizarBotao(binding.btnTabAtivos, abaAtual == AbaAgendamento.ATIVOS)
        estilizarBotao(binding.btnTabCancelados, abaAtual == AbaAgendamento.CANCELADOS)
        estilizarBotao(binding.btnTabHistorico, abaAtual == AbaAgendamento.HISTORICO)
    }

    /**
     * Animação suave na troca de conteúdo da lista
     * (fade-in curto, estilo app grande).
     */
    private fun animarLista() {
        binding.recyclerMeusAgendamentos.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(180)
                .start()
        }
    }

    private fun mostrarDialogAvisoGenerico(title: String, message: String) {
        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()

        dialog.setOnShowListener {
            estilizarBotoesDialog(dialog, isDestructive = false)
        }

        dialog.show()
    }

    private fun estilizarBotoesDialog(dialog: AlertDialog, isDestructive: Boolean) {
        val primaryColor = ContextCompat.getColor(this, R.color.vermelho_primario)
        val white = ContextCompat.getColor(this, android.R.color.white)

        val botaoPositivo = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val botaoNegativo = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        botaoPositivo?.apply {
            setBackgroundColor(primaryColor)
            setTextColor(white)
            textSize = 14f
            isAllCaps = false
            setPadding(40, 10, 40, 10)
        }

        botaoNegativo?.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(primaryColor)
            textSize = 14f
            isAllCaps = false
        }
    }
}