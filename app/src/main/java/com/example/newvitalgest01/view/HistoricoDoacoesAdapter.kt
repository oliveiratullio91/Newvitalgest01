package com.example.newvitalgest01.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R

class HistoricoDoacoesAdapter(
    private val listaDoacoes: List<DoacaoHistoricoItem>
) : RecyclerView.Adapter<HistoricoDoacoesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtHemocentro: TextView = itemView.findViewById(R.id.txtHemocentroHistorico)
        val txtDataHora: TextView = itemView.findViewById(R.id.txtDataHoraHistorico)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatusHistorico)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historico_doacao, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaDoacoes[position]
        holder.txtHemocentro.text = item.hemocentro
        holder.txtDataHora.text = "${item.data} • ${item.hora}"
        holder.txtStatus.text = item.status
    }

    override fun getItemCount(): Int = listaDoacoes.size
}