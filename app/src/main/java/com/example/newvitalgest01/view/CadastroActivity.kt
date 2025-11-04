package com.example.newvitalgest01.view

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Toast
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

    // flag pra não entrar em loop quando formatar telefone
    private var isFormattingPhone: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupPhoneMask()
        setupPasswordHelper()
        setupButtons()
    }

    // ---------------- BOTÕES ----------------

    private fun setupButtons() {
        // Botão CADASTRAR
        binding.btCadastrar.setOnClickListener {
            val nomeCompleto = binding.editNomeCompleto.text.toString().trim()
            val email = binding.editEmail.text.toString().trim()
            val telefone = binding.editTelefone.text.toString().trim()
            val senha = binding.editSenha.text.toString()
            val confirmarSenha = binding.editConfirmarSenha.text.toString()

            when {
                nomeCompleto.isEmpty() -> {
                    mostrarMensagem("Preencha o nome completo!", "#FF0000")
                }
                email.isEmpty() -> {
                    mostrarMensagem("Preencha o e-mail!", "#FF0000")
                }
                !isEmailValido(email) -> {
                    mostrarMensagem("Digite um e-mail válido!", "#FF0000")
                }
                telefone.isEmpty() -> {
                    mostrarMensagem("Preencha o telefone!", "#FF0000")
                }
                senha.isEmpty() -> {
                    mostrarMensagem("Informe uma senha!", "#FF0000")
                }
                senha.length < 6 -> {
                    mostrarMensagem("A senha precisa ter pelo menos 6 caracteres!", "#FF0000")
                }
                confirmarSenha.isEmpty() -> {
                    mostrarMensagem("Confirme a senha!", "#FF0000")
                }
                senha != confirmarSenha -> {
                    mostrarMensagem("As senhas não conferem!", "#FF0000")
                }
                else -> {
                    cadastrarNoFirebase(nomeCompleto, email, telefone, senha)
                }
            }
        }

        // Botão VOLTAR (para o menu / login)
        binding.btVoltarLogin.setOnClickListener {
            navegarParaMenu()
        }
    }

    // ------------- CADASTRO NO FIREBASE (AUTH + Firestore em paralelo) -------------

    private fun cadastrarNoFirebase(
        nomeCompleto: String,
        email: String,
        telefone: String,
        senha: String
    ) {
        binding.btCadastrar.isEnabled = false
        mostrarMensagem("Criando conta, aguarde...", "#2196F3")

        auth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener { task ->
                binding.btCadastrar.isEnabled = true

                if (!task.isSuccessful) {
                    val e = task.exception
                    val mensagem = when (e) {
                        is FirebaseAuthException -> {
                            when (e.errorCode) {
                                "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já está em uso."
                                "ERROR_INVALID_EMAIL" -> "E-mail inválido."
                                "ERROR_WEAK_PASSWORD" -> "Senha fraca. Tente outra."
                                else -> "Erro ao criar conta: ${e.localizedMessage}"
                            }
                        }
                        else -> "Erro ao criar conta: ${e?.localizedMessage ?: "desconhecido"}"
                    }

                    mostrarMensagem(mensagem, "#FF0000")
                    return@addOnCompleteListener
                }

                // ✅ Usuário criado com sucesso no Auth
                val uid = task.result?.user?.uid

                mostrarMensagem(
                    "Conta criada com sucesso! Faça login para continuar.",
                    "#4CAF50"
                )

                // 🔹 Salva os dados extras no Firestore, mas NÃO bloqueia a navegação
                if (uid != null) {
                    salvarUsuarioNoFirestore(uid, nomeCompleto, email, telefone)
                }

                // 👉 Dá tempo de ler a mensagem e depois volta pra tela de login
                handler.postDelayed({
                    navegarParaMenu()
                }, 2000)
            }
    }

    private fun salvarUsuarioNoFirestore(
        uid: String,
        nomeCompleto: String,
        email: String,
        telefone: String
    ) {
        val usuarioMap = hashMapOf(
            "nome" to nomeCompleto,
            "email" to email,
            "telefone" to telefone,
            "criadoEm" to FieldValue.serverTimestamp()
        )

        firestore.collection("usuarios")
            .document(uid)
            .set(usuarioMap)
            .addOnFailureListener { e ->
                // Só um aviso rápido caso dê erro – não trava o fluxo
                Toast.makeText(
                    this,
                    "Conta criada, mas houve erro ao salvar dados no servidor.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ------------- MÁSCARA DE TELEFONE -------------

    private fun setupPhoneMask() {
        binding.editTelefone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingPhone) return

                val current = s?.toString() ?: ""
                val formatted = formatPhone(current)

                if (current == formatted) return

                isFormattingPhone = true
                binding.editTelefone.setText(formatted)
                binding.editTelefone.setSelection(formatted.length)
                isFormattingPhone = false
            }
        })
    }

    // transforma 81999999999 -> (81) 9.9999-9999
    private fun formatPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(11)
        if (digits.isEmpty()) return ""

        val l = digits.length
        val sb = StringBuilder()

        sb.append("(")
        sb.append(digits.substring(0, if (l >= 2) 2 else l))

        if (l < 3) {
            return sb.toString()
        }

        sb.append(") ")
        sb.append(digits[2])

        if (l == 3) {
            return sb.toString()
        }

        sb.append(".")

        if (l <= 7) {
            sb.append(digits.substring(3, l))
            return sb.toString()
        }

        sb.append(digits.substring(3, 7))
        sb.append("-")
        sb.append(digits.substring(7, l))

        return sb.toString()
    }

    // ------------- AJUDA NA SENHA -------------

    private fun setupPasswordHelper() {
        binding.editSenha.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                if (length in 1..5) {
                    binding.editSenha.error =
                        "A senha precisa ter pelo menos 6 caracteres (${length}/6)"
                } else {
                    binding.editSenha.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ------------- UTILITÁRIOS -------------

    private fun isEmailValido(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Usa sempre a root view pra garantir que o Snackbar aparece
    private fun mostrarMensagem(mensagem: String, cor: String) {
        val snackbar = Snackbar.make(binding.root, mensagem, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(Color.parseColor(cor))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    private fun navegarParaMenu() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}