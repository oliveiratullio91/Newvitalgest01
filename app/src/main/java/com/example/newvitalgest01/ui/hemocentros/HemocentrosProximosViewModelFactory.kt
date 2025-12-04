package com.example.newvitalgest01.ui.hemocentros

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newvitalgest01.data.local.AppDatabase
import com.example.newvitalgest01.data.local.HemocentroLocalDataSource
import com.example.newvitalgest01.data.remote.HemocentroRemoteDataSource
import com.example.newvitalgest01.data.repository.HemocentroRepositoryImpl
import com.example.newvitalgest01.domain.repository.HemocentroRepository
import com.google.firebase.firestore.FirebaseFirestore

class HemocentrosProximosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HemocentrosProximosViewModel::class.java)) {

            // Firestore (remoto)
            val firestore = FirebaseFirestore.getInstance()
            val remote = HemocentroRemoteDataSource(firestore)

            // Room (local)
            val db = AppDatabase.getInstance(context)
            val hemoDao = db.hemocentroDao()
            val local = HemocentroLocalDataSource(hemoDao)

            // Repository combinando remoto + local
            val repo: HemocentroRepository = HemocentroRepositoryImpl(remote, local)

            return HemocentrosProximosViewModel(repo) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}