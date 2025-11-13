package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityMeusAgendamentosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MeusAgendamentosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeusAgendamentosBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val listaAgendamentos = mutableListOf<AgendamentoItem>()
    private lateinit var adapter: MeusAgendamentosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeusAgendamentosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Remove ActionBar (faixa azul de cima)
        supportActionBar?.hide()

        // 🔹 Deixa status bar e navigation bar na cor do fundo
        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        // 🔹 Ícones escuros na status bar (modo claro)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setupRecycler()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        carregarAgendamentos()
    }

    private fun setupRecycler() {
        adapter = MeusAgendamentosAdapter(this, listaAgendamentos)
        binding.recyclerMeusAgendamentos.layoutManager = LinearLayoutManager(this)
        binding.recyclerMeusAgendamentos.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun carregarAgendamentos() {
        val usuario = auth.currentUser
        if (usuario == null) {
            binding.txtMensagemVazio.text = "Você precisa estar logado para ver seus agendamentos."
            binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
            binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
            return
        }

        firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                listaAgendamentos.clear()

                if (snapshot.isEmpty) {
                    binding.txtMensagemVazio.text = "Você ainda não possui agendamentos."
                    binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
                } else {
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val hemocentro = doc.getString("hemocentro") ?: "Hemocentro"
                        val data = doc.getString("data") ?: ""
                        val hora = doc.getString("hora") ?: ""
                        val endereco = doc.getString("endereco") ?: ""
                        val telefone = doc.getString("telefone") ?: ""
                        val status = doc.getString("status") ?: "Pendente"

                        val item = AgendamentoItem(
                            id = id,
                            hemocentro = hemocentro,
                            data = data,
                            hora = hora,
                            endereco = endereco,
                            telefone = telefone,
                            status = status
                        )
                        listaAgendamentos.add(item)
                    }

                    binding.txtMensagemVazio.visibility = android.view.View.GONE
                    binding.recyclerMeusAgendamentos.visibility = android.view.View.VISIBLE
                }

                adapter.atualizarLista(listaAgendamentos)
            }
            .addOnFailureListener {
                binding.txtMensagemVazio.text = "Erro ao carregar agendamentos."
                binding.txtMensagemVazio.visibility = android.view.View.VISIBLE
                binding.recyclerMeusAgendamentos.visibility = android.view.View.GONE
            }
    }

    /**
     * Mostra um diálogo genérico de erro ou aviso.
     */
    private fun mostrarDialogAvisoGenerico(title: String, message: String) {
        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()

        dialog.setOnShowListener {
            estilizarBotoesDialog(dialog, isDestructive = false)
        }

        dialog.show()
    }

    /**
     * Atualiza estilo de botões nos diálogos (padrão do app).
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
