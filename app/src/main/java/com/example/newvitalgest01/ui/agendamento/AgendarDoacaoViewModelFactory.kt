package com.example.newvitalgest01.ui.agendamento

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newvitalgest01.data.local.AgendamentoLocalDataSource
import com.example.newvitalgest01.data.local.AppDatabase
import com.example.newvitalgest01.data.local.HemocentroLocalDataSource
import com.example.newvitalgest01.data.remote.AgendamentoRemoteDataSource
import com.example.newvitalgest01.data.remote.HemocentroRemoteDataSource
import com.example.newvitalgest01.data.repository.AgendamentoRepositoryImpl
import com.example.newvitalgest01.data.repository.HemocentroRepositoryImpl
import com.example.newvitalgest01.domain.repository.AgendamentoRepository
import com.example.newvitalgest01.domain.repository.HemocentroRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AgendarDoacaoViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgendarDoacaoViewModel::class.java)) {

            // Firebase
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()

            // Remote data sources
            val hemoRemote = HemocentroRemoteDataSource(firestore)
            val agRemote = AgendamentoRemoteDataSource(auth, firestore)

            // Room + DAOs
            val db = AppDatabase.getInstance(context)
            val hemoDao = db.hemocentroDao()
            val agendamentoDao = db.agendamentoDao()

            // Local data sources usando Room
            val hemoLocal = HemocentroLocalDataSource(hemoDao)
            val agLocal = AgendamentoLocalDataSource(agendamentoDao)

            // Repositórios
            val hemoRepo: HemocentroRepository = HemocentroRepositoryImpl(hemoRemote, hemoLocal)
            val agRepo: AgendamentoRepository = AgendamentoRepositoryImpl(agRemote, agLocal)

            return AgendarDoacaoViewModel(
                hemocentroRepository = hemoRepo,
                agendamentoRepository = agRepo
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}