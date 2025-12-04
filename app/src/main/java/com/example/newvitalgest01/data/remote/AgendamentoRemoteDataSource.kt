package com.example.newvitalgest01.data.remote

import com.example.newvitalgest01.domain.model.Agendamento
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

class AgendamentoRemoteDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val locale = Locale("pt", "BR")

    /**
     * Busca todos os agendamentos do usuário logado no Firestore
     * e converte para a model de domínio Agendamento.
     */
    suspend fun listarAgendamentosUsuarioAtual(): List<Agendamento> {
        val usuario = auth.currentUser ?: return emptyList()

        val snapshot = firestore.collection("usuarios")
            .document(usuario.uid)
            .collection("agendamentos")
            .get()
            .await()

        if (snapshot.isEmpty) return emptyList()

        return snapshot.documents.map { doc ->
            Agendamento(
                id = doc.id,
                data = doc.getString("data") ?: "",
                hora = doc.getString("hora") ?: "",
                hemocentro = doc.getString("hemocentro") ?: "",
                cidade = doc.getString("cidade") ?: "",
                estado = doc.getString("estado") ?: "",
                endereco = doc.getString("endereco") ?: "",
                telefone = doc.getString("telefone") ?: "",
                status = (doc.getString("status") ?: "").lowercase(locale)
            )
        }
    }
}