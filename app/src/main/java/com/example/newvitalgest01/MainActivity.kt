package com.example.newvitalgest01

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.databinding.ActivityMainBinding
import com.example.newvitalgest01.view.CadastroActivity
import com.example.newvitalgest01.view.Home
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        supportActionBar?.hide()

        setupLoginButton()
        setupCadastroButton()
    }

    override fun onStart() {
        super.onStart()
        // Se o usuário já estiver logado no Firebase, pula a tela de login
        val usuario = auth.currentUser
        if (usuario != null) {
            navegarParaHome(usuario.email ?: "Usuário")
            // Se não quiser voltar para tela de login:
            // finish()
        }
    }

    private fun setupLoginButton() {
        binding.btLogin.setOnClickListener { view ->
            // AGORA: usamos o campo "Nome" como E-MAIL de login
            val email = binding.editNome.text.toString().trim()
            val senha = binding.editSenha.text.toString().trim()

            when {
                email.isEmpty() -> {
                    mostrarMensagem(view, "Informe seu e-mail!", "#FF0000")
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    mostrarMensagem(view, "Digite um e-mail válido!", "#FF0000")
                }
                senha.isEmpty() -> {
                    mostrarMensagem(view, "Informe sua senha!", "#FF0000")
                }
                senha.length < 6 -> {
                    mostrarMensagem(view, "A senha precisa ter pelo menos 6 caracteres!", "#FF0000")
                }
                else -> {
                    realizarLoginFirebase(view, email, senha)
                }
            }
        }
    }

    private fun realizarLoginFirebase(view: View, email: String, senha: String) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener {
                mostrarMensagem(view, "Login realizado com sucesso!", "#4CAF50")
                // Usa o próprio e-mail como “nome” para exibir na Home
                navegarParaHome(email)
            }
            .addOnFailureListener { e ->
                val mensagem = when (e) {
                    is FirebaseAuthException -> {
                        when (e.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "E-mail inválido."
                            "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado. Faça seu cadastro."
                            "ERROR_WRONG_PASSWORD" -> "Senha incorreta."
                            "ERROR_USER_DISABLED" -> "Usuário desativado."
                            else -> "Erro ao fazer login: ${e.localizedMessage}"
                        }
                    }
                    else -> "Erro ao fazer login: ${e.localizedMessage}"
                }

                mostrarMensagem(view, mensagem, "#FF0000")
            }
    }

    private fun setupCadastroButton() {
        binding.btCadastrar.setOnClickListener {
            navegarParaCadastro()
        }
    }

    private fun mostrarMensagem(view: View, mensagem: String, cor: String) {
        val snackbar = Snackbar.make(view, mensagem, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(Color.parseColor(cor))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    private fun navegarParaHome(nome: String) {
        val intent = Intent(this, Home::class.java)
        // Continua mandando o "nome" como extra, como você já fazia
        intent.putExtra("nome", nome)
        startActivity(intent)
        // Se quiser impedir voltar para o login ao apertar "voltar":
        // finish()
    }

    private fun navegarParaCadastro() {
        val intent = Intent(this, CadastroActivity::class.java)
        startActivity(intent)
    }
}