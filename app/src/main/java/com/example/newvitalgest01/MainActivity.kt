package com.example.newvitalgest01

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.example.newvitalgest01.view.BaseActivity
import com.example.newvitalgest01.databinding.ActivityMainBinding
import com.example.newvitalgest01.view.CadastroActivity
import com.example.newvitalgest01.view.Home
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import androidx.core.widget.addTextChangedListener   // ✅ IMPORT QUE FALTAVA

// IMPORTS GOOGLE
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : BaseActivity() {   // 🔹 Agora herdando de BaseActivity

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var prefs: SharedPreferences

    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Esconde a ActionBar para uma tela de login mais clean
        supportActionBar?.hide()

        // SharedPreferences para lembrar login/senha
        prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)

        // Listeners dos campos para esconder/mostrar os hints dinamicamente
        binding.editLogin.addTextChangedListener {
            atualizarHints()
        }

        binding.editSenha.addTextChangedListener {
            atualizarHints()
        }

        setupLoginButton()
        setupCadastroButton()
        setupGoogleLogin()       // 🔹 Configura botão "Entrar com Google"
        carregarLoginSalvo()
    }

    // 🟢 Atualiza os hints de login e senha
    private fun atualizarHints() {
        val loginVazio = binding.editLogin.text.isNullOrEmpty()
        val senhaVazia = binding.editSenha.text.isNullOrEmpty()

        binding.textInputLayoutLogin.hint =
            if (loginVazio) "Digite seu login (e-mail)" else ""

        binding.textInputLayoutSenha.hint =
            if (senhaVazia) "Digite sua senha" else ""
    }

    // 🟢 Carrega login e senha se estavam salvos
    private fun carregarLoginSalvo() {
        val lembrar = prefs.getBoolean("lembrar", false)
        val loginSalvo = prefs.getString("login", "")
        val senhaSalva = prefs.getString("senha", "")

        if (lembrar) {
            binding.checkLembrarLogin.isChecked = true
            binding.editLogin.setText(loginSalvo)
            binding.editSenha.setText(senhaSalva)
        } else {
            binding.checkLembrarLogin.isChecked = false
        }

        atualizarHints()
    }

    // ---------------- LOGIN EMAIL/SENHA ----------------

    private fun setupLoginButton() {
        binding.btLogin.setOnClickListener { view ->
            val login = binding.editLogin.text.toString().trim()
            val senha = binding.editSenha.text.toString().trim()

            when {
                login.isEmpty() -> {
                    mostrarMensagem(view, "Informe seu login!", "#FF0000")
                }
                senha.isEmpty() -> {
                    mostrarMensagem(view, "Informe sua senha!", "#FF0000")
                }
                senha.length < 6 -> {
                    mostrarMensagem(view, "A senha precisa ter pelo menos 6 caracteres!", "#FF0000")
                }
                else -> {
                    realizarLoginFirebase(view, login, senha)
                }
            }
        }
    }

    private fun realizarLoginFirebase(view: View, login: String, senha: String) {
        auth.signInWithEmailAndPassword(login, senha)
            .addOnSuccessListener {
                mostrarMensagem(view, "Login realizado com sucesso!", "#4CAF50")

                // Salvar ou limpar credenciais conforme o checkbox
                if (binding.checkLembrarLogin.isChecked) {
                    prefs.edit()
                        .putBoolean("lembrar", true)
                        .putString("login", login)
                        .putString("senha", senha)
                        .apply()
                } else {
                    prefs.edit().clear().apply()
                }

                navegarParaHome(login)
            }
            .addOnFailureListener { e ->
                val mensagem = when (e) {
                    is FirebaseAuthException -> {
                        when (e.errorCode) {
                            "ERROR_INVALID_EMAIL" -> "Login inválido. Verifique o e-mail cadastrado."
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

    // ---------------- LOGIN COM GOOGLE ----------------

    private fun setupGoogleLogin() {
        // Configuração padrão do Google Sign-In + token para Firebase
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // precisa estar no strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btLoginGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    mostrarMensagem(binding.root, "Falha ao obter token do Google.", "#FF0000")
                }
            } catch (e: ApiException) {
                mostrarMensagem(binding.root, "Falha no login com Google: ${e.statusCode}", "#FF0000")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val usuario = auth.currentUser
                val nome = usuario?.displayName ?: usuario?.email ?: "Usuário"

                mostrarMensagem(binding.root, "Login com Google realizado com sucesso!", "#4CAF50")
                navegarParaHome(nome)
            }
            .addOnFailureListener { e ->
                mostrarMensagem(binding.root, "Erro ao autenticar com Google: ${e.localizedMessage}", "#FF0000")
            }
    }

    // ---------------- CADASTRO ----------------

    private fun setupCadastroButton() {
        binding.btCadastrar.setOnClickListener {
            val intent = Intent(this, CadastroActivity::class.java)
            startActivity(intent)
        }
    }

    // ---------------- UTILITÁRIOS ----------------

    private fun mostrarMensagem(view: View, mensagem: String, corHex: String) {
        val snackbar = Snackbar.make(view, mensagem, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(Color.parseColor(corHex))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()
    }

    private fun navegarParaHome(login: String) {
        val intent = Intent(this, Home::class.java)
        intent.putExtra("nome", login)
        startActivity(intent)
        // Se quiser travar o "voltar" para o login:
        // finish()
    }
}