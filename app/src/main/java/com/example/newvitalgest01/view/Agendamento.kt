package com.example.newvitalgest01.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.databinding.ActivityAgendamentoBinding
import com.example.newvitalgest01.databinding.DialogConfirmacaoAgendamentoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Agendamento : AppCompatActivity() {

    private lateinit var binding: ActivityAgendamentoBinding

    private val calendar: Calendar = Calendar.getInstance()
    private var dataSelecionada: String = ""
    private var horaSelecionada: String = ""
    private var hemocentroSelecionado: String? = null

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val hemocentros = mapOf(
        "HEMOPE Recife" to mapOf(
            "endereco" to "Rua Joaquim Nabuco, 171 - Graças",
            "telefone" to "(81) 3416-4800",
            "horario" to "Seg-Sex: 7h30-18h30"
        ),
        "Hospital das Clínicas" to mapOf(
            "endereco" to "Av. Prof. Moraes Rego, 1235 - Cidade Universitária",
            "telefone" to "(81) 2126-3600",
            "horario" to "Seg-Sex: 7h-16h"
        ),
        "IMIP" to mapOf(
            "endereco" to "Rua dos Coelhos, 300 - Boa Vista",
            "telefone" to "(81) 2122-4700",
            "horario" to "Seg-Sex: 7h-17h"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        inicializarDataHora()
        configurarInformacoesHemocentros()
        configurarDataHoraListeners()
        configurarSelecaoHemocentros()
        configurarBotoes()
    }

    // ---------------- BOTÕES ----------------

    private fun configurarBotoes() {
        // Confirmar agendamento
        binding.btAgendar.setOnClickListener {
            validarEAgendar()
        }

        // Voltar para serviços (Home) sem agendar
        binding.btVoltarServicos.setOnClickListener {
            navegarParaHome()
        }

        // Verificar elegibilidade
        binding.btVerificarElegibilidade.setOnClickListener {
            startActivity(Intent(this, ElegibilidadeActivity::class.java))
        }
    }

    private fun validarEAgendar() {
        binding.btAgendar.isEnabled = true

        when {
            dataSelecionada.isEmpty() -> {
                mostrarToast("Selecione uma data!", irParaHome = false)
                return
            }
            horaSelecionada.isEmpty() -> {
                mostrarToast("Selecione um horário!", irParaHome = false)
                return
            }
            hemocentroSelecionado == null -> {
                mostrarToast("Selecione um hemocentro!", irParaHome = false)
                return
            }
            !isHorarioFuncionamentoValido(horaSelecionada, hemocentroSelecionado) -> {
                val horario = getHorarioFuncionamento(hemocentroSelecionado)
                mostrarToast(
                    "Hemocentro fechado! Horário de funcionamento: $horario",
                    irParaHome = false
                )
                return
            }
            else -> {
                // Desabilita botão para evitar múltiplos cliques
                binding.btAgendar.isEnabled = false

                val info = hemocentros[hemocentroSelecionado]
                val endereco = info?.get("endereco") ?: ""
                val telefone = info?.get("telefone") ?: ""

                // Mostra tela de confirmação no padrão do app
                mostrarDialogoConfirmacaoAgendamento(
                    hemocentroSelecionado!!,
                    dataSelecionada,
                    horaSelecionada,
                    endereco,
                    telefone
                )

                // Salva no Firebase em paralelo (não trava a UI)
                salvarAgendamentoNoFirebase(
                    hemocentroSelecionado!!,
                    dataSelecionada,
                    horaSelecionada
                )
            }
        }
    }

    // ---------------- DATA / HORA ----------------

    private fun configurarDataHoraListeners() {
        binding.datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ) { _, year, monthOfYear, dayOfMonth ->
            val dia = dayOfMonth.toString().padStart(2, '0')
            val mes = (monthOfYear + 1).toString().padStart(2, '0')
            dataSelecionada = "$dia/$mes/$year"
        }

        binding.timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            val h = hourOfDay.toString().padStart(2, '0')
            val m = minute.toString().padStart(2, '0')
            horaSelecionada = "$h:$m"
        }

        binding.timePicker.setIs24HourView(true)
    }

    private fun inicializarDataHora() {
        val dia = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val mes = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val ano = calendar.get(Calendar.YEAR)
        dataSelecionada = "$dia/$mes/$ano"

        val horaAtual = calendar.get(Calendar.HOUR_OF_DAY)
        val minutoAtual = calendar.get(Calendar.MINUTE)
        horaSelecionada =
            "${horaAtual.toString().padStart(2, '0')}:${minutoAtual.toString().padStart(2, '0')}"
    }

    // ---------------- HEMOCENTROS ----------------

    private fun configurarInformacoesHemocentros() {
        binding.txtInfoHemope.text =
            "HEMOPE Recife\n${hemocentros["HEMOPE Recife"]?.get("endereco")}\n${hemocentros["HEMOPE Recife"]?.get("telefone")}"

        binding.txtInfoHc.text =
            "Hospital das Clínicas\n${hemocentros["Hospital das Clínicas"]?.get("endereco")}\n${hemocentros["Hospital das Clínicas"]?.get("telefone")}"

        binding.txtInfoImip.text =
            "IMIP\n${hemocentros["IMIP"]?.get("endereco")}\n${hemocentros["IMIP"]?.get("telefone")}"
    }

    private fun configurarSelecaoHemocentros() {
        val checkBoxes = listOf(
            binding.hemopeCheckbox,
            binding.hcCheckbox,
            binding.imipCheckbox
        )

        checkBoxes.forEach { cb ->
            cb.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    checkBoxes.filter { it != buttonView }.forEach { it.isChecked = false }
                    hemocentroSelecionado = when (buttonView.id) {
                        binding.hemopeCheckbox.id -> "HEMOPE Recife"
                        binding.hcCheckbox.id -> "Hospital das Clínicas"
                        binding.imipCheckbox.id -> "IMIP"
                        else -> null
                    }
                } else if (hemocentroSelecionado == when (buttonView.id) {
                        binding.hemopeCheckbox.id -> "HEMOPE Recife"
                        binding.hcCheckbox.id -> "Hospital das Clínicas"
                        binding.imipCheckbox.id -> "IMIP"
                        else -> null
                    }
                ) {
                    hemocentroSelecionado = null
                }
            }
        }
    }

    // ---------------- FIREBASE ----------------

    private fun salvarAgendamentoNoFirebase(
        hemocentro: String,
        data: String,
        hora: String
    ) {
        val user = auth.currentUser
        if (user == null) {
            mostrarToast("Usuário não autenticado. Faça login novamente.", irParaHome = false)
            return
        }

        val agendamento = hashMapOf(
            "hemocentro" to hemocentro,
            "data" to data,
            "hora" to hora,
            "criadoEm" to FieldValue.serverTimestamp(),
            "status" to "Agendado"
        )

        firestore.collection("usuarios")
            .document(user.uid)
            .collection("agendamentos")
            .add(agendamento)
            .addOnSuccessListener {
                atualizarProximoAgendamento(user.uid)
            }
            .addOnFailureListener {
                mostrarToast(
                    "Não foi possível sincronizar o agendamento com o servidor. Verifique sua conexão.",
                    irParaHome = false
                )
            }
    }

    private fun atualizarProximoAgendamento(uid: String) {
        firestore.collection("usuarios")
            .document(uid)
            .collection("agendamentos")
            .orderBy("data")
            .orderBy("hora")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val agendamento = documents.documents[0]
                    val hemocentro = agendamento.getString("hemocentro") ?: ""
                    val data = agendamento.getString("data") ?: ""
                    val hora = agendamento.getString("hora") ?: ""

                    val proximoAgendamento = hashMapOf(
                        "proximoHemocentro" to hemocentro,
                        "proximaData" to data,
                        "proximaHora" to hora,
                        "atualizadoEm" to FieldValue.serverTimestamp()
                    )

                    firestore.collection("usuarios")
                        .document(uid)
                        .collection("info")
                        .document("proximo_agendamento")
                        .set(proximoAgendamento)
                }
            }
    }

    // ---------------- REGRAS DE HORÁRIO ----------------

    private fun isHorarioFuncionamentoValido(hora: String, hemocentro: String?): Boolean {
        if (hora.isEmpty() || hemocentro == null) return false
        val partes = hora.split(":")
        val horas = partes.getOrNull(0)?.toIntOrNull() ?: return false
        val minutos = partes.getOrNull(1)?.toIntOrNull() ?: 0

        return when (hemocentro) {
            "HEMOPE Recife" -> (horas in 7..17) || (horas == 18 && minutos <= 30)
            "Hospital das Clínicas" -> horas in 7..15 || (horas == 16 && minutos == 0)
            "IMIP" -> horas in 7..16 || (horas == 17 && minutos == 0)
            else -> false
        }
    }

    private fun getHorarioFuncionamento(hemocentro: String?): String {
        return hemocentros[hemocentro]?.get("horario") ?: "Horário não disponível"
    }

    // ---------------- UI HELPERS ----------------

    private fun mostrarToast(
        mensagem: String,
        irParaHome: Boolean
    ) {
        runOnUiThread {
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()

            if (irParaHome) {
                Handler(Looper.getMainLooper()).postDelayed({
                    navegarParaHome()
                }, 2500)
            }
        }
    }

    private fun mostrarDialogoConfirmacaoAgendamento(
        hemocentro: String,
        data: String,
        hora: String,
        endereco: String,
        telefone: String
    ) {
        // Usa ViewBinding do layout do diálogo
        val dialogBinding = DialogConfirmacaoAgendamentoBinding.inflate(layoutInflater)

        dialogBinding.txtTituloConfirmacao.text = "Agendamento confirmado!"
        dialogBinding.txtHemocentroValor.text = hemocentro
        dialogBinding.txtDataValor.text = data
        dialogBinding.txtHoraValor.text = hora
        dialogBinding.txtEnderecoValor.text = endereco
        dialogBinding.txtTelefoneValor.text = telefone

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.btIrParaServicos.setOnClickListener {
            dialog.dismiss()
            navegarParaHome()
        }

        dialog.show()
    }

    private fun navegarParaHome() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }
}