package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsetsController
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityEditarDadosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Locale

class EditarDadosActivity : BaseActivity() {

    private lateinit var binding: ActivityEditarDadosBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var isFormattingPhone = false
    private var isFormattingDate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Força SOMENTE esta Activity a usar modo CLARO
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO

        binding = ActivityEditarDadosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cores da status bar / nav bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        supportActionBar?.hide()

        setupDropdownSexo()
        setupDropdownTipoSanguineo()
        setupPhoneMask()
        setupDateMask()
        setupButtons()
        carregarDadosAtuais()
    }

    // ---------------- DROPDOWNS ----------------

    private fun setupDropdownSexo() {
        val sexos = listOf("Masculino", "Feminino", "Prefiro não informar")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            sexos
        )
        binding.editSexo.setAdapter(adapter)

        // impede teclado e força abrir a lista
        binding.editSexo.keyListener = null

        binding.editSexo.setOnClickListener {
            binding.editSexo.showDropDown()
        }

        binding.editSexo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.editSexo.showDropDown()
            }
        }
    }

    private fun setupDropdownTipoSanguineo() {
        val tipos = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Não sei")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            tipos
        )
        binding.editTipoSanguineo.setAdapter(adapter)

        // impede teclado e força abrir a lista
        binding.editTipoSanguineo.keyListener = null

        binding.editTipoSanguineo.setOnClickListener {
            binding.editTipoSanguineo.showDropDown()
        }

        binding.editTipoSanguineo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.editTipoSanguineo.showDropDown()
            }
        }
    }

    // ---------------- MÁSCARA DE TELEFONE ----------------

    private fun setupPhoneMask() {
        binding.editTelefone.addTextChangedListener(object : TextWatcher {
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
            ) { }

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

    // ---------------- MÁSCARA DE DATA (dd/mm/aaaa) ----------------

    private fun setupDateMask() {
        binding.editDataNascimento.addTextChangedListener(object : TextWatcher {
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
            ) { }

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

    // converte só números pra dd/mm/aaaa
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

    // ---------------- BOTÕES ----------------

    private fun setupButtons() {
        binding.btnSalvarDados.setOnClickListener {
            salvarDados()
        }

        binding.btnCancelarEdicao.setOnClickListener {
            finish()
        }
    }

    // ---------------- CARREGAR DADOS ATUAIS ----------------

    private fun carregarDadosAtuais() {
        val usuario = auth.currentUser ?: run {
            finish()
            return
        }

        firestore.collection("usuarios")
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val nome = doc.getString("nome") ?: ""
                val telefone = doc.getString("telefone") ?: ""
                val peso = doc.getString("peso") ?: ""
                val cidade = doc.getString("cidade") ?: ""
                val estado = doc.getString("estado") ?: ""
                val tipoSanguineo = doc.getString("tipoSanguineo") ?: ""
                val dataNascimento = doc.getString("dataNascimento") ?: ""
                val sexo = doc.getString("sexo") ?: ""

                binding.editNomeCompleto.setText(nome)
                binding.editTelefone.setText(telefone)
                binding.editPeso.setText(peso)
                binding.editCidade.setText(cidade)
                binding.editEstado.setText(estado)
                binding.editDataNascimento.setText(dataNascimento)

                if (tipoSanguineo.isNotBlank()) {
                    binding.editTipoSanguineo.setText(tipoSanguineo, false)
                }

                if (sexo.isNotBlank()) {
                    val sexoExibicao = when (sexo.lowercase()) {
                        "masculino" -> "Masculino"
                        "feminino" -> "Feminino"
                        else -> "Prefiro não informar"
                    }
                    binding.editSexo.setText(sexoExibicao, false)
                }
            }
    }

    // ---------------- SALVAR (COM VALIDAÇÃO DE DATA) ----------------

    private fun salvarDados() {
        val usuario = auth.currentUser ?: run {
            Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val nome = binding.editNomeCompleto.text.toString().trim()
        val telefone = binding.editTelefone.text.toString().trim()
        val peso = binding.editPeso.text.toString().trim()
        val cidade = binding.editCidade.text.toString().trim()
        val estado = binding.editEstado.text.toString().trim()
        val tipoSanguineoInput = binding.editTipoSanguineo.text.toString().trim()
        val dataNascimento = binding.editDataNascimento.text.toString().trim()
        val sexoExibicao = binding.editSexo.text.toString().trim()

        // Validação da data, se preenchida
        if (dataNascimento.isNotEmpty() && !isDataValida(dataNascimento)) {
            Toast.makeText(
                this,
                "Data de nascimento inválida. Use o formato dd/mm/aaaa.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val updates = mutableMapOf<String, Any>()

        if (nome.isNotEmpty()) updates["nome"] = nome
        if (telefone.isNotEmpty()) updates["telefone"] = telefone
        if (peso.isNotEmpty()) updates["peso"] = peso
        if (cidade.isNotEmpty()) updates["cidade"] = cidade
        if (estado.isNotEmpty()) updates["estado"] = estado
        if (dataNascimento.isNotEmpty()) updates["dataNascimento"] = dataNascimento

        // Normalização do sexo
        if (sexoExibicao.isNotEmpty()) {
            val sexoFirestore = when (sexoExibicao.lowercase()) {
                "masculino" -> "masculino"
                "feminino" -> "feminino"
                else -> "nao_informado"
            }
            updates["sexo"] = sexoFirestore
        }

        // Normalização do tipo sanguíneo
        if (tipoSanguineoInput.isNotEmpty()) {
            if (tipoSanguineoInput.equals("Não sei", ignoreCase = true) ||
                tipoSanguineoInput.equals("Nao sei", ignoreCase = true)
            ) {
                // Se o usuário explicitamente escolheu "Não sei", limpamos no Firestore
                updates["tipoSanguineo"] = ""
            } else {
                updates["tipoSanguineo"] = tipoSanguineoInput.uppercase(Locale.getDefault())
            }
        }

        if (updates.isEmpty()) {
            Toast.makeText(
                this,
                "Nenhuma informação para atualizar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.btnSalvarDados.isEnabled = false

        firestore.collection("usuarios")
            .document(usuario.uid)
            // set + merge garante que cria o doc se não existir e atualiza apenas esses campos
            .set(updates as Map<String, Any>, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Dados atualizados com sucesso.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnSalvarDados.isEnabled = true
                Toast.makeText(
                    this,
                    "Erro ao atualizar: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ---------------- VALIDAÇÃO DE DATA ----------------

    private fun isDataValida(dataStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            sdf.isLenient = false
            sdf.parse(dataStr)
            true
        } catch (e: Exception) {
            false
        }
    }
}