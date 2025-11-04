package com.example.newvitalgest01.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newvitalgest01.R
import com.example.newvitalgest01.databinding.ActivityMeusAgendamentosBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.animation.AnimationUtils


class MeusAgendamentosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMeusAgendamentosBinding

    private val listaCompleta = mutableListOf<AgendamentoItem>()
    private val listaFiltrada = mutableListOf<AgendamentoItem>()
    private lateinit var adapter: AgendamentoAdapter

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val hemocentros = mapOf(
        "HEMOPE Recife" to mapOf(
            "endereco" to "Rua Joaquim Nabuco, 171 - Graças",
            "telefone" to "(81) 3416-4800"
        ),
        "Hospital das Clínicas" to mapOf(
            "endereco" to "Av. Prof. Moraes Rego, 1235 - Cidade Universitária",
            "telefone" to "(81) 2126-3600"
        ),
        "IMIP" to mapOf(
            "endereco" to "Rua dos Coelhos, 300 - Boa Vista",
            "telefone" to "(81) 2122-4700"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeusAgendamentosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        configurarRecycler()
        configurarFiltro()
        configurarBotoes()
        carregarAgendamentosDoFirebase()
    }

    private fun configurarRecycler() {
        adapter = AgendamentoAdapter(listaFiltrada) { item, position ->
            mostrarDialogoCancelar(item, position)
        }
        binding.recyclerAgendamentos.layoutManager = LinearLayoutManager(this)
        binding.recyclerAgendamentos.adapter = adapter
    }

    private fun configurarFiltro() {
        binding.edtFiltroAgendamentos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarAgendamentos(s?.toString() ?: "")
            }
        })
    }

    private fun configurarBotoes() {
        binding.btnVoltarMeusAgendamentos.setOnClickListener {
            finish()
        }
    }

    private fun filtrarAgendamentos(texto: String) {
        val consulta = texto.lowercase().trim()

        listaFiltrada.clear()

        if (consulta.isEmpty()) {
            listaFiltrada.addAll(listaCompleta)
        } else {
            listaFiltrada.addAll(
                listaCompleta.filter { item ->
                    item.hemocentro.lowercase().contains(consulta) ||
                            item.data.lowercase().contains(consulta) ||
                            item.hora.lowercase().contains(consulta)
                }
            )
        }

        adapter.notifyDataSetChanged()
    }

    private fun carregarAgendamentosDoFirebase() {
        val user = auth.currentUser ?: return

        firestore.collection("usuarios")
            .document(user.uid)
            .collection("agendamentos")
            .orderBy("data")
            .orderBy("hora")
            .get()
            .addOnSuccessListener { docs ->
                listaCompleta.clear()

                for (doc in docs) {
                    val id = doc.id
                    val hemocentro = doc.getString("hemocentro") ?: ""
                    val data = doc.getString("data") ?: ""
                    val hora = doc.getString("hora") ?: ""
                    val status = doc.getString("status") ?: "Agendado"

                    val info = hemocentros[hemocentro]
                    val endereco = info?.get("endereco") ?: ""
                    val telefone = info?.get("telefone") ?: ""

                    listaCompleta.add(
                        AgendamentoItem(
                            id = id,
                            hemocentro = hemocentro,
                            data = data,
                            hora = hora,
                            endereco = endereco,
                            telefone = telefone,
                            status = status
                        )
                    )
                }

                listaFiltrada.clear()
                listaFiltrada.addAll(listaCompleta)
                adapter.notifyDataSetChanged()
            }
    }

    // ---------------- CANCELAMENTO ----------------

    private fun mostrarDialogoCancelar(item: AgendamentoItem, position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_cancel, null)

        val txtMensagem = dialogView.findViewById<TextView>(R.id.txtMensagemConfirmacao)
        val btnNao = dialogView.findViewById<Button>(R.id.btnNaoCancelar)
        val btnSim = dialogView.findViewById<Button>(R.id.btnSimCancelar)

        txtMensagem.text =
            "Tem certeza que deseja cancelar a doação no hemocentro \"${item.hemocentro}\" no dia ${item.data} às ${item.hora}?"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Fundo da janela transparente (sem quadrado branco)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnNao.setOnClickListener {
            dialog.dismiss()
        }

        btnSim.setOnClickListener {
            dialog.dismiss()
            cancelarAgendamento(item, position)
        }

        dialog.show()

        // Animação de entrada (fade + zoom leve)
        val anim = AnimationUtils.loadAnimation(this, R.anim.dialog_enter)
        dialogView.startAnimation(anim)
    }


    private fun cancelarAgendamento(item: AgendamentoItem, position: Int) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_LONG).show()
            return
        }

        // 🔹 1) Atualiza a UI imediatamente (remoção otimista)
        listaCompleta.removeAll { it.id == item.id }

        val indexNaFiltrada = listaFiltrada.indexOfFirst { it.id == item.id }
        if (indexNaFiltrada != -1) {
            listaFiltrada.removeAt(indexNaFiltrada)
            adapter.notifyItemRemoved(indexNaFiltrada)
        } else {
            // fallback, se algo sair do esperado
            val textoFiltro = binding.edtFiltroAgendamentos.text?.toString() ?: ""
            filtrarAgendamentos(textoFiltro)
        }

        // 🔹 2) Chama o Firestore para realmente apagar
        firestore.collection("usuarios")
            .document(user.uid)
            .collection("agendamentos")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                // Mensagem mais bonita usando Snackbar
                val snackbar = Snackbar.make(
                    binding.root,
                    "Doação cancelada com sucesso.",
                    Snackbar.LENGTH_LONG
                )
                snackbar.show()
            }
            .addOnFailureListener {
                // Se falhar, avisa o usuário e recarrega a lista para corrigir
                Toast.makeText(
                    this,
                    "Erro ao cancelar agendamento. Atualizando lista...",
                    Toast.LENGTH_LONG
                ).show()
                carregarAgendamentosDoFirebase()
            }
    }
}