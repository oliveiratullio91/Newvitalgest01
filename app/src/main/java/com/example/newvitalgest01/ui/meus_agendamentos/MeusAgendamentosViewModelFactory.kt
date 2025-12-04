package com.example.newvitalgest01.ui.meus_agendamentos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newvitalgest01.data.local.AgendamentoLocalDataSource
import com.example.newvitalgest01.data.local.AppDatabase
import com.example.newvitalgest01.data.remote.AgendamentoRemoteDataSource
import com.example.newvitalgest01.data.repository.AgendamentoRepositoryImpl
import com.example.newvitalgest01.domain.repository.AgendamentoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MeusAgendamentosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeusAgendamentosViewModel::class.java)) {

            // Firebase (remoto)
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val remote = AgendamentoRemoteDataSource(auth, firestore)

            // Room (local)
            val db = AppDatabase.getInstance(context)
            val agendamentoDao = db.agendamentoDao()
            val local = AgendamentoLocalDataSource(agendamentoDao)

            // Repository combinando remoto + local
            val repo: AgendamentoRepository = AgendamentoRepositoryImpl(remote, local)

            return MeusAgendamentosViewModel(repo) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}