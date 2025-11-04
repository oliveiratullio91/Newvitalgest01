package com.example.newvitalgest01.view

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.newvitalgest01.databinding.ActivityAgendamentoBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Agendamento : AppCompatActivity() {

    private lateinit var binding: ActivityAgendamentoBinding
    private val calendar: Calendar = Calendar.getInstance()
    private var data: String = ""
    private var hora: String = ""
    private var hemocentroSelecionado: String? = null

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Dados dos hemocentros
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendamentoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        inicializarDataHora()
        configurarInformacoesHemocentros()

        binding.datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ) { _, year, monthOfYear, dayOfMonth ->
            val dia = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()
            val mes = if (monthOfYear < 9) "0${monthOfYear + 1}" else (monthOfYear + 1).toString()
            data = "$dia/$mes/$year"
        }

        binding.timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            val horaFormatada = if (hourOfDay < 10) "0$hourOfDay" else hourOfDay.toString()
            val minuto = if (minute < 10) "0$minute" else minute.toString()
            hora = "$horaFormatada:$minuto"
        }
        binding.timePicker.setIs24HourView(true)

        configurarSelecaoUnicaHemocentros()

        binding.btAgendar.setOnClickListener {
            when {
                data.isEmpty() -> {
                    mostrarMensagem(it, "Selecione uma data!", "#FF5252", false)
                }
                hora.isEmpty() -> {
                    mostrarMensagem(it, "Selecione um horário!", "#FF5252", false)
                }
                hemocentroSelecionado == null -> {
                    mostrarMensagem(it, "Selecione um hemocentro!", "#FF5252", false)
                }
                !isHorarioFuncionamentoValido(hora, hemocentroSelecionado) -> {
                    val horario = getHorarioFuncionamento(hemocentroSelecionado)
                    mostrarMensagem(
                        it,
                        "Hemocentro fechado! Horário de funcionamento: $horario",
                        "#FF5252",
                        false
                    )
                }
                else -> {
                    val infoHemocentro = hemocentros[hemocentroSelecionado]
                    val endereco = infoHemocentro?.get("endereco") ?: ""
                    val telefone = infoHemocentro?.get("telefone") ?: ""

                    val mensagemSucesso = """
                        ✅ Agendamento confirmado!

                        📍 $hemocentroSelecionado
                        🗓️ Data: $data
                        ⏰ Hora: $hora
                        📞 $telefone
                        🏠 $endereco

                        Seu agendamento foi salvo na sua conta.
                    """.trimIndent()

                    salvarAgendamentoNoFirebase(
                        hemocentroSelecionado!!,
                        data,
                        hora
                    ) { sucesso ->
                        if (sucesso) {
                            mostrarMensagem(it, mensagemSucesso, "#4CAF50", true)
                        } else {
                            mostrarMensagem(
                                it,
                                "Erro ao salvar agendamento. Tente novamente.",
                                "#FF5252",
                                false
                            )
                        }
                    }
                }
            }
        }

        binding.btVerificarElegibilidade.setOnClickListener {
            val intent = Intent(this, ElegibilidadeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun configurarInformacoesHemocentros() {
        binding.txtInfoHemope.text =
            "HEMOPE Recife\n${hemocentros["HEMOPE Recife"]?.get("endereco")}\n${hemocentros["HEMOPE Recife"]?.get("telefone")}"

        binding.txtInfoHc.text =
            "Hospital das Clínicas\n${hemocentros["Hospital das Clínicas"]?.get("endereco")}\n${hemocentros["Hospital das Clínicas"]?.get("telefone")}"

        binding.txtInfoImip.text =
            "IMIP\n${hemocentros["IMIP"]?.get("endereco")}\n${hemocentros["IMIP"]?.get("telefone")}"
    }

    private fun configurarSelecaoUnicaHemocentros() {
        val checkBoxes: List<CheckBox> = listOf(
            binding.hemopeCheckbox,
            binding.hcCheckbox,
            binding.imipCheckbox
        )

        val onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                checkBoxes.forEach { checkbox ->
                    if (checkbox != buttonView) {
                        checkbox.isChecked = false
                    }
                }

                hemocentroSelecionado = when (buttonView.id) {
                    binding.hemopeCheckbox.id -> "HEMOPE Recife"
                    binding.hcCheckbox.id -> "Hospital das Clínicas"
                    binding.imipCheckbox.id -> "IMIP"
                    else -> null
                }
            } else {
                val hemocentroDoBotao = when (buttonView.id) {
                    binding.hemopeCheckbox.id -> "HEMOPE Recife"
                    binding.hcCheckbox.id -> "Hospital das Clínicas"
                    binding.imipCheckbox.id -> "IMIP"
                    else -> null
                }
                if (hemocentroSelecionado == hemocentroDoBotao) {
                    hemocentroSelecionado = null
                }
            }
        }

        checkBoxes.forEach { checkbox ->
            checkbox.setOnCheckedChangeListener(onCheckedChangeListener)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun inicializarDataHora() {
        val diaAtual = calendar.get(Calendar.DAY_OF_MONTH)
        val mesAtual = calendar.get(Calendar.MONTH)
        val anoAtual = calendar.get(Calendar.YEAR)

        val dia = if (diaAtual < 10) "0$diaAtual" else diaAtual.toString()
        val mes = if (mesAtual < 9) "0${mesAtual + 1}" else (mesAtual + 1).toString()
        data = "$dia/$mes/$anoAtual"

        val horaAtual = calendar.get(Calendar.HOUR_OF_DAY)
        val minutoAtual = calendar.get(Calendar.MINUTE)

        val (horaPadrao, minutoPadrao) = if (horaAtual in 7..18) {
            Pair(horaAtual, minutoAtual)
        } else {
            Pair(9, 0)
        }

        val horaFormatada = if (horaPadrao < 10) "0$horaPadrao" else horaPadrao.toString()
        val minuto = if (minutoPadrao < 10) "0$minutoPadrao" else minutoPadrao.toString()
        hora = "$horaFormatada:$minuto"
    }

    private fun isHorarioFuncionamentoValido(hora: String, hemocentro: String?): Boolean {
        if (hora.isEmpty() || hemocentro == null) return false

        return try {
            val partes = hora.split(":")
            val horas = partes[0].toInt()
            val minutos = partes[1].toInt()

            when (hemocentro) {
                "HEMOPE Recife" -> horas in 7..18 || (horas == 18 && minutos <= 30)
                "Hospital das Clínicas" -> horas in 7..16
                "IMIP" -> horas in 7..17
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getHorarioFuncionamento(hemocentro: String?): String {
        return hemocentros[hemocentro]?.get("horario") ?: "Horário não disponível"
    }

    private fun salvarAgendamentoNoFirebase(
        hemocentro: String,
        data: String,
        hora: String,
        callback: (Boolean) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            callback(false)
            return
        }

        val agendamento = hashMapOf(
            "hemocentro" to hemocentro,
            "data" to data,
            "hora" to hora,
            "criadoEm" to FieldValue.serverTimestamp()
        )

        firestore.collection("usuarios")
            .document(user.uid)
            .collection("agendamentos")
            .add(agendamento)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun mostrarMensagem(view: View, mensagem: String, cor: String, navegarParaHome: Boolean) {
        val snackbar = Snackbar.make(view, mensagem, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(Color.parseColor(cor))
        snackbar.setTextColor(Color.WHITE)
        snackbar.show()

        if (navegarParaHome) {
            Handler(Looper.getMainLooper()).postDelayed({
                navegarParaHome()
            }, 4000)
        }
    }

    private fun navegarParaHome() {
        val intent = Intent(this, Home::class.java)
        startActivity(intent)
        finish()
    }
}