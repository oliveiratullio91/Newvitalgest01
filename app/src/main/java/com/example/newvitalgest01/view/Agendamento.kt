package com.example.newvitalgest01.view

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityAgendamentoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * Modelo de Hemocentro usado na tela de agendamento.
 * O nome já vem LIMPO (sem LTDA, S/A, ME etc.).
 */
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
    val distanciaKm: Double?        // pode ser nulo se não tiver localização
)

class Agendamento : BaseActivity() {

    private lateinit var binding: ActivityAgendamentoBinding

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Lista de hemocentros já filtrados/deduplicados
    private val todosHemocentros = mutableListOf<Hemocentro>()
    private var hemocentroSelecionado: Hemocentro? = null

    // Localização do usuário (opcional) – pode vir como extra na Intent
    private var userLat: Double? = null
    private var userLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendamentoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        supportActionBar?.hide()

        // Cores da status bar / nav bar
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

        // Tenta pegar localização do usuário enviada pela outra tela (opcional)
        val latExtra = intent.getDoubleExtra("userLat", Double.NaN)
        val lngExtra = intent.getDoubleExtra("userLng", Double.NaN)
        if (!latExtra.isNaN() && !lngExtra.isNaN()) {
            userLat = latExtra
            userLng = lngExtra
        }

        configurarPickers()
        configurarSpinners()
        configurarCliques()

        // Esconde o card inicialmente
        binding.cardHemocentroInfo.visibility = View.GONE

