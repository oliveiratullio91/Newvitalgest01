package com.example.newvitalgest01

import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        supportActionBar?.hide()

        prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)

        setupLoginButton()
        setupCadastroButton()
        carregarLoginSalvo()
    }

    // 🟢 Carrega e-mail e senha se estavam salvos
    private fun carregarLoginSalvo() {
        val lembrar = prefs.getBoolean("lembrar", false)
        val emailSalvo = prefs.getString("email", "")
        val senhaSalva = prefs.getString("senha", "")

        if (lembrar) {
            binding.checkLembrarLogin.isChecked = true
            binding.editNome.setText(emailSalvo)
            binding.editSenha.setText(senhaSalva)
        }
    }

    private fun setupLoginButton() {
        binding.btLogin.setOnClickListener { view ->
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

                // Se o usuário marcou "Lembrar login", salvar credenciais
                if (binding.checkLembrarLogin.isChecked) {
                    prefs.edit()
                        .putBoolean("lembrar", true)
                        .putString("email", email)
                        .putString("senha", senha)
                        .apply()
                } else {
                    // Apagar se não quiser mais lembrar
                    prefs.edit().clear().apply()
                }

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
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
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
        intent.putExtra("nome", nome)
        startActivity(intent)
        // finish() se quiser impedir voltar pro login
    }
}