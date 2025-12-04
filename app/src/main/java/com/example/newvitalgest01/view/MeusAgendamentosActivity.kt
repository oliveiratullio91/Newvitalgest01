package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityMeusAgendamentosBinding
import com.example.newvitalgest01.domain.model.Agendamento
import com.example.newvitalgest01.ui.meus_agendamentos.MeusAgendamentosViewModel
import com.example.newvitalgest01.ui.meus_agendamentos.MeusAgendamentosViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class MeusAgendamentosActivity : BaseActivity() {

    private lateinit var binding: ActivityMeusAgendamentosBinding

    // ViewModel (MVVM)
    private val viewModel: MeusAgendamentosViewModel by viewModels {
        MeusAgendamentosViewModelFactory(applicationContext)
    }

    // Listas separadas por aba (como antes)
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
        observarEstadoViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.recarregar(forceRemote = true)
    }


    // ---------------- RECYCLER / ADAPTER ----------------

    private fun setupRecycler() {
        adapter = MeusAgendamentosAdapter(this, emptyList())
        binding.recyclerMeusAgendamentos.layoutManager = LinearLayoutManager(this)
        binding.recyclerMeusAgendamentos.adapter = adapter
    }

    // ---------------- UI / ABAS / BOTÕES ----------------

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

    // ---------------- OBSERVANDO VIEWMODEL ----------------

    private fun observarEstadoViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collectLatest { state ->
                // Loading simples: podemos mostrar um texto "Carregando..."
                if (state.isLoading) {
                    binding.txtMensagemVazio.text = "Carregando seus agendamentos..."
                    binding.txtMensagemVazio.visibility = View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = View.GONE
                }

                // Se veio erro
                if (state.error != null) {
                    binding.txtMensagemVazio.text = state.error
                    binding.txtMensagemVazio.visibility = View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = View.GONE
                    adapter.atualizarLista(emptyList())
                    return@collectLatest
                }

                // Quando terminar de carregar sem erro
                if (!state.isLoading && state.error == null) {
                    val agendamentos = state.agendamentos

                    listaAgendamentosAtivos.clear()
                    listaAgendamentosCancelados.clear()
                    listaAgendamentosHistorico.clear()

                    if (agendamentos.isEmpty()) {
                        binding.txtMensagemVazio.text = "Você ainda não possui agendamentos."
                        binding.txtMensagemVazio.visibility = View.VISIBLE
                        binding.recyclerMeusAgendamentos.visibility = View.GONE
                        adapter.atualizarLista(emptyList())
                    } else {
                        preencherListasPorStatusEData(agendamentos)
                        atualizarListaPorAba()
                    }
                }
            }
        }
    }

    /**
     * Converte a lista de Agendamento (domínio) em listas de AgendamentoItem
     * separadas por: Ativos, Cancelados e Histórico (Concluídos/Expirados)
     * usando a mesma regra que você já tinha.
     */
    private fun preencherListasPorStatusEData(agendamentos: List<Agendamento>) {
        val agora = Date()

        for (ag in agendamentos) {
            val item = AgendamentoItem(
                id = ag.id,
                hemocentro = ag.hemocentro,
                data = ag.data,
                hora = ag.hora,
                endereco = ag.endereco,
                telefone = ag.telefone,
                status = ag.status
            )

            val statusNormalizado = ag.status.trim()

            // Verifica se data/hora já passou
            val dataHoraStr = "${ag.data} ${ag.hora}"
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
    }

    // ---------------- ATUALIZAÇÃO POR ABA ----------------

    private fun atualizarListaPorAba() {
        when (abaAtual) {
            AbaAgendamento.ATIVOS -> {
                if (listaAgendamentosAtivos.isEmpty()) {
                    binding.txtMensagemVazio.text = "Você ainda não possui agendamentos ativos."
                    binding.txtMensagemVazio.visibility = View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = View.GONE
                    adapter.atualizarLista(emptyList())
                } else {
                    binding.txtMensagemVazio.visibility = View.GONE
                    binding.recyclerMeusAgendamentos.visibility = View.VISIBLE
                    adapter.atualizarLista(listaAgendamentosAtivos)
                    animarLista()
                }
            }

            AbaAgendamento.CANCELADOS -> {
                if (listaAgendamentosCancelados.isEmpty()) {
                    binding.txtMensagemVazio.text = "Você ainda não possui agendamentos cancelados."
                    binding.txtMensagemVazio.visibility = View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = View.GONE
                    adapter.atualizarLista(emptyList())
                } else {
                    binding.txtMensagemVazio.visibility = View.GONE
                    binding.recyclerMeusAgendamentos.visibility = View.VISIBLE
                    adapter.atualizarLista(listaAgendamentosCancelados)
                    animarLista()
                }
            }

            AbaAgendamento.HISTORICO -> {
                if (listaAgendamentosHistorico.isEmpty()) {
                    binding.txtMensagemVazio.text = "Você ainda não possui histórico de agendamentos."
                    binding.txtMensagemVazio.visibility = View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = View.GONE
                    adapter.atualizarLista(emptyList())
                } else {
                    binding.txtMensagemVazio.visibility = View.GONE
                    binding.recyclerMeusAgendamentos.visibility = View.VISIBLE
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