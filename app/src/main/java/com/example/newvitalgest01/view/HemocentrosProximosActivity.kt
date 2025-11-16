package com.example.newvitalgest01.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.res.ColorStateList

// ---------------------------
// MODELO DE DADOS
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
    val status: String?
)

class HemocentrosProximosActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HemocentroAdapter

    private lateinit var spinnerRegiaoFiltro: Spinner
    private lateinit var spinnerUfFiltro: Spinner
    private lateinit var spinnerCidadeFiltro: Spinner

    private val listaHemocentros = mutableListOf<HemocentroListaItem>()
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hemocentros_proximos)

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

        recyclerView = findViewById(R.id.recyclerHemocentros)
        spinnerRegiaoFiltro = findViewById(R.id.spinnerRegiaoFiltro)
        spinnerUfFiltro = findViewById(R.id.spinnerUfFiltro)
        spinnerCidadeFiltro = findViewById(R.id.spinnerCidadeFiltro)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HemocentroAdapter(this, emptyList())
        recyclerView.adapter = adapter

        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        btnVoltar.setOnClickListener { finish() }

        carregarHemocentros()
    }

    // ---------------------------
    // Função segura para ler String ou Number do Firestore
    // ---------------------------
    private fun getStringSafe(doc: DocumentSnapshot, campo: String): String? {
        val valor = doc.get(campo)
        return when (valor) {
            is String -> valor
            is Number -> valor.toString()
            else -> null
        }
    }

    // ---------------------------
    // CARREGAR DADOS DO FIRESTORE
    // ---------------------------
    private fun carregarHemocentros() {
        firestore.collection("hemocentros")
            .get()
            .addOnSuccessListener { snapshot ->
                listaHemocentros.clear()

                for (doc in snapshot) {

                    val fantasia = doc.getString("fantasia")
                    val nomeJuridico = doc.getString("nome")
                    val nomeExibicao = when {
                        !fantasia.isNullOrBlank() -> fantasia
                        !nomeJuridico.isNullOrBlank() -> nomeJuridico
                        else -> "Hemocentro"
                    }

                    val logradouro = doc.getString("logradouro") ?: ""
                    val numero =
                        doc.getString("numero")
                            ?: doc.getString("NU_ENDERECO")
                            ?: ""

                    val enderecoCompleto = buildString {
                        append(logradouro)
                        if (numero.isNotBlank() && numero != "S/N") {
                            if (isNotEmpty()) append(", ")
                            append(numero)
                        }
                    }

                    val cidade = doc.getString("cidade") ?: doc.getString("municipio")
                    val uf = doc.getString("uf")
                    val cep = doc.getString("cep") ?: doc.getString("CEP")
                    val telefone = doc.getString("telefone")
                    val latitude = getStringSafe(doc, "latitude") ?: getStringSafe(doc, "Latitude")
                    val longitude = getStringSafe(doc, "longitude") ?: getStringSafe(doc, "Longitude")
                    val regiao = doc.getString("regiao") ?: doc.getString("Regiao")
                    val status = doc.getString("status") ?: doc.getString("STATUS")

                    listaHemocentros.add(
                        HemocentroListaItem(
                            id = doc.id,
                            nome = nomeExibicao,
                            endereco = enderecoCompleto,
                            cidade = cidade,
                            uf = uf,
                            cep = cep,
                            telefone = telefone,
                            latitude = latitude,
                            longitude = longitude,
                            regiao = regiao,
                            status = status
                        )
                    )
                }

                // Ordena por nome
                listaHemocentros.sortBy { it.nome.lowercase() }

                // Mostra tudo inicialmente
                adapter.atualizarLista(listaHemocentros)

                if (listaHemocentros.isEmpty()) {
                    mostrarSnackbar("Não há hemocentros cadastrados.", "#FF0000")
                } else {
                    configurarFiltros()
                }
            }
            .addOnFailureListener { e ->
                mostrarSnackbar("Erro ao carregar hemocentros: ${e.localizedMessage}", "#FF0000")
            }
    }

    // ---------------------------
    // CONFIGURAR FILTROS (REGIÃO / UF / CIDADE)
    // ---------------------------
    private fun configurarFiltros() {
        // REGIÃO
        val regioes = listaHemocentros
            .mapNotNull { it.regiao }
            .distinct()
            .sorted()

        val adapterRegiao = ArrayAdapter(
            this,
            R.layout.item_spinner_text,                 // item selecionado com texto preto/fundo branco
            listOf("Todas") + regioes
        )
        adapterRegiao.setDropDownViewResource(
            R.layout.item_spinner_dropdown_text        // dropdown com texto preto/fundo branco
        )
        spinnerRegiaoFiltro.adapter = adapterRegiao

        spinnerRegiaoFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val regiaoSel = parent.getItemAtPosition(position) as String
                atualizarUfFiltro(regiaoSel)
                filtrarLista()
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
                val ufSel = parent.getItemAtPosition(position) as String
                atualizarCidadeFiltro(ufSel)
                filtrarLista()
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
                filtrarLista()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Inicia UFs/cidades como "Todos"
        atualizarUfFiltro("Todas")
        atualizarCidadeFiltro("Todos")
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
        adapterUf.setDropDownViewResource(
            R.layout.item_spinner_dropdown_text
        )
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
        adapterCidade.setDropDownViewResource(
            R.layout.item_spinner_dropdown_text
        )
        spinnerCidadeFiltro.adapter = adapterCidade
    }

    private fun filtrarLista() {
        val regiaoSel = spinnerRegiaoFiltro.selectedItem as? String ?: "Todas"
        val ufSel = spinnerUfFiltro.selectedItem as? String ?: "Todos"
        val cidadeSel = spinnerCidadeFiltro.selectedItem as? String ?: "Todas"

        val filtrada = listaHemocentros.filter { item ->
            (regiaoSel == "Todas" || item.regiao == regiaoSel) &&
                    (ufSel == "Todos" || item.uf == ufSel) &&
                    (cidadeSel == "Todas" || item.cidade == cidadeSel)
        }

        adapter.atualizarLista(filtrada)
    }

    // ---------------------------
    // SNACKBAR
    // ---------------------------
    private fun mostrarSnackbar(msg: String, corHex: String) {
        val root = findViewById<View>(android.R.id.content)
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
            .setBackgroundTint(Color.parseColor(corHex))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()
    }
}

