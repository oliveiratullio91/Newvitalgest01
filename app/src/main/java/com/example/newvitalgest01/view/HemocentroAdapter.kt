package com.example.newvitalgest01.view

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.R

class HemocentroAdapter(
    private val listaHemocentros: List<HemocentroItem>
) : RecyclerView.Adapter<HemocentroAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNome: TextView = itemView.findViewById(R.id.txtNomeHemocentro)
        val txtEndereco: TextView = itemView.findViewById(R.id.txtEnderecoHemocentro)
        val txtTelefone: TextView = itemView.findViewById(R.id.txtTelefoneHemocentro)
        val btnLigar: Button = itemView.findViewById(R.id.btnLigarHemocentro)
        val btnMapa: Button = itemView.findViewById(R.id.btnMapaHemocentro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hemocentro, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaHemocentros[position]

        holder.txtNome.text = item.nome
        holder.txtEndereco.text = item.endereco
        holder.txtTelefone.text = item.telefone

        holder.btnLigar.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${item.telefone}")
            context.startActivity(intent)
        }

        holder.btnMapa.setOnClickListener {
            val context = holder.itemView.context
            // Usa o endereço no Maps
            val uri = Uri.encode(item.endereco)
            val gmmIntentUri = Uri.parse("geo:0,0?q=$uri")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            context.startActivity(mapIntent)
        }
    }

    override fun getItemCount(): Int = listaHemocentros.size
}