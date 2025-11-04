package com.example.newvitalgest01.view

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.MainActivity
import com.example.newvitalgest01.databinding.ActivityCadastroBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private val handler = Handler(Looper.getMainLooper())

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.btCadastrar.setOnClickListener { view ->
            cadastrarUsuario(view)
        }

        binding.btVoltarLogin.setOnClickListener {
            navegarParaLogin()
        }
    }

    private fun cadastrarUsuario(view: View) {
        val nome = binding.editNomeCompleto.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val telefone = binding.editTelefone.text.toString().trim()
        val senha = binding.editSenha.text.toString()
        val confirmarSenha = binding.editConfirmarSenha.text.toString()

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty()) {
            mostrarMensagem(view, "Preencha todos os campos obrigatórios.", "#FF5252")
            return
        }

        if (senha.length < 6) {
            mostrarMensagem(view, "A senha deve ter pelo menos 6 caracteres.", "#FF5252")
            return
        }

        if (senha != confirmarSenha) {
            mostrarMensagem(view, "As senhas não conferem.", "#FF5252")
            return
        }

        // Desabilita o botão para evitar cliques múltiplos
        binding.btCadastrar.isEnabled = false

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    binding.btCadastrar.isEnabled = true
                    mostrarMensagem(view, "Erro inesperado ao criar a conta.", "#FF5252")
                    return@addOnSuccessListener
                }

                val dadosUsuario = hashMapOf(
                    "nome" to nome,
                    "email" to email,
                    "telefone" to telefone,
                    "criadoEm" to FieldValue.serverTimestamp()
                )

                firestore.collection("usuarios")
                    .document(uid)
                    .set(dadosUsuario)
                    .addOnSuccessListener {
                        mostrarMensagem(view, "Conta criada com sucesso!", "#4CAF50")

                        handler.postDelayed({
                            navegarParaLogin()
                        }, 2000)
                    }
                    .addOnFailureListener { e ->
                        binding.btCadastrar.isEnabled = true
                        mostrarMensagem(
                            view,
                            "Erro ao salvar dados: ${e.localizedMessage}",
                            "#FF5252"
                        )
                    }
            }
            .addOnFailureListener { e ->
                binding.btCadastrar.isEnabled = true

                val mensagem = when (e) {
                    is FirebaseAuthException -> when (e.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já está em uso."
                        "ERROR_INVALID_EMAIL" -> "E-mail inválido."
                        "ERROR_WEAK_PASSWORD" -> "Senha fraca. Tente outra."
                        else -> "Erro ao criar conta: ${e.localizedMessage}"
                    }
                    else -> "Erro ao criar conta: ${e.localizedMessage}"
                }

                mostrarMensagem(view, mensagem, "#FF5252")
            }
    }

    private fun mostrarMensagem(view: View, mensagem: String, cor: String) {
        val snackbar = Snackbar.make(view, mensagem, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(Color.parseColor(cor))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    private fun navegarParaLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}