// ---------------------------
// ADAPTER DO RECYCLER
// ---------------------------
class HemocentroAdapter(
    private val context: Context,
    private var hemocentros: List<HemocentroListaItem>
) : RecyclerView.Adapter<HemocentroAdapter.HemocentroViewHolder>() {

    inner class HemocentroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeHemocentro)
        val txtEndereco: TextView = itemView.findViewById(R.id.txtEnderecoHemocentro)
        val txtCidadeUf: TextView = itemView.findViewById(R.id.txtCidadeUfHemocentro)
        val txtTelefone: TextView = itemView.findViewById(R.id.txtTelefoneHemocentro)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatusHemocentro)
        val btnLigar: Button = itemView.findViewById(R.id.btnLigarHemocentro)
        val btnMapa: Button = itemView.findViewById(R.id.btnMapaHemocentro)
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

        // Badge de STATUS
        val statusRaw = item.status?.trim()?.uppercase()
        val (label, colorHex) = when (statusRaw) {
            "OK", "ATIVO", "FUNCIONANDO" -> "ATIVO" to "#2E7D32" // verde
            "INATIVO", "FECHADO" -> "INATIVO" to "#C62828"      // vermelho
            null, "" -> "NÃO INFORMADO" to "#9E9E9E"           // cinza
            else -> statusRaw to "#9E9E9E"
        }

        holder.txtStatus.text = label
        ViewCompat.setBackgroundTintList(
            holder.txtStatus,
            ColorStateList.valueOf(Color.parseColor(colorHex))
        )

        // Clique no CARD → mostra detalhes
        holder.itemView.setOnClickListener {
            mostrarDialogDetalhes(item)
        }

        // Botão Ligar
        holder.btnLigar.setOnClickListener {
            ligarParaHemocentro(item)
        }

        // Botão Ver no mapa
        holder.btnMapa.setOnClickListener {
            abrirNoMapa(item)
        }
    }

    override fun getItemCount(): Int = hemocentros.size

    fun atualizarLista(novaLista: List<HemocentroListaItem>) {
        hemocentros = novaLista
        notifyDataSetChanged()
    }

    // ---------------------------
    // Detalhes em diálogo
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

    // ---------------------------
    // Abrir no Google Maps
    // ---------------------------
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

    // ---------------------------
    // Ligar para o hemocentro
    // ---------------------------
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

    // ---------------------------
    // Estilizar botões do diálogo
    // ---------------------------
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