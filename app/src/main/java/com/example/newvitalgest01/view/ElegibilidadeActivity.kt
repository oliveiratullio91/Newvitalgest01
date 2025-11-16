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

            binding.resultadoElegivel.visibility = View.GONE
            binding.resultadoNaoElegivel.visibility = View.GONE
            binding.textoLoading.visibility = View.GONE

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

            if (elegivel) {
                binding.resultadoElegivel.visibility = View.VISIBLE
                binding.resultadoNaoElegivel.visibility = View.GONE
                binding.textoLoading.visibility = View.VISIBLE

                mostrarMensagem(
                    view,
                    "Parabéns! Você aparenta estar apto(a) a doar sangue.",
                    "#4CAF50"
                )

                handler.postDelayed({
                    navegarParaHome()
                }, 2000)
            } else {
                binding.resultadoElegivel.visibility = View.GONE
                binding.resultadoNaoElegivel.visibility = View.VISIBLE
                binding.textoLoading.visibility = View.VISIBLE

                mostrarMensagem(
                    view,
                    "Neste momento você não deve doar sangue. Procure um hemocentro para orientação.",
                    "#FF5252"
                )

                handler.postDelayed({
                    navegarParaHome()
                }, 2000)
            }
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
