package com.example.newvitalgest01.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.newvitalgest01.databinding.ServicosItemBinding
import com.example.newvitalgest01.model.Servicos

class ServicosAdapter(private val Context: Context, private val ListaServicos: MutableList<Servicos>):
    RecyclerView.Adapter<ServicosAdapter.ServicosViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ServicosViewHolder {
        val itemLista = ServicosItemBinding.inflate(LayoutInflater.from(Context), parent, false)
        return ServicosViewHolder(itemLista)
    }

    override fun onBindViewHolder(
        holder: ServicosViewHolder,
        position: Int,
    ) {
        holder.imgServico.setImageResource(ListaServicos[position].img!!)
        holder.txtServico.text = ListaServicos[position].nome
    }

    override fun getItemCount() = ListaServicos.size

    inner class ServicosViewHolder(binding: ServicosItemBinding): RecyclerView.ViewHolder(binding.root){
        val imgServico = binding.imgServico
        val txtServico = binding.txtServico

    }
}