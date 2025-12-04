package com.example.newvitalgest01.data.remote

import com.example.newvitalgest01.domain.model.Hemocentro
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class HemocentroRemoteDataSource(
    private val firestore: FirebaseFirestore
) {

    private val locale = Locale("pt", "BR")

    suspend fun listarHemocentros(
        userLat: Double? = null,
        userLng: Double? = null
    ): List<Hemocentro> {
        val snapshot = firestore.collection("hemocentros")
            .get()
            .await()

        if (snapshot.isEmpty) return emptyList()

        val lista = mutableListOf<Hemocentro>()
        val chavesVistas = mutableSetOf<String>()
        val temLocalizacaoUsuario = userLat != null && userLng != null

        for (doc in snapshot) {
            val cidade = doc.getString("cidade") ?: doc.getString("municipio")
            val uf = doc.getString("uf")?.uppercase(locale)
            val regiao = (doc.getString("regiao") ?: doc.getString("Regiao"))?.uppercase(locale)

            if (cidade.isNullOrBlank() || uf.isNullOrBlank() || regiao.isNullOrBlank()) continue

            val cepRaw = doc.getString("cep") ?: doc.getString("CEP")
            val cep = cepRaw?.trim()
            val cepLimpo = cep?.filter { it.isDigit() }
            if (cepLimpo.isNullOrBlank() || cepLimpo.length < 8) continue

            val fantasia = doc.getString("fantasia")
            var nomeJuridico = doc.getString("nome")

            if (!nomeJuridico.isNullOrBlank()) {
                val padraoJuridico =
                    "(?i)\\b(LTDA|Ltda|LTDA\\.|S/A|SA|S A|ME|EPP|EMPRESA|INDÚSTRIA|INDUSTRIA|COMÉRCIO|COMERCIO)\\b"
                nomeJuridico = nomeJuridico
                    .replace(Regex(padraoJuridico), "")
                    .replace(Regex("\\(.*?\\)"), "")
                    .replace(Regex("\\s{2,}"), " ")
                    .trim()
            }

            val nomeBase = when {
                !fantasia.isNullOrBlank() -> fantasia.trim()
                !nomeJuridico.isNullOrBlank() -> nomeJuridico.trim()
                else -> "Hemocentro"
            }

            val nomeLimpo = nomeBase.replace(Regex("\\s{2,}"), " ").trim()
            if (nomeLimpo.isBlank()) continue

            val nomeUpper = nomeLimpo.uppercase(locale)
            val termosBloqueados = listOf(
                "HOSPITAL",
                "CLINICA",
                "CLÍNICA",
                "LABORATORIO",
                "LABORATÓRIO",
                "SERVICOS MEDICOS",
                "SERVIÇOS MEDICOS",
                "SERVIÇOS MÉDICOS",
                "DIAGNOSTICO",
                "DIAGNÓSTICO",
                "MULTIHEMO",
                "UNIHEMO"
            )
            if (termosBloqueados.any { termo -> nomeUpper.contains(termo) }) continue

            val statusRaw = doc.getString("status") ?: doc.getString("STATUS")
            val status = statusRaw?.trim()
            val statusUpper = status?.uppercase(locale)

            if (statusUpper != null) {
                val statusBloqueados = listOf("INATIVO", "FECHADO", "BLOQUEADO", "DESATIVADO")
                if (statusBloqueados.any { statusUpper.contains(it) }) continue
            }

            val chave = nomeLimpo.uppercase(locale) + "|" + cidade.trim().uppercase(locale)
            if (!chavesVistas.add(chave)) continue

            val logradouro = doc.getString("logradouro")
            val numero = doc.getString("numero") ?: doc.getString("NU_ENDERECO")
            val telefone = doc.getString("telefone")

            val horario = doc.getString("horario_funcionamento")
                ?: doc.getString("horario")
                ?: doc.getString("horarioFuncionamento")

            val lat = doc.getDouble("latitude") ?: doc.getDouble("lat")
            val lng = doc.getDouble("longitude") ?: doc.getDouble("lng")

            val distanciaKm = if (temLocalizacaoUsuario && lat != null && lng != null) {
                calcularDistanciaKm(userLat!!, userLng!!, lat, lng)
            } else {
                null
            }

            val hemo = Hemocentro(
                id = doc.id,
                nome = nomeLimpo,
                cidade = cidade,
                uf = uf,
                regiao = regiao,
                telefone = telefone,
                logradouro = logradouro,
                status = status,
                cep = cep,
                numero = numero,
                horarioFuncionamento = horario,
                latitude = lat,
                longitude = lng,
                distanciaKm = distanciaKm
            )

            lista.add(hemo)
        }

        return lista
    }

    // mesmo cálculo que você já usa na tela de agendamento
    private fun calcularDistanciaKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}