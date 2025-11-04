package com.example.newvitalgest01.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.databinding.ActivityElegibilidadeBinding
import com.google.android.material.snackbar.Snackbar

class ElegibilidadeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityElegibilidadeBinding
    private val respostas = mutableMapOf<Int, Boolean>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElegibilidadeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        setupQuiz()
    }

    private fun setupQuiz() {
        // Pergunta 1
        binding.pergunta1Sim.setOnClickListener { selecionarResposta(0, true) }
        binding.pergunta1Nao.setOnClickListener { selecionarResposta(0, false) }

        // Pergunta 2
        binding.pergunta2Sim.setOnClickListener { selecionarResposta(1, true) }
        binding.pergunta2Nao.setOnClickListener { selecionarResposta(1, false) }

        // Pergunta 3
        binding.pergunta3Sim.setOnClickListener { selecionarResposta(2, true) }
        binding.pergunta3Nao.setOnClickListener { selecionarResposta(2, false) }

        binding.btVerificarResultado.setOnClickListener { view ->
            when {
                respostas.size < 3 -> {
                    mostrarMensagem(view, "Responda todas as perguntas.", "#FF5252")
                }

                isElegivel() -> {
                    binding.resultadoElegivel.visibility = View.VISIBLE
                    binding.resultadoNaoElegivel.visibility = View.GONE
                    binding.textoLoading.visibility = View.VISIBLE

                    mostrarMensagem(
                        view,
                        "Parabéns! Você está apto(a) a doar sangue.",
                        "#4CAF50"
                    )

                    handler.postDelayed({
                        navegarParaHome()
                    }, 2000)
                }

                else -> {
                    binding.resultadoElegivel.visibility = View.GONE
                    binding.resultadoNaoElegivel.visibility = View.VISIBLE
                    binding.textoLoading.visibility = View.VISIBLE

                    mostrarMensagem(
                        view,
                        "No momento você não está apto(a) a doar. Consulte um hemocentro.",
                        "#FF5252"
                    )

                    handler.postDelayed({
                        navegarParaHome()
                    }, 2000)
                }
            }
        }
    }

    private fun selecionarResposta(indice: Int, resposta: Boolean) {
        respostas[indice] = resposta
        atualizarBotoesResposta(indice, resposta)
    }

    private fun atualizarBotoesResposta(indice: Int, resposta: Boolean) {
        when (indice) {
            0 -> atualizarEstadoBotao(binding.pergunta1Sim, binding.pergunta1Nao, resposta)
            1 -> atualizarEstadoBotao(binding.pergunta2Sim, binding.pergunta2Nao, resposta)
            2 -> atualizarEstadoBotao(binding.pergunta3Sim, binding.pergunta3Nao, resposta)
        }
    }

    private fun atualizarEstadoBotao(botaoSim: Button, botaoNao: Button, resposta: Boolean) {
        atualizarCorBotao(botaoSim, resposta)
        atualizarCorBotao(botaoNao, !resposta)
    }

    private fun atualizarCorBotao(botao: Button, selecionado: Boolean) {
        if (selecionado) {
            botao.setBackgroundColor(Color.parseColor("#4CAF50"))
            botao.setTextColor(Color.WHITE)
        } else {
            botao.setBackgroundColor(Color.parseColor("#CCCCCC"))
            botao.setTextColor(Color.BLACK)
        }
    }

    private fun isElegivel(): Boolean {
        // Exemplo de regra:
        // 0: idade ok
        // 1: peso ok
        // 2: não está doente / sem impedimentos
        return respostas.size == 3 &&
                (respostas[0] == true) &&
                (respostas[1] == true) &&
                (respostas[2] == false)
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