package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityAgendamentoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Agendamento : AppCompatActivity() {

    private lateinit var binding: ActivityAgendamentoBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendamentoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        supportActionBar?.hide()

        configurarPickers()
        setupListeners()
    }

    private fun configurarPickers() {
        // Impede escolher datas passadas
        val hoje = Calendar.getInstance()
        binding.datePicker.minDate = hoje.timeInMillis

        // Formato 24h no relógio
        binding.timePicker.setIs24HourView(true)
    }

    private fun setupListeners() {
        // Botão de confirmar agendamento → abre dialog de confirmação
        binding.btAgendar.setOnClickListener {
            confirmarAgendamento()
        }

        // Botão "Voltar para Serviços"
        binding.btVoltarServicos.setOnClickListener {
            finish()
        }

        // Permitir apenas um hemocentro selecionado
        binding.hemopeCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.hcCheckbox.isChecked = false
                binding.imipCheckbox.isChecked = false
            }
        }

        binding.hcCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.hemopeCheckbox.isChecked = false
                binding.imipCheckbox.isChecked = false
            }
        }

        binding.imipCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.hemopeCheckbox.isChecked = false
                binding.hcCheckbox.isChecked = false
            }
        }
    }

    /**
     * Monta o resumo do agendamento e exibe um diálogo profissional de confirmação.
     */
    private fun confirmarAgendamento() {
        val usuario = auth.currentUser
        if (usuario == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val hemocentro = when {
            binding.hemopeCheckbox.isChecked -> "HEMOPE Recife"
            binding.hcCheckbox.isChecked -> "Hospital das Clínicas"
            binding.imipCheckbox.isChecked -> "IMIP"
            else -> null
        }

        if (hemocentro == null) {
            mostrarSnackbar("Selecione um hemocentro para continuar.", "#FF0000")
            return
        }

        val dia = binding.datePicker.dayOfMonth
        val mes = binding.datePicker.month + 1
        val ano = binding.datePicker.year
        val dataFormatada = String.format("%02d/%02d/%04d", dia, mes, ano)

        val hora: Int
        val minuto: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hora = binding.timePicker.hour
            minuto = binding.timePicker.minute
        } else {
            @Suppress("DEPRECATION")
            hora = binding.timePicker.currentHour
            @Suppress("DEPRECATION")
            minuto = binding.timePicker.currentMinute
        }
        val horaFormatada = String.format("%02d:%02d", hora, minuto)

        val mensagem = buildString {
            appendLine("Confira os dados do seu agendamento:")
            appendLine()
            appendLine("🏥 $hemocentro")
            appendLine("📅 $dataFormatada às $horaFormatada")
            appendLine()
            append("Deseja confirmar este agendamento?")
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar agendamento")
            .setMessage(mensagem)
            .setCancelable(false)
            .setNegativeButton("Editar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                salvarAgendamento(hemocentro, dataFormatada, horaFormatada)
            }
            .create()

        dialog.setOnShowListener {
            estilizarBotoesDialog(dialog, isDestructive = false)
        }

        dialog.show()
    }

    /**
     * Salva o agendamento no Firestore.
     */
    private fun salvarAgendamento(
        hemocentro: String,
        dataFormatada: String,
        horaFormatada: String
    ) {
        val usuario = auth.currentUser
        if (usuario == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        // Endereço/telefone prontos pra serem exibidos em "Meus Agendamentos"
        val endereco = when (hemocentro) {
            "HEMOPE Recife" -> "R. Joaquim Nabuco, 171 - Graças, Recife - PE"
            "Hospital das Clínicas" -> "Av. Prof. Moraes Rego, 1235 - Cidade Universitária, Recife - PE"
            "IMIP" -> "R. dos Coelhos, 300 - Boa Vista, Recife - PE"
            else -> ""
        }

        val telefone = when (hemocentro) {
            "HEMOPE Recife" -> "(81) 3182-4600"
            "Hospital das Clínicas" -> "(81) 2126-3900"
            "IMIP" -> "(81) 2122-4100"
            else -> ""
        }

        val agendamento = hashMapOf(
            "hemocentro" to hemocentro,
            "data" to dataFormatada,
            "hora" to horaFormatada,
            "endereco" to endereco,
            "telefone" to telefone,
            "criadoEm" to FieldValue.serverTimestamp(),
            "status" to "Pendente"
        )

        firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .add(agendamento)
            .addOnSuccessListener {
                val dialog = MaterialAlertDialogBuilder(this)
                    .setTitle("Agendamento confirmado")
                    .setMessage(
                        "Seu agendamento foi registrado com sucesso! 🎉\n\n" +
                                "Você poderá consultá-lo em:\n" +
                                "📅 Meus Agendamentos."
                    )
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .create()

                dialog.setOnShowListener {
                    estilizarBotoesDialog(dialog, isDestructive = false)
                }

                dialog.show()
            }
            .addOnFailureListener { e ->
                val dialog = MaterialAlertDialogBuilder(this)
                    .setTitle("Erro ao agendar")
                    .setMessage(
                        "Não foi possível salvar seu agendamento no momento.\n\n" +
                                "Tente novamente em instantes.\n\n" +
                                "Detalhes técnicos:\n${e.localizedMessage}"
                    )
                    .setPositiveButton("OK", null)
                    .create()

                dialog.setOnShowListener {
                    estilizarBotoesDialog(dialog, isDestructive = false)
                }

                dialog.show()
            }
    }

    /**
     * Snackbar no padrão do app (fundo colorido, texto branco)
     */
    private fun mostrarSnackbar(mensagem: String, corHex: String) {
        val snackbar = Snackbar.make(binding.root, mensagem, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(android.graphics.Color.parseColor(corHex))
        snackbar.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        snackbar.show()
    }

    /**
     * Deixa os botões do diálogo no padrão do app:
     * - Botão positivo: fundo vermelho, texto branco, negrito
     * - Botão negativo: texto vermelho, sem fundo colorido
     */
    private fun estilizarBotoesDialog(dialog: AlertDialog, isDestructive: Boolean) {
        val primaryColor = ContextCompat.getColor(this, R.color.vermelho_primario)
        val white = ContextCompat.getColor(this, android.R.color.white)

        val botaoPositivo = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val botaoNegativo = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        botaoPositivo?.apply {
            setBackgroundColor(primaryColor)
            setTextColor(white)
            textSize = 14f
            isAllCaps = false
            // padding pra ficar mais “botão” mesmo
            setPadding(40, 10, 40, 10)
        }

        botaoNegativo?.apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(primaryColor)
            textSize = 14f
            isAllCaps = false
        }
    }
}