        // Carrega hemocentros do Firestore com filtro forte
        carregarHemocentros()
    }

    // --------------------------------------------------------------------
    // DatePicker & TimePicker
    // --------------------------------------------------------------------
    private fun configurarPickers() {
        val hoje = Calendar.getInstance()
        binding.datePicker.minDate = hoje.timeInMillis
        binding.timePicker.setIs24HourView(true)
    }

    // --------------------------------------------------------------------
    // Spinners
    // --------------------------------------------------------------------
    private fun configurarSpinners() {

        binding.btAgendar.isEnabled = false

        fun criarAdapter(itens: List<String>) =
            ArrayAdapter(this, R.layout.item_spinner_text, itens).apply {
                setDropDownViewResource(R.layout.item_spinner_dropdown_text)
            }

        // Placeholders
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
                    val regiao = parent?.getItemAtPosition(position) as? String ?: return
                    if (!regiao.startsWith("Selecione")) {
                        atualizarUf(regiao)
                    }
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
                    val uf = parent?.getItemAtPosition(position) as? String ?: return
                    if (!uf.startsWith("Selecione")) {
                        atualizarCidade(uf)
                    }
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
                    val cidade = parent?.getItemAtPosition(position) as? String ?: return
                    if (!cidade.startsWith("Selecione")) {
                        val uf = binding.spinnerUf.selectedItem as? String ?: return
                        atualizarHemocentro(uf, cidade)
                    }
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
                    val nome = parent?.getItemAtPosition(position) as? String ?: return

                    if (nome.startsWith("Selecione")) {
                        hemocentroSelecionado = null
                        binding.cardHemocentroInfo.visibility = View.GONE
                        return
                    }

                    val uf = binding.spinnerUf.selectedItem as? String
                    val cidade = binding.spinnerCidade.selectedItem as? String

                    val hemo = todosHemocentros.firstOrNull {
                        it.nome == nome && it.uf == uf && it.cidade == cidade
                    }

                    hemocentroSelecionado = hemo

                    if (hemo != null) {
                        preencherCardHemocentro(hemo)
                    } else {
                        binding.cardHemocentroInfo.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    hemocentroSelecionado = null
                    binding.cardHemocentroInfo.visibility = View.GONE
                }
            }
    }

    // --------------------------------------------------------------------
    // Botões
    // --------------------------------------------------------------------
    private fun configurarCliques() {
        binding.btAgendar.setOnClickListener {
            confirmarAgendamento()
        }

        binding.btVoltarServicos.setOnClickListener {
            finish()
        }
    }

    // --------------------------------------------------------------------
    // Carregamento de Hemocentros do Firestore
    // Com filtro forte + limpeza de nome + remoção de duplicados
    // --------------------------------------------------------------------
    private fun carregarHemocentros() {
        firestore.collection("hemocentros")
            .get()
            .addOnSuccessListener { snapshot ->
                todosHemocentros.clear()

                val chavesVistas = mutableSetOf<String>() // para deduplicar (nome + cidade)

                val temLocalizacaoUsuario = userLat != null && userLng != null

                for (doc in snapshot) {

                    // Cidade e UF
                    val cidade = doc.getString("cidade") ?: doc.getString("municipio")
                    val uf = doc.getString("uf")?.uppercase(Locale.ROOT)
                    val regiao = (doc.getString("regiao")
                        ?: doc.getString("Regiao"))?.uppercase(Locale.ROOT)

                    if (cidade.isNullOrBlank() || uf.isNullOrBlank() || regiao.isNullOrBlank()) {
                        continue
                    }

                    // CEP (obrigatório, 8 dígitos)
                    val cepRaw = doc.getString("cep") ?: doc.getString("CEP")
                    val cep = cepRaw?.trim()
                    val cepLimpo = cep?.filter { it.isDigit() }

                    if (cepLimpo.isNullOrBlank() || cepLimpo.length < 8) {
                        continue
                    }

                    // Fantasia x Razão Social
                    val fantasia = doc.getString("fantasia")
                    var nomeJuridico = doc.getString("nome")

                    if (!nomeJuridico.isNullOrBlank()) {
                        val padraoJuridico =
                            "(?i)\\b(LTDA|Ltda|LTDA\\.|S/A|SA|S A|ME|EPP|EMPRESA|INDÚSTRIA|INDUSTRIA|COMÉRCIO|COMERCIO)\\b"
                        nomeJuridico = nomeJuridico
                            .replace(Regex(padraoJuridico), "")
                            .replace(Regex("\\(.*?\\)"), "")   // remove texto entre parênteses
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

                    // Remove hospitais / clínicas / laboratórios / etc.
                    val nomeUpper = nomeLimpo.uppercase(Locale.ROOT)
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
                    if (termosBloqueados.any { termo -> nomeUpper.contains(termo) }) {
                        continue
                    }

                    // Status
                    val statusRaw = doc.getString("status") ?: doc.getString("STATUS")
                    val status = statusRaw?.trim()
                    val statusUpper = status?.uppercase(Locale.ROOT)

                    // Remove unidades claramente inativas
                    if (statusUpper != null) {
                        val statusBloqueados = listOf(
                            "INATIVO",
                            "FECHADO",
                            "BLOQUEADO",
                            "DESATIVADO"
                        )
                        if (statusBloqueados.any { statusUpper.contains(it) }) {
                            continue
                        }
                    }

                    // Deduplicar: mesmo nome + cidade
                    val chave = nomeLimpo.uppercase(Locale.ROOT) + "|" +
                            cidade.trim().uppercase(Locale.ROOT)
                    if (chavesVistas.contains(chave)) {
                        continue
                    } else {
                        chavesVistas.add(chave)
                    }

                    val logradouro = doc.getString("logradouro")
                    val numero = doc.getString("numero") ?: doc.getString("NU_ENDERECO")
                    val telefone = doc.getString("telefone")

                    // Horário de funcionamento (se tiver)
                    val horario = doc.getString("horario_funcionamento")
                        ?: doc.getString("horario")
                        ?: doc.getString("horarioFuncionamento")

                    // Localização do hemocentro
                    val lat = (doc.getDouble("latitude")
                        ?: doc.getDouble("lat"))
                    val lng = (doc.getDouble("longitude")
                        ?: doc.getDouble("lng"))

                    // Distância (se tivermos tudo)
                    val distanciaKm = if (temLocalizacaoUsuario && lat != null && lng != null) {
                        calcularDistanciaKm(
                            userLat!!,
                            userLng!!,
                            lat,
                            lng
                        )
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
                        latitude = lat,
                        longitude = lng,
                        distanciaKm = distanciaKm
                    )

                    todosHemocentros.add(hemo)
                }

                if (todosHemocentros.isEmpty()) {
                    mostrarSnackbar(
                        "Não foi possível carregar a lista de hemocentros.",
                        "#FF0000"
                    )
                    binding.btAgendar.isEnabled = false
                    return@addOnSuccessListener
                }

                // Preenche regiões
                val regioes = todosHemocentros
                    .map { it.regiao }
                    .distinct()
                    .sorted()

                val listaRegioes = mutableListOf("Selecione a região")
                listaRegioes.addAll(regioes)

                val adapterRegiao = ArrayAdapter(
                    this,
                    R.layout.item_spinner_text,
                    listaRegioes
                ).apply {
                    setDropDownViewResource(R.layout.item_spinner_dropdown_text)
                }

                binding.spinnerRegiao.adapter = adapterRegiao
                binding.btAgendar.isEnabled = true
            }
            .addOnFailureListener { e ->
                mostrarSnackbar(
                    "Erro ao carregar hemocentros: ${e.localizedMessage}",
                    "#FF0000"
                )
            }
    }

    // --------------------------------------------------------------------
    // Atualização de UF / Cidade / Hemocentro
    // --------------------------------------------------------------------
    private fun atualizarUf(regiao: String) {
        hemocentroSelecionado = null
        binding.cardHemocentroInfo.visibility = View.GONE

        val ufs = todosHemocentros
            .filter { it.regiao == regiao }
            .map { it.uf }
            .distinct()
            .sorted()

        val listaUf = mutableListOf("Selecione o estado")
        listaUf.addAll(ufs)

        val adapterUf = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listaUf
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_text)
        }

        binding.spinnerUf.adapter = adapterUf
        binding.spinnerCidade.adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listOf("Selecione o estado")
        )
        binding.spinnerHemocentro.adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listOf("Selecione a cidade")
        )
    }

    private fun atualizarCidade(uf: String) {
        hemocentroSelecionado = null
        binding.cardHemocentroInfo.visibility = View.GONE

        val cidades = todosHemocentros
            .filter { it.uf == uf }
            .map { it.cidade }
            .distinct()
            .sorted()

        val listaCidades = mutableListOf("Selecione a cidade")
        listaCidades.addAll(cidades)

        val adapterCidade = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listaCidades
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_text)
        }

        binding.spinnerCidade.adapter = adapterCidade
        binding.spinnerHemocentro.adapter = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listOf("Selecione o hemocentro")
        )
    }

    private fun atualizarHemocentro(uf: String, cidade: String) {
        hemocentroSelecionado = null
        binding.cardHemocentroInfo.visibility = View.GONE

        val hemocentrosCidade = todosHemocentros
            .filter { it.uf == uf && it.cidade == cidade }

        val listaNomes = mutableListOf("Selecione o hemocentro")
        listaNomes.addAll(
            hemocentrosCidade
                .map { it.nome }
                .distinct()
                .sorted()
        )

        val adapterHemocentro = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listaNomes
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_text)
        }

        binding.spinnerHemocentro.adapter = adapterHemocentro
    }

    // --------------------------------------------------------------------
    // Preencher CARD com dados do hemocentro
    // (1, 2, 3, 4, 6, 7)
    // --------------------------------------------------------------------
    private fun preencherCardHemocentro(hemo: Hemocentro) {

        binding.cardHemocentroInfo.visibility = View.VISIBLE

        // 1) Nome
        binding.txtNomeHemo.text = hemo.nome

        // 2) Endereço completo
        val endereco = buildString {
            append(hemo.logradouro ?: "Endereço não informado")
            if (!hemo.numero.isNullOrBlank() && hemo.numero != "S/N") {
                append(", ${hemo.numero}")
            }
            append("\n${hemo.cidade} - ${hemo.uf}")
            if (!hemo.cep.isNullOrBlank()) {
                append("\nCEP: ${hemo.cep}")
            }
        }
        binding.txtEnderecoHemo.text = endereco

        // 3) Telefone
        binding.txtTelefoneHemo.text =
            "Telefone: " + (hemo.telefone ?: "Não informado")

        // 4) Horário de funcionamento
        binding.txtHorarioHemo.text =
            "Horário: " + (hemo.horarioFuncionamento ?: "Não informado")

        // 6) Status
        binding.txtStatusHemo.text =
            "Status: " + (hemo.status ?: "Não informado")

        // 7) Distância
        val distanciaStr = hemo.distanciaKm?.let {
            String.format(Locale("pt", "BR"), "%.1f km", it)
        } ?: "--"
        binding.txtDistanciaHemo.text = "Distância: $distanciaStr"
    }

    // --------------------------------------------------------------------
    // Confirmação de agendamento
    // --------------------------------------------------------------------
    private fun confirmarAgendamento() {
        val usuario = auth.currentUser
        if (usuario == null) {
            mostrarSnackbar("Usuário não autenticado.", "#FF0000")
            return
        }

        val hemo = hemocentroSelecionado
        if (hemo == null) {
            mostrarSnackbar("Selecione um hemocentro.", "#FF0000")
            return
        }

        // Data
        val dia = binding.datePicker.dayOfMonth
        val mes = binding.datePicker.month + 1
        val ano = binding.datePicker.year
        val data = "%02d/%02d/%04d".format(dia, mes, ano)

        // Hora (08h às 17h)
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

        if (hora !in 8..17) {
            mostrarSnackbar("Agendamento permitido das 08h às 17h.", "#FF0000")
            return
        }

        val horaTxt = "%02d:%02d".format(hora, minuto)

        val enderecoLinha = buildString {
            if (!hemo.logradouro.isNullOrBlank()) {
                append(hemo.logradouro)
                if (!hemo.numero.isNullOrBlank() && hemo.numero != "S/N") {
                    append(", ")
                    append(hemo.numero)
                }
            } else {
                append("Endereço não informado")
            }
        }

        val resumo = """
            Confirme o agendamento:
            
            🏥 ${hemo.nome}
            📅 $data às $horaTxt
            📍 $enderecoLinha
            🏙️ ${hemo.cidade} - ${hemo.uf}
            📞 ${hemo.telefone ?: "Não informado"}
        """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle("Confirmar agendamento")
            .setMessage(resumo)
            .setNegativeButton("Editar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                salvarAgendamento(hemo, data, horaTxt)
            }
            .create()

        dialog.setOnShowListener { estilizarDialog(dialog) }
        dialog.show()
    }

    // --------------------------------------------------------------------
    // Salvar no Firestore
    // --------------------------------------------------------------------
    private fun salvarAgendamento(hemo: Hemocentro, data: String, hora: String) {
        val usuario = auth.currentUser ?: return

        val agendamento = hashMapOf(
            "hemocentroId" to hemo.id,
            "hemocentro" to hemo.nome,
            "data" to data,
            "hora" to hora,
            "cidade" to hemo.cidade,
            "uf" to hemo.uf,
            "regiao" to hemo.regiao,
            "endereco" to hemo.logradouro,
            "numero" to hemo.numero,
            "cep" to hemo.cep,
            "telefone" to hemo.telefone,
            "horarioFuncionamento" to hemo.horarioFuncionamento,
            "statusHemocentro" to hemo.status,
            "latitude" to hemo.latitude,
            "longitude" to hemo.longitude,
            "distanciaKm" to hemo.distanciaKm,
            "status" to "Pendente",
            "criadoEm" to FieldValue.serverTimestamp()
        )

        firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .add(agendamento)
            .addOnSuccessListener {
                MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
                    .setTitle("Agendado!")
                    .setMessage("Seu agendamento foi realizado com sucesso! 🎉")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .show()
            }
            .addOnFailureListener {
                mostrarSnackbar("Erro ao salvar agendamento.", "#FF0000")
            }
    }

    // --------------------------------------------------------------------
    // Helpers de UI
    // --------------------------------------------------------------------
    private fun mostrarSnackbar(msg: String, corHex: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
            .setBackgroundTint(Color.parseColor(corHex))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()
    }

    private fun estilizarDialog(dialog: AlertDialog) {
        val primaryColor = ContextCompat.getColor(this, R.color.vermelho_primario)
        val white = ContextCompat.getColor(this, android.R.color.white)

        val botaoPositivo = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val botaoNegativo = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        botaoPositivo?.apply {
            setBackgroundColor(primaryColor)
            setTextColor(white)
            textSize = 14f
            isAllCaps = false
            setPadding(40, 10, 40, 10)
        }

        botaoNegativo?.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(primaryColor)
            textSize = 14f
            isAllCaps = false
        }
    }

    // --------------------------------------------------------------------
    // Cálculo de distância entre dois pontos (km)
    // --------------------------------------------------------------------
    private fun calcularDistanciaKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val R = 6371.0 // raio da Terra em km
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