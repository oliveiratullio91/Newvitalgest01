package com.example.newvitalgest01.view

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityAgendamentoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Hemocentro(
    val id: String,
    val nome: String,
    val cidade: String,
    val uf: String,
    val regiao: String,
    val telefone: String?,
    val logradouro: String?,
    val status: String?,
    val cep: String?,
    val numero: String?,
    val horarioFuncionamento: String?,
    val latitude: Double?,
    val longitude: Double?,
    val distanciaKm: Double?
)

class Agendamento : BaseActivity() {

    private lateinit var binding: ActivityAgendamentoBinding

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val todosHemocentros = mutableListOf<Hemocentro>()
    private var hemocentroSelecionado: Hemocentro? = null

    private var userLat: Double? = null
    private var userLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendamentoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        supportActionBar?.hide()

        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // localização opcional vinda da intent (para distância)
        val latExtra = intent.getDoubleExtra("userLat", Double.NaN)
        val lngExtra = intent.getDoubleExtra("userLng", Double.NaN)
        userLat = if (latExtra.isNaN()) null else latExtra
        userLng = if (lngExtra.isNaN()) null else lngExtra

        configurarPickers()
        configurarSpinners()
        configurarCliques()

        // card de informações do hemocentro começa escondido
        binding.cardHemocentroInfo.visibility = View.GONE
        desabilitarBotaoAgendar("Selecione um hemocentro para continuar")

