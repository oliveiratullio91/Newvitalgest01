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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.MainActivity
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityCadastroBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class CadastroActivity : BaseActivity() {

    private lateinit var binding: ActivityCadastroBinding
    private val handler = Handler(Looper.getMainLooper())

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // flags pra não entrar em loop quando formatar telefone/data
    private var isFormattingPhone: Boolean = false
    private var isFormattingDate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Status bar / nav bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        binding = ActivityCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupClearHintOnType()
        setupPhoneMask()
        setupDateMask()
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

            val dataNascimento = binding.editDataNascimento.text.toString().trim()
            val sexo = when {
                binding.radioSexoMasculino.isChecked -> "masculino"
                binding.radioSexoFeminino.isChecked -> "feminino"
                else -> ""
            }
            val peso = binding.editPeso.text.toString().trim()
            val cidade = binding.editCidade.text.toString().trim()
            val estado = binding.editEstado.text.toString().trim()

            val jaDoouAntes = binding.checkboxJaDoou.isChecked
            val aceitaLembretes = binding.checkboxAceitaLembretes.isChecked
            val aceitaTermos = binding.checkboxAceitaTermos.isChecked

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
                dataNascimento.isEmpty() -> {
                    mostrarMensagem("Informe sua data de nascimento!", "#FF0000")
                }
                !isDataValida(dataNascimento) -> {
                    mostrarMensagem(
                        "Data de nascimento inválida. Use o formato dd/mm/aaaa.",
                        "#FF0000"
                    )
                }
                sexo.isEmpty() -> {
                    mostrarMensagem("Selecione se você é homem ou mulher.", "#FF0000")
                }
                cidade.isEmpty() -> {
                    mostrarMensagem("Informe sua cidade!", "#FF0000")
                }
                estado.isEmpty() -> {
                    mostrarMensagem("Informe seu estado (UF)!", "#FF0000")
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
                !aceitaTermos -> {
                    mostrarMensagem(
                        "Você precisa concordar com os Termos de Uso e LGPD para continuar.",
                        "#FF0000"
                    )
                }
                else -> {
                    cadastrarNoFirebase(
                        nomeCompleto = nomeCompleto,
                        email = email,
                        telefone = telefone,
                        senha = senha,
                        dataNascimento = dataNascimento,
                        sexo = sexo,
                        peso = peso,
                        cidade = cidade,
                        estado = estado,
                        jaDoouAntes = jaDoouAntes,
                        aceitaLembretes = aceitaLembretes,
                        aceitaTermos = aceitaTermos
                    )
                }
            }
        }

        // Botão VOLTAR (para login)
        binding.btVoltarLogin.setOnClickListener {
            navegarParaMenu()
        }

        // Botão para ver os Termos de Uso e LGPD
        binding.btVerTermos.setOnClickListener {
            mostrarDialogoTermos()
        }
    }

    // ------------- DIÁLOGO DOS TERMOS -------------

    private fun mostrarDialogoTermos() {
        val termosTexto = """
            Este documento apresenta um resumo dos Termos de Uso e da Política de Privacidade (LGPD) aplicáveis ao aplicativo VitalGest.

            1. Objetivo do aplicativo
            O VitalGest foi desenvolvido para auxiliar usuários no acompanhamento e agendamento de doações de sangue, fornecendo informações, lembretes e registro histórico de doações.
            O aplicativo tem caráter informativo e de apoio, não substituindo, em nenhuma hipótese, a avaliação médica, a triagem clínica oficial ou as orientações fornecidas pelos hemocentros e serviços de saúde.

            2. Uso do aplicativo
            • O usuário se compromete a utilizar o aplicativo de forma responsável e verdadeira, fornecendo informações corretas no cadastro e nas funcionalidades utilizadas.
            • É proibido o uso do aplicativo para fins ilícitos, fraudulentos ou que violem direitos de terceiros.
            • O usuário é responsável por manter a confidencialidade de seus dados de acesso (e-mail e senha).

            3. Limitações e isenção de responsabilidade
            • As informações exibidas no aplicativo têm caráter educativo e de apoio ao agendamento de doações.
            • O VitalGest não realiza atendimento de urgência, nem substitui serviços médicos, hospitais ou hemocentros.
            • A elegibilidade final para doação de sangue é sempre determinada pelo serviço de hemoterapia responsável, durante a triagem presencial.
            • O desenvolvedor do aplicativo não se responsabiliza por decisões tomadas exclusivamente com base nas informações apresentadas no app, sem consulta a profissionais ou serviços de saúde.

            4. Dados pessoais coletados
            Para funcionamento adequado do aplicativo, podem ser coletados e armazenados dados como:
            • Nome completo;
            • E-mail e telefone;
            • Data de nascimento, sexo e peso;
            • Cidade e estado;
            • Informações relacionadas a doações de sangue (histórico, agendamentos, preferências);
            • Preferências de comunicação (ex.: receber lembretes).

            Esses dados são utilizados para:
            • criação e gerenciamento da conta do usuário;
            • exibição de informações personalizadas (ex.: intervalo entre doações);
            • envio de notificações e lembretes, quando autorizado;
            • melhoria da experiência de uso e estatísticas internas (sem identificação direta dos usuários nos relatórios).

            5. Tratamento de dados pessoais (LGPD – Lei nº 13.709/2018)
            O tratamento de dados pessoais realizado pelo VitalGest observa os princípios da LGPD, incluindo:
            • finalidade: os dados são tratados para finalidades específicas e legítimas ligadas ao uso do aplicativo;
            • necessidade: coletamos apenas os dados estritamente necessários ao funcionamento das funcionalidades;
            • transparência: o usuário é informado sobre o uso de seus dados;
            • segurança: são adotadas medidas razoáveis de proteção para reduzir riscos de acesso não autorizado, vazamento ou alteração indevida de dados.

            6. Compartilhamento de dados
            • Os dados do usuário não são vendidos ou compartilhados com terceiros para fins comerciais.
            • O compartilhamento de dados poderá ocorrer, quando necessário, com provedores de serviços técnicos (ex.: serviços de autenticação, banco de dados e hospedagem) estritamente para viabilizar o funcionamento do aplicativo, observando medidas de segurança e confidencialidade.
            • Caso, no futuro, haja integração com hemocentros, bancos de sangue ou outros parceiros, o usuário será informado de forma clara, e, quando exigido por lei, será solicitado novo consentimento.

            7. Direitos do usuário
            Nos termos da LGPD, o usuário tem direito de:
            • acessar os dados pessoais que mantemos a seu respeito;
            • solicitar correção de dados incompletos, inexatos ou desatualizados;
            • solicitar a exclusão de seus dados pessoais, quando aplicável e permitido por lei;
            • revogar o consentimento para uso de dados que dependam dessa base legal, ciente de que isso pode limitar ou inviabilizar o uso de certas funcionalidades do aplicativo.

            Para exercer esses direitos, o usuário poderá entrar em contato pelo e-mail:
            contato@vitalgest.app

            8. Armazenamento e segurança
            • Os dados são armazenados em serviços de nuvem que adotam padrões reconhecidos de segurança.
            • Embora sejam utilizadas medidas técnicas e organizacionais adequadas, nenhum sistema é totalmente imune a incidentes. Em caso de eventual falha de segurança que afete dados pessoais, serão adotadas as medidas razoáveis para mitigar impactos e, quando exigido pela legislação, os usuários e autoridades competentes serão comunicados.

            9. Menores de idade
            • O uso do aplicativo por menores de 18 anos deve ocorrer com conhecimento e acompanhamento de pais ou responsáveis.
            • Em situações em que regulamentações específicas exijam consentimento de responsável legal, o usuário menor de idade deve obter essa autorização antes de utilizar o aplicativo.

            10. Atualizações destes Termos
            Estes Termos de Uso e a Política de Privacidade poderão ser atualizados periodicamente para refletir melhorias no aplicativo, alterações legais ou mudanças em processos de tratamento de dados.
            Sempre que mudanças relevantes forem realizadas, poderemos informar o usuário por meio do próprio aplicativo ou por e-mail, quando adequado.

            11. Contato
            Em caso de dúvidas, solicitações ou reclamações relacionadas a estes Termos de Uso e à proteção de dados pessoais, o usuário poderá entrar em contato pelo e-mail:
            contato@vitalgest.app

            Ao marcar a opção "Li e concordo com os Termos de uso e LGPD" e prosseguir com o cadastro, o usuário declara estar ciente e de acordo com as condições acima.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Termos de Uso e LGPD")
            .setMessage(termosTexto)
            .setPositiveButton("Fechar", null)
            .show()
    }

    // ------------- CADASTRO NO FIREBASE (AUTH + Firestore) -------------

    private fun cadastrarNoFirebase(
        nomeCompleto: String,
        email: String,
        telefone: String,
        senha: String,
        dataNascimento: String,
        sexo: String,
        peso: String,
        cidade: String,
        estado: String,
        jaDoouAntes: Boolean,
        aceitaLembretes: Boolean,
        aceitaTermos: Boolean
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

                val uid = task.result?.user?.uid

                mostrarMensagem(
                    "Conta criada com sucesso! Faça login para continuar.",
                    "#4CAF50"
                )

                if (uid != null) {
                    salvarUsuarioNoFirestore(
                        uid = uid,
                        nomeCompleto = nomeCompleto,
                        email = email,
                        telefone = telefone,
                        dataNascimento = dataNascimento,
                        sexo = sexo,
                        peso = peso,
                        cidade = cidade,
                        estado = estado,
                        jaDoouAntes = jaDoouAntes,
                        aceitaLembretes = aceitaLembretes,
                        aceitaTermos = aceitaTermos
                    )
                }

                handler.postDelayed({
                    navegarParaMenu()
                }, 2000)
            }
    }

    private fun salvarUsuarioNoFirestore(
        uid: String,
        nomeCompleto: String,
        email: String,
        telefone: String,
        dataNascimento: String,
        sexo: String,
        peso: String,
        cidade: String,
        estado: String,
        jaDoouAntes: Boolean,
        aceitaLembretes: Boolean,
        aceitaTermos: Boolean
    ) {
        val usuarioMap = hashMapOf(
            "nome" to nomeCompleto,
            "email" to email,
            "telefone" to telefone,
            "dataNascimento" to dataNascimento,
            "sexo" to sexo,
            "peso" to peso,
            "cidade" to cidade,
            "estado" to estado,
            "jaDoouAntes" to jaDoouAntes,
            "aceitaLembretes" to aceitaLembretes,
            "aceitaTermos" to aceitaTermos,
            "criadoEm" to FieldValue.serverTimestamp()
        )

        firestore.collection("usuarios")
            .document(uid)
            .set(usuarioMap)
            .addOnFailureListener {
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

    // 81999999999 -> (81) 9.9999-9999
    private fun formatPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(11)
        if (digits.isEmpty()) return ""

        val l = digits.length
        val sb = StringBuilder()

        sb.append("(")
        sb.append(digits.substring(0, if (l >= 2) 2 else l))

        if (l < 3) return sb.toString()

        sb.append(") ")
        sb.append(digits[2])

        if (l == 3) return sb.toString()

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

    // ------------- MÁSCARA DE DATA (dd/mm/aaaa) -------------

    private fun setupDateMask() {
        binding.editDataNascimento.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingDate) return

                val current = s?.toString() ?: ""
                val formatted = formatDate(current)

                if (current == formatted) return

                isFormattingDate = true
                binding.editDataNascimento.setText(formatted)
                binding.editDataNascimento.setSelection(formatted.length)
                isFormattingDate = false
            }
        })
    }

    // Converte só números para dd/mm/aaaa com as barras nos lugares certos
    private fun formatDate(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8) // ddMMyyyy
        val l = digits.length

        if (l == 0) return ""

        val sb = StringBuilder()

        for (i in digits.indices) {
            sb.append(digits[i])
            if (i == 1 && l > 2) {
                sb.append("/")
            } else if (i == 3 && l > 4) {
                sb.append("/")
            }
        }

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

    // ------------- SUMIR HINT AO DIGITAR -------------

    private fun setupClearHintOnType() {
        val campos = listOf(
            binding.editNomeCompleto,
            binding.editEmail,
            binding.editTelefone,
            binding.editDataNascimento,
            binding.editPeso,
            binding.editCidade,
            binding.editEstado,
            binding.editSenha,
            binding.editConfirmarSenha
        )

        val hintsOriginais = campos.associateWith { it.hint?.toString() ?: "" }

        campos.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) { }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    val original = hintsOriginais[editText] ?: ""
                    editText.hint = if (s.isNullOrEmpty()) original else ""
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    // ------------- UTILITÁRIOS -------------

    private fun isEmailValido(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isDataValida(dataStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            sdf.isLenient = false // não aceita datas tipo 32/13/2024
            sdf.parse(dataStr)
            true
        } catch (e: Exception) {
            false
        }
    }

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