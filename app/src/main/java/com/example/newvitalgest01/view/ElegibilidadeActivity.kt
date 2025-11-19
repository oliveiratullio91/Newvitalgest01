package com.example.newvitalgest01.view

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityElegibilidadeBinding
import com.google.android.material.snackbar.Snackbar

class ElegibilidadeActivity : BaseActivity() {

    private lateinit var binding: ActivityElegibilidadeBinding
    private val respostas = mutableMapOf<Int, Boolean>() // chave = nº da pergunta (1..22), valor = true = "Sim"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityElegibilidadeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Deixa status bar e navigation bar com a mesma cor do fundo
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        // 🔹 Ícones escuros na status bar (modo claro)
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

        setupQuiz()
        configurarCliqueOverlay()
        configurarBotaoVoltar()
    }

    private fun setupQuiz() {
        // Liga cada pergunta aos botões SIM/NÃO
        setupQuestion(1, binding.pergunta1Sim, binding.pergunta1Nao)
        setupQuestion(2, binding.pergunta2Sim, binding.pergunta2Nao)
        setupQuestion(3, binding.pergunta3Sim, binding.pergunta3Nao)
        setupQuestion(4, binding.pergunta4Sim, binding.pergunta4Nao)
        setupQuestion(5, binding.pergunta5Sim, binding.pergunta5Nao)
        setupQuestion(6, binding.pergunta6Sim, binding.pergunta6Nao)
        setupQuestion(7, binding.pergunta7Sim, binding.pergunta7Nao)
        setupQuestion(8, binding.pergunta8Sim, binding.pergunta8Nao)
        setupQuestion(9, binding.pergunta9Sim, binding.pergunta9Nao)
        setupQuestion(10, binding.pergunta10Sim, binding.pergunta10Nao)
        setupQuestion(11, binding.pergunta11Sim, binding.pergunta11Nao)
        setupQuestion(12, binding.pergunta12Sim, binding.pergunta12Nao)
        setupQuestion(13, binding.pergunta13Sim, binding.pergunta13Nao)
        setupQuestion(14, binding.pergunta14Sim, binding.pergunta14Nao)
        setupQuestion(15, binding.pergunta15Sim, binding.pergunta15Nao)
        setupQuestion(16, binding.pergunta16Sim, binding.pergunta16Nao)
        setupQuestion(17, binding.pergunta17Sim, binding.pergunta17Nao)
        setupQuestion(18, binding.pergunta18Sim, binding.pergunta18Nao)
        setupQuestion(19, binding.pergunta19Sim, binding.pergunta19Nao)
        setupQuestion(20, binding.pergunta20Sim, binding.pergunta20Nao)
        setupQuestion(21, binding.pergunta21Sim, binding.pergunta21Nao)
        setupQuestion(22, binding.pergunta22Sim, binding.pergunta22Nao)

        binding.btVerificarResultado.setOnClickListener { view ->
            val totalPerguntas = 22

            // Garante que nada de loading/overlay esteja visível antes de calcular
            binding.textoLoading.visibility = View.GONE
            binding.overlayResultado.visibility = View.GONE

            if (!respondeuTodas(totalPerguntas)) {
                mostrarMensagem(
                    view,
                    "Responda todas as perguntas antes de continuar.",
                    "#FF5252"
                )
                return@setOnClickListener
            }

            val elegivel = isElegivel()
            salvarStatusElegibilidade(elegivel)

            val motivos = if (elegivel) {
                emptyList()
            } else {
                obterMotivosNaoElegivel()
            }

            // Mostra a caixa sobreposta com o resultado
            mostrarOverlayResultado(elegivel, motivos)

            // Snackbar apenas como feedback rápido
            val (mensagemSnack, corSnack) = if (elegivel) {
                "Parabéns! Você aparenta estar apto(a) a doar sangue." to "#4CAF50"
            } else {
                "Neste momento você não deve doar sangue. Veja os motivos na tela." to "#FF5252"
            }

            mostrarMensagem(view, mensagemSnack, corSnack)
        }
    }

    private fun configurarBotaoVoltar() {
        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun configurarCliqueOverlay() {
        // Ao tocar em qualquer lugar do overlay:
        binding.overlayResultado.setOnClickListener {
            // Esconde a caixa de resultado
            binding.overlayResultado.visibility = View.GONE

            // Mostra um pequeno texto de "voltando..."
            binding.textoLoading.visibility = View.VISIBLE

            // Só depois de 2 segundos volta para a tela de serviços (Home)
            handler.postDelayed({
                navegarParaHome()
            }, 2000)
        }
    }

    private fun setupQuestion(id: Int, botaoSim: Button, botaoNao: Button) {
        botaoSim.setOnClickListener {
            respostas[id] = true  // respondeu "Sim"
            atualizarEstadoBotao(botaoSim, botaoNao, true)
        }

        botaoNao.setOnClickListener {
            respostas[id] = false // respondeu "Não"
            atualizarEstadoBotao(botaoSim, botaoNao, false)
        }
    }

    private fun atualizarEstadoBotao(botaoSim: Button, botaoNao: Button, respostaSim: Boolean) {
        atualizarCorBotao(botaoSim, respostaSim)
        atualizarCorBotao(botaoNao, !respostaSim)
    }

    private fun atualizarCorBotao(botao: Button, selecionado: Boolean) {
        if (selecionado) {
            botao.setBackgroundColor(Color.parseColor("#4CAF50")) // Verde quando selecionado
            botao.setTextColor(Color.WHITE)
        } else {
            botao.setBackgroundColor(Color.parseColor("#CCCCCC")) // Cinza quando não selecionado
            botao.setTextColor(Color.BLACK)
        }
    }

    private fun respondeuTodas(totalPerguntas: Int): Boolean {
        val todasPerguntas = (1..totalPerguntas).toList()
        return respostas.keys.containsAll(todasPerguntas)
    }

    private fun isElegivel(): Boolean {
        // Perguntas que precisam ser "SIM" (requisitos básicos)
        val obrigatoriasSim = listOf(1, 2, 3, 4)

        // Perguntas que NÃO podem ser "SIM" (são impedimentos)
        val impedimentoSim = listOf(
            5, 6, 7, 8, 9, 10, 11, 12,
            13, 14, 15, 16, 17, 18, 19,
            20, 21, 22
        )

        val todasPerguntasRespondidas = respondeuTodas(22)
        if (!todasPerguntasRespondidas) return false

        val basicasOk = obrigatoriasSim.all { respostas[it] == true }
        val nenhumImpedimento = impedimentoSim.all { respostas[it] == false }

        return basicasOk && nenhumImpedimento
    }

    private fun obterMotivosNaoElegivel(): List<String> {
        val motivos = mutableListOf<String>()

        // 1–4: deveriam ser "SIM". Se for "NÃO", vira motivo.
        if (respostas[1] == false) {
            motivos.add("Você informou que NÃO tem entre 16 e 69 anos, faixa etária permitida para a doação.")
        }
        if (respostas[2] == false) {
            motivos.add("Você informou que pesa menos de 50 kg, abaixo do mínimo recomendado para doação.")
        }
        if (respostas[3] == false) {
            motivos.add("Você dormiu menos de 6 horas nas últimas 24 horas.")
        }
        if (respostas[4] == false) {
            motivos.add("Você não se alimentou adequadamente nas últimas 4 horas ou consumiu alimentos muito gordurosos.")
        }

        // 5–22: se respondeu "SIM", vira motivo de impedimento
        if (respostas[5] == true) {
            motivos.add("Você consumiu bebida alcoólica nas últimas 12 horas.")
        }
        if (respostas[6] == true) {
            motivos.add("Você está com febre, infecção ou se sentindo mal hoje.")
        }
        if (respostas[7] == true) {
            motivos.add("Você está usando antibióticos ou medicamentos controlados.")
        }
        if (respostas[8] == true) {
            motivos.add("Você relatou doença crônica grave ou descompensada.")
        }
        if (respostas[9] == true) {
            motivos.add("Você já teve hepatite após os 11 anos de idade.")
        }
        if (respostas[10] == true) {
            motivos.add("Você informou HIV, sífilis, hepatite B ou C, doença de Chagas ou malária.")
        }
        if (respostas[11] == true) {
            motivos.add("Você fez tatuagem ou piercing nos últimos 6 meses.")
        }
        if (respostas[12] == true) {
            motivos.add("Você fez tratamento dentário invasivo nos últimos 7 dias.")
        }
        if (respostas[13] == true) {
            motivos.add("Você fez cirurgia recente (menos de 3 meses).")
        }
        if (respostas[14] == true) {
            motivos.add("Você está grávida, esteve grávida recentemente ou está amamentando.")
        }
        if (respostas[15] == true) {
            motivos.add("Você recebeu transfusão de sangue alguma vez na vida.")
        }
        if (respostas[16] == true) {
            motivos.add("Você teve contato íntimo sem proteção com parceiro de risco para IST/DST.")
        }
        if (respostas[17] == true) {
            motivos.add("Você usou drogas ilícitas injetáveis.")
        }
        if (respostas[18] == true) {
            motivos.add("Você recebeu alguma vacina recentemente (menos de 30 dias).")
        }
        if (respostas[19] == true) {
            motivos.add("Você esteve em área endêmica para malária nos últimos 12 meses.")
        }
        if (respostas[20] == true) {
            motivos.add("Você teve COVID-19 recentemente ou está em investigação.")
        }
        if (respostas[21] == true) {
            motivos.add("Seu diagnóstico de COVID-19 tem menos de 10 dias.")
        }
        if (respostas[22] == true) {
            motivos.add("Você informou outro problema de saúde importante que pode impedir a doação.")
        }

        return motivos
    }

    private fun mostrarOverlayResultado(elegivel: Boolean, motivos: List<String>) {
        // some qualquer loading
        binding.textoLoading.visibility = View.GONE

        // mostra overlay
        binding.overlayResultado.visibility = View.VISIBLE

        if (elegivel) {
            binding.txtTituloResultado.text = "Você aparenta estar apto(a) a doar sangue"
            binding.txtMensagemResultado.text =
                "Com base nas suas respostas, não foram encontrados impedimentos importantes. A avaliação final será feita pela equipe do hemocentro."
            binding.txtMotivosResultado.text =
                "Parabéns! Você está elegível para seguir com o agendamento da sua doação."
        } else {
            binding.txtTituloResultado.text = "Neste momento você não deve doar sangue"
            binding.txtMensagemResultado.text =
                "Com base nas suas respostas, identificamos os seguintes pontos de atenção:"
            binding.txtMotivosResultado.text =
                if (motivos.isEmpty()) {
                    "Há respostas que indicam impedimentos temporários ou definitivos para doação. Procure um hemocentro para avaliação detalhada."
                } else {
                    motivos.joinToString(separator = "\n• ", prefix = "• ")
                }
        }
    }

    private fun salvarStatusElegibilidade(elegivel: Boolean) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("elegibilidade_respondida", true)
            .putBoolean("elegivel_para_doar", elegivel)
            .apply()
    }

    private fun mostrarMensagem(view: View, mensagem: String, cor: String) {
        val snackbar = Snackbar.make(view, mensagem, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(Color.parseColor(cor))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    private fun navegarParaHome() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}