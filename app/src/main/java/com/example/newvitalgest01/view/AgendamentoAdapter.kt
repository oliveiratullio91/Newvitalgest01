package com.example.newvitalgest01.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R

class AgendamentoAdapter(
    private val itens: MutableList<AgendamentoItem>,
    private val onCancelarClick: (AgendamentoItem, Int) -> Unit
) : RecyclerView.Adapter<AgendamentoAdapter.AgendamentoViewHolder>() {

    inner class AgendamentoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtHemocentro: TextView = view.findViewById(R.id.txtHemocentro)
        val txtData: TextView = view.findViewById(R.id.txtData)
        val txtHora: TextView = view.findViewById(R.id.txtHora)
        val txtEndereco: TextView = view.findViewById(R.id.txtEndereco)
        val txtTelefone: TextView = view.findViewById(R.id.txtTelefone)
        val txtStatus: TextView = view.findViewById(R.id.txtStatus)
        val btnCancelar: Button = view.findViewById(R.id.btnCancelarAgendamento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agendamento, parent, false)
        return AgendamentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgendamentoViewHolder, position: Int) {
        val item = itens[position]

        holder.txtHemocentro.text = item.hemocentro
        holder.txtData.text = "Data: ${item.data}"
        holder.txtHora.text = "Hora: ${item.hora}"
        holder.txtEndereco.text = item.endereco
        holder.txtTelefone.text = "Telefone: ${item.telefone}"
        holder.txtStatus.text = item.status

        holder.btnCancelar.setOnClickListener {
            val pos = holder.adapterPosition   // 👈 trocado de bindingAdapterPosition para adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onCancelarClick(itens[pos], pos)
            }
        }
    }

    override fun getItemCount(): Int = itens.size
}