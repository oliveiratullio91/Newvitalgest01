package com.example.newvitalgest01.view

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R
import com.example.newvitalgest01.domain.model.Hemocentro
import com.example.newvitalgest01.ui.hemocentros.HemocentrosProximosViewModel
import com.example.newvitalgest01.ui.hemocentros.HemocentrosProximosViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ---------------------------
// MODELO DE DADOS DA TELA
// ---------------------------
data class HemocentroListaItem(
    val id: String,
    val nome: String,
    val endereco: String,
    val cidade: String?,
    val uf: String?,
    val cep: String?,
    val telefone: String?,
    val latitude: String?,
    val longitude: String?,
    val regiao: String?,
    val status: String?,
    var isFavorito: Boolean = false
)

class HemocentrosProximosActivity : BaseActivity() {

    private lateinit var recyclerHemocentros: RecyclerView
    private lateinit var recyclerFavoritos: RecyclerView
    private lateinit var adapterHemocentros: HemocentroAdapter
    private lateinit var adapterFavoritos: HemocentroAdapter

    private lateinit var spinnerRegiaoFiltro: Spinner
    private lateinit var spinnerUfFiltro: Spinner
    private lateinit var spinnerCidadeFiltro: Spinner
    private lateinit var txtTituloFavoritos: TextView

    // Controle de exibição da lista principal
    private var mostrarListaPrincipal: Boolean = false

    // Flag para saber se os filtros já terminaram a configuração inicial
    private var filtrosProntos: Boolean = false

    // Fonte única de verdade da tela
    private val listaHemocentros = mutableListOf<HemocentroListaItem>()

    private val prefs by lazy { getSharedPreferences("hemocentros_prefs", Context.MODE_PRIVATE) }

    private val viewModel: HemocentrosProximosViewModel by viewModels {
        HemocentrosProximosViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hemocentros_proximos)

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

        recyclerHemocentros = findViewById(R.id.recyclerHemocentros)
        recyclerFavoritos = findViewById(R.id.recyclerFavoritos)
        spinnerRegiaoFiltro = findViewById(R.id.spinnerRegiaoFiltro)
        spinnerUfFiltro = findViewById(R.id.spinnerUfFiltro)
        spinnerCidadeFiltro = findViewById(R.id.spinnerCidadeFiltro)
        txtTituloFavoritos = findViewById(R.id.txtTituloFavoritos)

        recyclerHemocentros.layoutManager = LinearLayoutManager(this)
        recyclerFavoritos.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Garante que começa invisível, além do XML
        recyclerHemocentros.visibility = View.GONE

        adapterHemocentros = HemocentroAdapter(this, emptyList()) { id -> onFavoritoClick(id) }
        adapterFavoritos = HemocentroAdapter(this, emptyList()) { id -> onFavoritoClick(id) }

        recyclerHemocentros.adapter = adapterHemocentros
        recyclerFavoritos.adapter = adapterFavoritos

        findViewById<Button>(R.id.btnVoltar).setOnClickListener { finish() }

