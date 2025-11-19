package com.example.newvitalgest01.view

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
        private val txtVerDetalhes: TextView = itemView.findViewById(R.id.txtVerDetalhesItem)

        fun bind(item: AgendamentoItem) {
            txtHemocentro.text = item.hemocentro
            txtDataHora.text = "📅 ${item.data} às ${item.hora}"
            txtEndereco.text = "📍 ${item.endereco}"
            txtTelefone.text = if (item.telefone.isNotBlank()) {
                "☎ ${item.telefone}"
            } else {
                "☎ Telefone não informado"
            }

            val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dataHoraStr = "${item.data} ${item.hora}"
            val dataAgendada = try {
                formato.parse(dataHoraStr)
            } catch (e: Exception) {
                null
            }
            val agora = Date()

            val diffHoras = if (dataAgendada != null) {
                (dataAgendada.time - agora.time) / (1000 * 60 * 60)
            } else {
                0L
            }

            val expirado = dataAgendada != null && dataAgendada.before(agora)

            // -------------------------
            // Status (cores modernas)
            // -------------------------
            val statusExibicao = when {
                item.status.equals("Cancelado", ignoreCase = true) -> "Cancelado"
                expirado -> "Expirado"
                else -> item.status
            }

            txtStatus.text = statusExibicao

            // Fundo neutro (chip)
            txtStatus.setBackgroundResource(R.drawable.bg_status_chip_neutro)

            // Cores do texto conforme status
            when (statusExibicao.lowercase(Locale.getDefault())) {
                "pendente" -> {
                    txtStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.azul_info)
                    )
                }
                "confirmado" -> {
                    txtStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.holo_green_dark)
                    )
                }
                "cancelado" -> {
                    txtStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.vermelho_primario)
                    )
                }
                "expirado" -> {
                    txtStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.darker_gray)
                    )
                }
                else -> {
                    txtStatus.setTextColor(
                        ContextCompat.getColor(context, android.R.color.black)
                    )
                }
            }

            // -------------------------
            // Botão Cancelar (regra 48h)
            // -------------------------

            // Se já estiver cancelado ou expirado -> esconde botão
            if (statusExibicao.equals("Cancelado", true) || statusExibicao.equals("Expirado", true)) {
                btnCancelar.visibility = View.GONE
            } else {
                btnCancelar.visibility = View.VISIBLE
                // Estilo consistente
                btnCancelar.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.vermelho_primario)
                )
                btnCancelar.setTextColor(Color.WHITE)

                if (diffHoras > 48) {
                    // Pode cancelar normalmente
                    btnCancelar.setOnClickListener {
                        confirmarCancelamento(item)
                    }
                } else {
                    // Menos de 48h -> mostrar aviso
                    btnCancelar.setOnClickListener {
                        aviso48h(item)
                    }
                }
            }

            // -------------------------
            // Ver detalhes (resumo)
            // -------------------------
            txtVerDetalhes.setOnClickListener {
                val mensagem = buildString {
                    appendLine("🏥 ${item.hemocentro}")
                    appendLine("📅 ${item.data} às ${item.hora}")
                    appendLine("📍 ${item.endereco}")
                    if (item.telefone.isNotBlank()) {
                        appendLine("☎ ${item.telefone}")
                    }
                    appendLine()
                    appendLine("Status atual: $statusExibicao")
                }

                MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogTheme)
                    .setTitle("Detalhes do agendamento")
                    .setMessage(mensagem)
                    .setPositiveButton("OK") { d, _ -> d.dismiss() }
                    .show()
            }
        }

        /**
         * Dialog intermediário com motivo de cancelamento (sugestões 3 e 4).
         */
        private fun confirmarCancelamento(item: AgendamentoItem) {
            val dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_cancelar_agendamento, null)

            val txtInfo = dialogView.findViewById<TextView>(R.id.txtInfoAgendamento)
            val rgMotivo = dialogView.findViewById<RadioGroup>(R.id.rgMotivoCancelamento)
            val edtMotivoOutro = dialogView.findViewById<EditText>(R.id.edtMotivoOutro)

            val resumo = buildString {
                appendLine("🏥 ${item.hemocentro}")
                appendLine("📅 ${item.data} às ${item.hora}")
                appendLine("📍 ${item.endereco}")
            }
            txtInfo.text = resumo

            // Mostrar campo "outro motivo" apenas quando marcado
            rgMotivo.setOnCheckedChangeListener { _, checkedId ->
                edtMotivoOutro.visibility =
                    if (checkedId == R.id.rbOutroMotivo) View.VISIBLE else View.GONE
            }

            val dialog = MaterialAlertDialogBuilder(context, R.style.CustomAlertDialogTheme)
                .setTitle("Cancelar agendamento")
                .setView(dialogView)
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Confirmar cancelamento") { d, _ ->
                    val motivo = obterMotivoSelecionado(rgMotivo, edtMotivoOutro)

                    val usuario = auth.currentUser
                    if (usuario != null) {
                        val dadosAtualizacao = mapOf(
                            "status" to "Cancelado",
                            "motivoCancelamento" to motivo,
                            "canceladoEm" to FieldValue.serverTimestamp()
                        )

                        firestore.collection("usuarios")
                            .document(usuario.uid)
                            .collection("agendamentos")
                            .document(item.id)
                            .update(dadosAtualizacao)
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
                .create()

            dialog.show()
        }

        private fun obterMotivoSelecionado(
            rgMotivo: RadioGroup,
            edtMotivoOutro: EditText
        ): String {
            val selectedId = rgMotivo.checkedRadioButtonId
            return when (selectedId) {
                R.id.rbNaoComparecerei -> "Não poderei comparecer"
                R.id.rbErreiData -> "Errei a data/horário"
                R.id.rbMudancaPlanos -> "Mudança de planos"
                R.id.rbOutroMotivo -> {
                    val textoOutro = edtMotivoOutro.text.toString().trim()
                    if (textoOutro.isNotEmpty()) textoOutro else "Outro motivo"
                }
                else -> "Não informado"
            }
        }

        /**
         * Aviso quando estiver a menos de 48h (sugestão 2B + 5).
         */
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

        /**
         * Snackbar estilizada (sugestão 6).
         */
        private fun mostrarMensagem(mensagem: String) {
            Snackbar.make(itemView, mensagem, Snackbar.LENGTH_LONG)
                .setBackgroundTint(
                    ContextCompat.getColor(context, R.color.vermelho_primario)
                )
                .setTextColor(Color.WHITE)
                .show()
        }
    }
}