        carregarHemocentros()
    }

    // ---------------- Date & Time ----------------

    private fun configurarPickers() {
        val hoje = Calendar.getInstance()
        binding.datePicker.minDate = hoje.timeInMillis
        binding.timePicker.setIs24HourView(true)
    }

    // ---------------- Adapters de Spinner ----------------

    private fun criarAdapter(lista: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            lista
        ).also {
            it.setDropDownViewResource(R.layout.item_spinner_dropdown_text)
        }
    }

    // ---------------- Spinners ----------------

    private fun configurarSpinners() {
        // Estados iniciais "carregando"
        binding.spinnerRegiao.adapter = criarAdapter(listOf("Carregando regiões..."))
        binding.spinnerUf.adapter = criarAdapter(listOf("Selecione a região"))
        binding.spinnerCidade.adapter = criarAdapter(listOf("Selecione o estado"))
        binding.spinnerHemocentro.adapter = criarAdapter(listOf("Selecione a cidade"))

        // Região
        binding.spinnerRegiao.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val regiao = parent?.getItemAtPosition(position) as String
                    if (regiao == "Carregando regiões..." || regiao == "Todas as regiões") return
                    atualizarSpinnerUf(regiao)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // UF
        binding.spinnerUf.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val uf = parent?.getItemAtPosition(position) as String
                    if (uf == "Selecione a região" || uf == "Todos os estados") return
                    val regiaoSelecionada = binding.spinnerRegiao.selectedItem as? String
                    atualizarSpinnerCidade(regiaoSelecionada, uf)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Cidade
        binding.spinnerCidade.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val cidade = parent?.getItemAtPosition(position) as String
                    if (cidade == "Selecione o estado" || cidade == "Todas as cidades") return
                    val ufSelecionada = binding.spinnerUf.selectedItem as? String
                    val regiaoSelecionada = binding.spinnerRegiao.selectedItem as? String
                    atualizarSpinnerHemocentro(regiaoSelecionada, ufSelecionada, cidade)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Hemocentro
        binding.spinnerHemocentro.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val nomeHemocentro = parent?.getItemAtPosition(position) as String
                    if (nomeHemocentro.startsWith("Selecione") ||
                        nomeHemocentro.startsWith("Nenhum hemocentro")
                    ) {
                        hemocentroSelecionado = null
                        binding.cardHemocentroInfo.visibility = View.GONE
                        desabilitarBotaoAgendar("Selecione um hemocentro para continuar")
                        return
                    }

                    val selecionado = todosHemocentros.find { it.nome == nomeHemocentro }
                    hemocentroSelecionado = selecionado

                    if (selecionado != null) {
                        binding.cardHemocentroInfo.visibility = View.VISIBLE

                        val endereco = buildString {
                            append(selecionado.logradouro ?: "")
                            if (!selecionado.numero.isNullOrBlank() && selecionado.numero != "S/N") {
                                if (isNotEmpty()) append(", ")
                                append(selecionado.numero)
                            }
                            if (selecionado.cidade.isNotBlank() && selecionado.uf.isNotBlank()) {
                                if (isNotEmpty()) append(" - ")
                                append("${selecionado.cidade}/${selecionado.uf}")
                            }
                        }

                        binding.txtNomeHemo.text = selecionado.nome
                        binding.txtEnderecoHemo.text =
                            endereco.ifBlank { "Endereço não informado" }
                        binding.txtTelefoneHemo.text =
                            selecionado.telefone ?: "Telefone não informado"
                        binding.txtStatusHemo.text =
                            selecionado.status ?: "Status não informado"
                        binding.txtHorarioHemo.text =
                            selecionado.horarioFuncionamento ?: "Horário não informado"

                        val distancia = selecionado.distanciaKm
                        if (distancia != null) {
                            binding.txtDistanciaHemo.visibility = View.VISIBLE
                            binding.txtDistanciaHemo.text = String.format(
                                Locale("pt", "BR"),
                                "%.1f km de você",
                                distancia
                            )
                        } else {
                            binding.txtDistanciaHemo.visibility = View.GONE
                        }

                        habilitarBotaoAgendar()
                    } else {
                        binding.cardHemocentroInfo.visibility = View.GONE
                        desabilitarBotaoAgendar("Selecione um hemocentro para continuar")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun atualizarSpinnerRegiao() {
        val regioes = todosHemocentros
            .map { it.regiao }
            .distinct()
            .sorted()

        if (regioes.isEmpty()) {
            binding.spinnerRegiao.adapter =
                criarAdapter(listOf("Nenhum hemocentro disponível"))
            binding.spinnerUf.adapter =
                criarAdapter(listOf("—"))
            binding.spinnerCidade.adapter =
                criarAdapter(listOf("—"))
            binding.spinnerHemocentro.adapter =
                criarAdapter(listOf("Nenhum hemocentro encontrado"))
            desabilitarBotaoAgendar("Nenhum hemocentro disponível no momento")
            return
        }

        binding.spinnerRegiao.adapter =
            criarAdapter(listOf("Todas as regiões") + regioes)
    }

    private fun atualizarSpinnerUf(regiaoSelecionada: String) {
        val ufs = todosHemocentros
            .filter { it.regiao == regiaoSelecionada }
            .map { it.uf }
            .distinct()
            .sorted()

        if (ufs.isEmpty()) {
            binding.spinnerUf.adapter =
                criarAdapter(listOf("Nenhum estado disponível"))
            binding.spinnerCidade.adapter =
                criarAdapter(listOf("—"))
            binding.spinnerHemocentro.adapter =
                criarAdapter(listOf("Nenhum hemocentro encontrado"))
            desabilitarBotaoAgendar("Nenhum hemocentro disponível para essa região")
            return
        }

        binding.spinnerUf.adapter =
            criarAdapter(listOf("Todos os estados") + ufs)
    }

    private fun atualizarSpinnerCidade(regiaoSelecionada: String?, ufSelecionada: String?) {
        if (regiaoSelecionada == null || ufSelecionada == null) return

        val cidades = todosHemocentros
            .filter { it.regiao == regiaoSelecionada && it.uf == ufSelecionada }
            .map { it.cidade }
            .distinct()
            .sorted()

        if (cidades.isEmpty()) {
            binding.spinnerCidade.adapter =
                criarAdapter(listOf("Nenhuma cidade disponível"))
            binding.spinnerHemocentro.adapter =
                criarAdapter(listOf("Nenhum hemocentro encontrado"))
            desabilitarBotaoAgendar("Nenhum hemocentro disponível para esse filtro")
            return
        }

        binding.spinnerCidade.adapter =
            criarAdapter(listOf("Todas as cidades") + cidades)
    }

    private fun atualizarSpinnerHemocentro(
        regiaoSelecionada: String?,
        ufSelecionada: String?,
        cidadeSelecionada: String?
    ) {
        if (regiaoSelecionada == null || ufSelecionada == null || cidadeSelecionada == null) return

        val hemocentrosFiltrados = todosHemocentros
            .filter {
                it.regiao == regiaoSelecionada &&
                        it.uf == ufSelecionada &&
                        it.cidade == cidadeSelecionada
            }
            .sortedBy { it.nome.lowercase(Locale("pt", "BR")) }

        val nomes = hemocentrosFiltrados.map { it.nome }

        if (nomes.isEmpty()) {
            binding.spinnerHemocentro.adapter =
                criarAdapter(listOf("Nenhum hemocentro encontrado"))
            hemocentroSelecionado = null
            binding.cardHemocentroInfo.visibility = View.GONE
            desabilitarBotaoAgendar("Nenhum hemocentro disponível para esse filtro")
        } else {
            binding.spinnerHemocentro.adapter =
                criarAdapter(listOf("Selecione o hemocentro") + nomes)
        }
    }

    // ---------------- Botões ----------------

    private fun configurarCliques() {
        binding.btAgendar.setOnClickListener {
            realizarAgendamento()
        }

        binding.btVoltarServicos.setOnClickListener {
            finish()
        }
    }

    private fun desabilitarBotaoAgendar(motivo: String? = null) {
        binding.btAgendar.isEnabled = false
        binding.btAgendar.alpha = 0.5f
        motiveToSnackbar(motivo)
    }

    private fun habilitarBotaoAgendar() {
        binding.btAgendar.isEnabled = true
        binding.btAgendar.alpha = 1f
    }

    private fun motiveToSnackbar(motivo: String?) {
        if (motivo.isNullOrBlank()) return
        mostrarSnackbar(motivo, "#9E9E9E")
    }

    private fun realizarAgendamento() {
        val usuario = auth.currentUser
        if (usuario == null) {
            mostrarSnackbar("⚠ Você precisa estar autenticado para agendar.", "#FF0000")
            return
        }

        val hemocentro = hemocentroSelecionado
        if (hemocentro == null) {
            mostrarSnackbar("Selecione um hemocentro para continuar.", "#FF0000")
            return
        }

        val calendario = Calendar.getInstance()
        val ano = binding.datePicker.year
        val mes = binding.datePicker.month
        val dia = binding.datePicker.dayOfMonth
        val hora = binding.timePicker.hour
        val minuto = binding.timePicker.minute

        calendario.set(ano, mes, dia, hora, minuto, 0)

        val dataHoraAgendada = calendario.time
        val agora = Date()

        if (dataHoraAgendada.before(agora)) {
            mostrarSnackbar("Escolha uma data e hora futuras para o agendamento.", "#FF0000")
            return
        }

        val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val formatoHora = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

        val dataFormatada = formatoData.format(dataHoraAgendada)
        val horaFormatada = formatoHora.format(dataHoraAgendada)

        // Mostra um resumo antes de salvar
        mostrarDialogConfirmacao(hemocentro, dataFormatada, horaFormatada)
    }

    // ---------------- Diálogo de Confirmação ----------------

    private fun mostrarDialogConfirmacao(
        hemocentro: Hemocentro,
        dataFormatada: String,
        horaFormatada: String
    ) {
        val enderecoResumo = buildString {
            append(hemocentro.logradouro ?: "")
            if (!hemocentro.numero.isNullOrBlank() && hemocentro.numero != "S/N") {
                if (isNotEmpty()) append(", ")
                append(hemocentro.numero)
            }
            if (hemocentro.cidade.isNotBlank() && hemocentro.uf.isNotBlank()) {
                if (isNotEmpty()) append(" - ")
                append("${hemocentro.cidade}/${hemocentro.uf}")
            }
        }

        val mensagem = """
            Confira os dados do seu agendamento:
            
            🏥 Hemocentro:
            ${hemocentro.nome}
            
            📍 Endereço:
            ${if (enderecoResumo.isBlank()) "Não informado" else enderecoResumo}
            
            📅 Data: $dataFormatada
            ⏰ Horário: $horaFormatada
            
            Deseja confirmar o agendamento?
        """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle("Confirmar agendamento")
            .setMessage(mensagem)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { d, _ ->
                salvarAgendamentoNoFirestore(hemocentro, dataFormatada, horaFormatada)
                d.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val botaoConfirmar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val botaoCancelar = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            botaoConfirmar?.setTextColor(
                ContextCompat.getColor(this, R.color.vermelho_primario)
            )
            botaoCancelar?.setTextColor(
                ContextCompat.getColor(this, R.color.vermelho_primario)
            )
        }

        dialog.show()
    }

    // ---------------- Salvar no Firestore ----------------

    private fun salvarAgendamentoNoFirestore(
        hemocentro: Hemocentro,
        dataFormatada: String,
        horaFormatada: String
    ) {
        val usuario = auth.currentUser ?: run {
            mostrarSnackbar("Erro: usuário não autenticado.", "#FF0000")
            return
        }

        val agendamento = hashMapOf(
            "hemocentroId" to hemocentro.id,
            "hemocentro" to hemocentro.nome,
            "data" to dataFormatada,
            "hora" to horaFormatada,
            "cidade" to hemocentro.cidade,
            "uf" to hemocentro.uf,
            "regiao" to hemocentro.regiao,
            "endereco" to hemocentro.logradouro,
            "numero" to hemocentro.numero,
            "cep" to hemocentro.cep,
            "telefone" to hemocentro.telefone,
            "horarioFuncionamento" to hemocentro.horarioFuncionamento,
            "statusHemocentro" to hemocentro.status,
            "latitude" to hemocentro.latitude,
            "longitude" to hemocentro.longitude,
            "distanciaKm" to hemocentro.distanciaKm,
            "status" to "Pendente",
            "criadoEm" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        desabilitarBotaoAgendar()
        mostrarSnackbar("Salvando agendamento...", "#9E9E9E")

        firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .add(agendamento)
            .addOnSuccessListener {
                habilitarBotaoAgendar()
                mostrarDialogAgendamentoSucesso()
            }
            .addOnFailureListener { e ->
                habilitarBotaoAgendar()
                mostrarSnackbar(
                    "Erro ao salvar agendamento: ${e.localizedMessage}",
                    "#FF0000"
                )
            }
    }

    private fun mostrarDialogAgendamentoSucesso() {
        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle("Agendamento realizado!")
            .setMessage(
                "Seu agendamento foi registrado com sucesso. " +
                        "Você poderá acompanhá-lo na tela de Meus Agendamentos."
            )
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
                finish()
            }
            .create()

        dialog.setOnShowListener {
            val botao = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            botao?.setTextColor(ContextCompat.getColor(this, R.color.vermelho_primario))
        }

        dialog.show()
    }

    private fun mostrarSnackbar(mensagem: String, corHex: String) {
        val root = binding.root
        val snack = Snackbar.make(root, mensagem, Snackbar.LENGTH_LONG)
        snack.view.setBackgroundColor(Color.parseColor(corHex))
        snack.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        snack.show()
    }

    // ---------------- Firestore: carregar hemocentros ----------------

    private fun carregarHemocentros() {
        val temLocalizacaoUsuario = userLat != null && userLng != null

        firestore.collection("hemocentros")
            .get()
            .addOnSuccessListener { snapshot ->
                todosHemocentros.clear()

                val chavesVistas = mutableSetOf<String>()

                for (doc in snapshot) {
                    val cidade = doc.getString("cidade") ?: doc.getString("municipio")
                    val uf = doc.getString("uf")?.uppercase(Locale("pt", "BR"))
                    val regiao =
                        doc.getString("regiao")?.uppercase(Locale("pt", "BR"))
                            ?: doc.getString("Regiao")?.uppercase(Locale("pt", "BR"))

                    if (cidade.isNullOrBlank() || uf.isNullOrBlank() || regiao.isNullOrBlank()) continue

                    val cepRaw = doc.getString("cep") ?: doc.getString("CEP")
                    val cep = cepRaw?.trim()
                    val cepLimpo = cep?.filter { it.isDigit() }
                    if (cepLimpo.isNullOrBlank() || cepLimpo.length < 8) continue

                    val fantasia = doc.getString("fantasia")
                    var nomeJuridico = doc.getString("nome")

                    if (!nomeJuridico.isNullOrBlank()) {
                        val padraoJuridico =
                            "(?i)\\b(LTDA|Ltda|LTDA\\.|S/A|SA|S A|ME|EPP|EMPRESA|INDÚSTRIA|INDUSTRIA|COMÉRCIO|COMERCIO)\\b"
                        nomeJuridico = nomeJuridico
                            .replace(Regex(padraoJuridico), "")
                            .replace(Regex("\\(.*?\\)"), "")
                            .replace(Regex("\\s{2,}"), " ")
                            .trim()
                    }

                    val nomeBase = when {
                        !fantasia.isNullOrBlank() -> fantasia.trim()
                        !nomeJuridico.isNullOrBlank() -> nomeJuridico.trim()
                        else -> "Hemocentro"
                    }

                    val nomeLimpo = nomeBase
                        .replace(Regex("\\s{2,}"), " ")
                        .trim()

                    if (nomeLimpo.isBlank()) continue

                    val nomeUpper = nomeLimpo.uppercase(Locale("pt", "BR"))
                    val termosBloqueados = listOf(
                        "HOSPITAL",
                        "CLINICA",
                        "CLÍNICA",
                        "LABORATORIO",
                        "LABORATÓRIO",
                        "SERVICOS MEDICOS",
                        "SERVIÇOS MEDICOS",
                        "SERVIÇOS MÉDICOS",
                        "DIAGNOSTICO",
                        "DIAGNÓSTICO",
                        "MULTIHEMO",
                        "UNIHEMO"
                    )
                    if (termosBloqueados.any { termo -> nomeUpper.contains(termo) }) continue

                    val statusRaw = doc.getString("status") ?: doc.getString("STATUS")
                    val status = statusRaw?.trim()
                    val statusUpper = status?.uppercase(Locale("pt", "BR"))
                    if (statusUpper != null) {
                        val statusBloqueados =
                            listOf("INATIVO", "FECHADO", "BLOQUEADO", "DESATIVADO")
                        if (statusBloqueados.any { statusUpper.contains(it) }) continue
                    }

                    val chave = nomeLimpo.uppercase(Locale("pt", "BR")) + "|" +
                            cidade.trim().uppercase(Locale("pt", "BR"))
                    if (!chavesVistas.add(chave)) continue

                    val logradouro = doc.getString("logradouro")
                    val numero = doc.getString("numero") ?: doc.getString("NU_ENDERECO")
                    val telefone = doc.getString("telefone")

                    val horario = doc.getString("horario_funcionamento")
                        ?: doc.getString("horario")
                        ?: doc.getString("horarioFuncionamento")

                    val latitude = doc.getDouble("latitude") ?: doc.getDouble("lat")
                    val longitude = doc.getDouble("longitude") ?: doc.getDouble("lng")

                    val distanciaKm = if (temLocalizacaoUsuario && latitude != null && longitude != null) {
                        calcularDistanciaKm(userLat!!, userLng!!, latitude, longitude)
                    } else {
                        null
                    }

                    val hemo = Hemocentro(
                        id = doc.id,
                        nome = nomeLimpo,
                        cidade = cidade,
                        uf = uf,
                        regiao = regiao,
                        telefone = telefone,
                        logradouro = logradouro,
                        status = status,
                        cep = cep,
                        numero = numero,
                        horarioFuncionamento = horario,
                        latitude = latitude,
                        longitude = longitude,
                        distanciaKm = distanciaKm
                    )

                    todosHemocentros.add(hemo)
                }

                if (todosHemocentros.isEmpty()) {
                    atualizarSpinnerRegiao()
                    mostrarSnackbar(
                        "Nenhum hemocentro disponível no momento. Tente novamente mais tarde.",
                        "#9E9E9E"
                    )
                    return@addOnSuccessListener
                }

                if (userLat != null && userLng != null) {
                    todosHemocentros.sortBy { it.distanciaKm ?: Double.MAX_VALUE }
                } else {
                    todosHemocentros.sortBy { it.nome.lowercase(Locale("pt", "BR")) }
                }

                atualizarSpinnerRegiao()
            }
            .addOnFailureListener {
                mostrarSnackbar("Erro ao carregar hemocentros.", "#FF0000")
                atualizarSpinnerRegiao()
            }
    }

    private fun calcularDistanciaKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}