        observarViewModel()
        viewModel.carregarHemocentros()
    }

    // ---------------------------
    // SharedPreferences helpers
    // ---------------------------
    private fun carregarIdsFavoritos(): Set<String> {
        return prefs.getStringSet("favoritos_ids", emptySet()) ?: emptySet()
    }

    private fun salvarIdsFavoritos(ids: Set<String>) {
        prefs.edit().putStringSet("favoritos_ids", ids).apply()
    }

    // ---------------------------
    // Integração com ViewModel
    // ---------------------------
    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLoading) {
                    // colocar loading se quiser
                }

                state.error?.let { erro ->
                    mostrarSnackbar(erro, "#FF0000")
                }

                if (!state.isLoading) {
                    atualizarListaAPartirDoEstado(state.hemocentros)
                }
            }
        }
    }

    private fun atualizarListaAPartirDoEstado(hemocentrosDomain: List<Hemocentro>) {
        listaHemocentros.clear()

        if (hemocentrosDomain.isEmpty()) {
            adapterHemocentros.atualizarLista(emptyList())
            recyclerHemocentros.visibility = View.GONE
            txtTituloFavoritos.visibility = View.GONE
            recyclerFavoritos.visibility = View.GONE
            return
        }

        val idsFavoritos = carregarIdsFavoritos()

        listaHemocentros.addAll(
            hemocentrosDomain.map { hemo ->
                val enderecoCompleto = buildString {
                    append(hemo.logradouro ?: "")
                    val numero = hemo.numero
                    if (!numero.isNullOrBlank() && numero != "S/N") {
                        if (isNotEmpty()) append(", ")
                        append(numero)
                    }
                }

                HemocentroListaItem(
                    id = hemo.id,
                    nome = hemo.nome,
                    endereco = enderecoCompleto,
                    cidade = hemo.cidade,
                    uf = hemo.uf,
                    cep = hemo.cep,
                    telefone = hemo.telefone,
                    latitude = hemo.latitude?.toString(),
                    longitude = hemo.longitude?.toString(),
                    regiao = hemo.regiao,
                    status = hemo.status,
                    isFavorito = idsFavoritos.contains(hemo.id)
                )
            }
        )

        listaHemocentros.sortBy { it.nome.lowercase() }

        configurarFiltros()
        animarListaPrincipal()
    }

    // ---------------------------
    // Clique na estrela
    // ---------------------------
    private fun onFavoritoClick(hemocentroId: String) {
        for (i in listaHemocentros.indices) {
            val atual = listaHemocentros[i]
            if (atual.id == hemocentroId) {
                listaHemocentros[i] = atual.copy(isFavorito = !atual.isFavorito)
                break
            }
        }

        val idsFavoritos = listaHemocentros
            .filter { it.isFavorito }
            .map { it.id }
            .toSet()
        salvarIdsFavoritos(idsFavoritos)

        atualizarListasVisiveis()
    }

    // ---------------------------
    // Atualiza lista principal + favoritos respeitando filtros
    // ---------------------------
    private fun atualizarListasVisiveis() {
        val regiaoSel = spinnerRegiaoFiltro.selectedItem as? String ?: "Todas"
        val ufSel = spinnerUfFiltro.selectedItem as? String ?: "Todos"
        val cidadeSel = spinnerCidadeFiltro.selectedItem as? String ?: "Todas"

        val listaFiltrada = listaHemocentros.filter { item ->
            (regiaoSel == "Todas" || item.regiao == regiaoSel) &&
                    (ufSel == "Todos" || item.uf == ufSel) &&
                    (cidadeSel == "Todas" || item.cidade == cidadeSel)
        }

        // Lista principal só aparece quando o usuário interage com os filtros
        if (mostrarListaPrincipal && listaFiltrada.isNotEmpty()) {
            recyclerHemocentros.visibility = View.VISIBLE
            adapterHemocentros.atualizarLista(listaFiltrada)
        } else {
            recyclerHemocentros.visibility = View.GONE
            adapterHemocentros.atualizarLista(emptyList())
        }

        // Card de favoritos é sempre atualizado
        val favoritos = listaHemocentros.filter { it.isFavorito }
        if (favoritos.isEmpty()) {
            txtTituloFavoritos.visibility = View.GONE
            recyclerFavoritos.visibility = View.GONE
            adapterFavoritos.atualizarLista(emptyList())
        } else {
            txtTituloFavoritos.visibility = View.VISIBLE
            recyclerFavoritos.visibility = View.VISIBLE
            adapterFavoritos.atualizarLista(favoritos)
        }
    }

    // ---------------------------
    // Filtros
    // ---------------------------
    private fun configurarFiltros() {
        filtrosProntos = false
        mostrarListaPrincipal = false

        val regioes = listaHemocentros
            .mapNotNull { it.regiao }
            .distinct()
            .sorted()

        val adapterRegiao = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listOf("Todas") + regioes
        )
        adapterRegiao.setDropDownViewResource(R.layout.item_spinner_dropdown_text)
        spinnerRegiaoFiltro.adapter = adapterRegiao

        spinnerRegiaoFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!filtrosProntos) return

                val regiaoSel = parent.getItemAtPosition(position) as String
                atualizarUfFiltro(regiaoSel)
                mostrarListaPrincipal = true
                atualizarListasVisiveis()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerUfFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!filtrosProntos) return

                val ufSel = parent.getItemAtPosition(position) as String
                atualizarCidadeFiltro(ufSel)
                mostrarListaPrincipal = true
                atualizarListasVisiveis()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerCidadeFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!filtrosProntos) return

                mostrarListaPrincipal = true
                atualizarListasVisiveis()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Config inicial: essas chamadas não disparam a lista porque filtrosProntos ainda é false
        atualizarUfFiltro("Todas")
        atualizarCidadeFiltro("Todos")

        filtrosProntos = true

        // Primeira exibição: apenas favoritos, lista principal invisível
        atualizarListasVisiveis()
    }

    private fun atualizarUfFiltro(regiaoSel: String) {
        val ufs = listaHemocentros
            .filter { regiaoSel == "Todas" || it.regiao == regiaoSel }
            .mapNotNull { it.uf }
            .distinct()
            .sorted()

        val adapterUf = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listOf("Todos") + ufs
        )
        adapterUf.setDropDownViewResource(R.layout.item_spinner_dropdown_text)
        spinnerUfFiltro.adapter = adapterUf
    }

    private fun atualizarCidadeFiltro(ufSel: String) {
        val regiaoSel = spinnerRegiaoFiltro.selectedItem as? String ?: "Todas"

        val cidades = listaHemocentros
            .filter {
                (regiaoSel == "Todas" || it.regiao == regiaoSel) &&
                        (ufSel == "Todos" || it.uf == ufSel)
            }
            .mapNotNull { it.cidade }
            .distinct()
            .sorted()

        val adapterCidade = ArrayAdapter(
            this,
            R.layout.item_spinner_text,
            listOf("Todas") + cidades
        )
        adapterCidade.setDropDownViewResource(R.layout.item_spinner_dropdown_text)
        spinnerCidadeFiltro.adapter = adapterCidade
    }

    // ---------------------------
    // UI helpers
    // ---------------------------
    private fun animarListaPrincipal() {
        recyclerHemocentros.apply {
            alpha = 0f
            animate().alpha(1f).setDuration(180L).start()
        }
    }

    private fun mostrarSnackbar(msg: String, corHex: String) {
        val root = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(root, msg, Snackbar.LENGTH_LONG)

        try {
            val backgroundColor = Color.parseColor(corHex)
            snackbar.view.setBackgroundColor(backgroundColor)
        } catch (_: Exception) {
        }

        snackbar.show()
    }
}

