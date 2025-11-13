package com.example.newvitalgest01.view

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MeusAgendamentosAdapter(
    private val context: Context,
    private var itens: List<AgendamentoItem>
) : RecyclerView.Adapter<MeusAgendamentosAdapter.AgendamentoViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agendamento, parent, false)
        return AgendamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgendamentoViewHolder, position: Int) {
        val item = itens[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itens.size

    fun atualizarLista(novaLista: List<AgendamentoItem>) {
        itens = novaLista
        notifyDataSetChanged()
    }

    inner class AgendamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtHemocentro: TextView = itemView.findViewById(R.id.txtHemocentroItem)
        private val txtDataHora: TextView = itemView.findViewById(R.id.txtDataHoraItem)
        private val txtEndereco: TextView = itemView.findViewById(R.id.txtEnderecoItem)
        private val txtTelefone: TextView = itemView.findViewById(R.id.txtTelefoneItem)
        private val txtStatus: TextView = itemView.findViewById(R.id.txtStatusItem)
        private val btnCancelar: Button = itemView.findViewById(R.id.btnCancelarAgendamento)

        fun bind(item: AgendamentoItem) {
            txtHemocentro.text = item.hemocentro
            txtDataHora.text = "📅 ${item.data} às ${item.hora}"
            txtEndereco.text = "📍 ${item.endereco}"
            txtTelefone.text = "☎ ${item.telefone}"
            txtStatus.text = item.status

            val dataHoraStr = "${item.data} ${item.hora}"
            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dataAgendada = formato.parse(dataHoraStr)
            val agora = Date()

            val diffHoras = if (dataAgendada != null) {
                (dataAgendada.time - agora.time) / (1000 * 60 * 60)
            } else {
                0L
            }

            // Se já estiver cancelado, esconde o botão
            if (item.status.equals("Cancelado", ignoreCase = true)) {
                btnCancelar.visibility = View.GONE
                txtStatus.text = "Cancelado"
                return
            }

            // 🔴 Garante que o botão sempre fique vermelho com texto branco
            btnCancelar.setBackgroundColor(
                ContextCompat.getColor(context, R.color.vermelho_primario)
            )
            btnCancelar.setTextColor(Color.WHITE)

            // Mostra o botão e define comportamento
            if (diffHoras > 48) {
                btnCancelar.visibility = View.VISIBLE
                btnCancelar.setOnClickListener { confirmarCancelamento(item) }
            } else {
                btnCancelar.visibility = View.VISIBLE
                btnCancelar.setOnClickListener { aviso48h(item) }
            }
        }

        private fun confirmarCancelamento(item: AgendamentoItem) {
            val mensagem = buildString {
                appendLine("Tem certeza de que deseja cancelar este agendamento?")
                appendLine()
                appendLine("🏥 ${item.hemocentro}")
                appendLine("📅 ${item.data} às ${item.hora}")
            }

            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogTheme)
                .setTitle("Cancelar agendamento")
                .setMessage(mensagem)
                .setCancelable(false)
                .setNegativeButton("Voltar") { d, _ -> d.dismiss() }
                .setPositiveButton("Sim, cancelar") { d, _ ->
                    val usuario = auth.currentUser
                    if (usuario != null) {
                        firestore.collection("usuarios")
                            .document(usuario.uid)
                            .collection("agendamentos")
                            .document(item.id)
                            .update("status", "Cancelado")
                            .addOnSuccessListener {
                                mostrarMensagem("Agendamento cancelado com sucesso.")
                            }
                            .addOnFailureListener {
                                mostrarMensagem("Erro ao cancelar o agendamento.")
                            }
                    } else {
                        mostrarMensagem("Usuário não autenticado.")
                    }
                    d.dismiss()
                }
                .show()
        }

        private fun aviso48h(item: AgendamentoItem) {
            val mensagem = buildString {
                appendLine("O agendamento só pode ser cancelado com até 48h de antecedência.")
                appendLine()
                appendLine("Se precisar cancelar, entre em contato diretamente com o hemocentro:")
                appendLine("🏥 ${item.hemocentro}")
                if (item.telefone.isNotBlank()) {
                    appendLine("☎ ${item.telefone}")
                }
            }

            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogTheme)
                .setTitle("Cancelamento indisponível")
                .setMessage(mensagem)
                .setPositiveButton("Entendi") { d, _ -> d.dismiss() }
                .show()
        }

        private fun mostrarMensagem(mensagem: String) {
            MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogTheme)
                .setTitle("VitalGest")
                .setMessage(mensagem)
                .setPositiveButton("OK") { d, _ -> d.dismiss() }
                .show()
        }
    }
}