// ---------------------------
// ADAPTER
// ---------------------------
class HemocentroAdapter(
    private val context: Context,
    private var hemocentros: List<HemocentroListaItem>,
    private val onFavoritoClick: (String) -> Unit
) : RecyclerView.Adapter<HemocentroAdapter.HemocentroViewHolder>() {

    init {
        setHasStableIds(true)
    }

    inner class HemocentroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeHemocentro)
        val txtEndereco: TextView = itemView.findViewById(R.id.txtEnderecoHemocentro)
        val txtCidadeUf: TextView = itemView.findViewById(R.id.txtCidadeUfHemocentro)
        val txtTelefone: TextView = itemView.findViewById(R.id.txtTelefoneHemocentro)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatusHemocentro)
        val btnLigar: Button = itemView.findViewById(R.id.btnLigarHemocentro)
        val btnMapa: Button = itemView.findViewById(R.id.btnMapaHemocentro)
        val imgFavorito: ImageView = itemView.findViewById(R.id.imgFavoritoHemocentro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HemocentroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hemocentro, parent, false)
        return HemocentroViewHolder(view)
    }

    override fun onBindViewHolder(holder: HemocentroViewHolder, position: Int) {
        val item = hemocentros[position]

        holder.txtNome.text = item.nome
        holder.txtEndereco.text =
            item.endereco.ifBlank { "Endereço não informado" }

        val cidadeUf = listOfNotNull(item.cidade, item.uf).joinToString(" - ")
        holder.txtCidadeUf.text =
            if (cidadeUf.isNotBlank()) cidadeUf else "Cidade/UF não informadas"

        holder.txtTelefone.text =
            item.telefone?.takeIf { it.isNotBlank() } ?: "Telefone não informado"

        val statusRaw = item.status?.trim()?.uppercase()
        val (label, colorHex) = when (statusRaw) {
            "OK", "ATIVO", "FUNCIONANDO" -> "ATIVO" to "#2E7D32"
            "INATIVO", "FECHADO" -> "INATIVO" to "#C62828"
            null, "" -> "NÃO INFORMADO" to "#9E9E9E"
            else -> statusRaw to "#9E9E9E"
        }

        holder.txtStatus.text = label
        ViewCompat.setBackgroundTintList(
            holder.txtStatus,
            ColorStateList.valueOf(Color.parseColor(colorHex))
        )

        val iconRes = if (item.isFavorito) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        holder.imgFavorito.setImageResource(iconRes)

        holder.imgFavorito.setOnClickListener {
            onFavoritoClick(item.id)
        }

        holder.itemView.setOnClickListener {
            mostrarDialogDetalhes(item)
        }

        holder.btnLigar.setOnClickListener {
            ligarParaHemocentro(item)
        }

        holder.btnMapa.setOnClickListener {
            abrirNoMapa(item)
        }
    }

    override fun getItemCount(): Int = hemocentros.size

    override fun getItemId(position: Int): Long =
        hemocentros.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID

    fun atualizarLista(novaLista: List<HemocentroListaItem>) {
        hemocentros = novaLista
        notifyDataSetChanged()
    }

    // ---------------------------
    // Detalhes / mapa / ligação
    // ---------------------------
    private fun mostrarDialogDetalhes(item: HemocentroListaItem) {
        val enderecoCompleto = buildString {
            append(item.endereco.ifBlank { "Endereço não informado" })
            val cidadeUf = listOfNotNull(item.cidade, item.uf).joinToString(" - ")
            if (cidadeUf.isNotBlank()) {
                append("\n$cidadeUf")
            }
        }

        val cepTxt = item.cep?.takeIf { it.isNotBlank() } ?: "Não informado"
        val telefoneTxt = item.telefone?.takeIf { it.isNotBlank() } ?: "Não informado"
        val statusTxt = item.status?.ifBlank { "Não informado" } ?: "Não informado"

        val mensagem = """
            🏥 ${item.nome}
            
            📍 $enderecoCompleto
            📮 CEP: $cepTxt
            ✅ Status: $statusTxt
            📞 Telefone: $telefoneTxt
            
            O que você deseja fazer?
        """.trimIndent()

        val dialog = MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogTheme)
            .setTitle("Detalhes do hemocentro")
            .setMessage(mensagem)
            .setNegativeButton("Fechar", null)
            .setNeutralButton("Ver no mapa") { _, _ ->
                abrirNoMapa(item)
            }
            .setPositiveButton("Ligar") { _, _ ->
                ligarParaHemocentro(item)
            }
            .create()

        dialog.setOnShowListener {
            estilizarBotoesDialog(dialog)
        }

        dialog.show()
    }

    private fun abrirNoMapa(item: HemocentroListaItem) {
        val intent = if (!item.latitude.isNullOrBlank() && !item.longitude.isNullOrBlank()) {
            val uri = Uri.parse("geo:${item.latitude},${item.longitude}?q=${Uri.encode(item.nome)}")
            Intent(Intent.ACTION_VIEW, uri)
        } else {
            val enderecoBusca = buildString {
                append(item.endereco)
                val cidadeUf = listOfNotNull(item.cidade, item.uf).joinToString(" - ")
                if (cidadeUf.isNotBlank()) {
                    append(" - ")
                    append(cidadeUf)
                }
            }
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(enderecoBusca)}")
            Intent(Intent.ACTION_VIEW, uri)
        }

        intent.setPackage("com.google.android.apps.maps")

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            intent.setPackage(null)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    private fun ligarParaHemocentro(item: HemocentroListaItem) {
        val telefoneLimpo = item.telefone
            ?.replace("(", "")
            ?.replace(")", "")
            ?.replace(" ", "")
            ?.replace("-", "")

        if (telefoneLimpo.isNullOrBlank()) {
            val root = (context as? AppCompatActivity)
                ?.findViewById<View>(android.R.id.content) ?: return

            Snackbar.make(root, "Telefone não disponível para este hemocentro.", Snackbar.LENGTH_LONG)
                .show()
            return
        }

        val uri = Uri.parse("tel:$telefoneLimpo")
        val intent = Intent(Intent.ACTION_DIAL, uri)

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun estilizarBotoesDialog(dialog: AlertDialog) {
        val primary = ContextCompat.getColor(context, R.color.vermelho_primario)
        val white = ContextCompat.getColor(context, android.R.color.white)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setBackgroundColor(primary)
            setTextColor(white)
            textSize = 14f
            isAllCaps = false
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.apply {
            setTextColor(primary)
            textSize = 14f
            isAllCaps = false
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(primary)
            textSize = 14f
            isAllCaps = false
        }
    }
}