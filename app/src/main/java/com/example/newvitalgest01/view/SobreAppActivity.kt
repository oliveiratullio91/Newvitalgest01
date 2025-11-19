package com.example.newvitalgest01.view

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.newvitalgest01.R
import com.google.android.material.button.MaterialButton

class SobreAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sobre_app)

        supportActionBar?.hide()

        window.statusBarColor = ContextCompat.getColor(this, R.color.fundo_claro)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.fundo_claro)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        val btnTermos = findViewById<MaterialButton>(R.id.btnTermos)
        val btnFechar = findViewById<MaterialButton>(R.id.btnFecharSobre)

        btnTermos.setOnClickListener {
            mostrarDialogoTermos()
        }

        btnFechar.setOnClickListener {
            finish()
        }
    }

    private fun mostrarDialogoTermos() {
        val termosTexto = """
            Este documento apresenta um resumo dos Termos de Uso e da Política de Privacidade (LGPD) aplicáveis ao aplicativo VitalGest.

            1. Objetivo do aplicativo
            O VitalGest foi desenvolvido para auxiliar usuários no acompanhamento e agendamento de doações de sangue, fornecendo informações, lembretes e registro histórico de doações.
            O aplicativo tem caráter informativo e de apoio, não substituindo, em nenhuma hipótese, a avaliação médica, a triagem clínica oficial ou as orientações fornecidas pelos hemocentros e serviços de saúde.

            2. Uso do aplicativo
            • O usuário se compromete a utilizar o aplicativo de forma responsável e verdadeira, fornecendo informações corretas no cadastro e nas funcionalidades utilizadas.
            • É proibido o uso do aplicativo para fins ilícitos, fraudulentos ou que violem direitos de terceiros.
            • O usuário é responsável por manter a confidencialidade de seus dados de acesso (e-mail e senha).

            3. Limitações e isenção de responsabilidade
            • As informações exibidas no aplicativo têm caráter educativo e de apoio ao agendamento de doações.
            • O VitalGest não realiza atendimento de urgência, nem substitui serviços médicos, hospitais ou hemocentros.
            • A elegibilidade final para doação de sangue é sempre determinada pelo serviço de hemoterapia responsável, durante a triagem presencial.
            • O desenvolvedor do aplicativo não se responsabiliza por decisões tomadas exclusivamente com base nas informações apresentadas no app, sem consulta a profissionais ou serviços de saúde.

            4. Dados pessoais coletados
            Podem ser coletados e armazenados dados como:
            • Nome completo;
            • E-mail e telefone;
            • Data de nascimento, sexo e peso;
            • Cidade e estado;
            • Informações sobre doações (histórico, agendamentos, preferências);
            • Preferências de comunicação.

            Esses dados são utilizados para:
            • criação e gerenciamento da conta do usuário;
            • exibição de informações personalizadas (como intervalo entre doações);
            • envio de notificações e lembretes, quando autorizado;
            • melhoria da experiência de uso e estatísticas internas (sem identificação direta).

            5. Tratamento de dados (LGPD – Lei nº 13.709/2018)
            O tratamento de dados pessoais observa os princípios da LGPD:
            • finalidade, necessidade, adequação, transparência e segurança.
            Os dados são utilizados apenas para finalidades relacionadas ao funcionamento do aplicativo.

            6. Compartilhamento de dados
            • Os dados não são vendidos ou compartilhados com terceiros para fins comerciais.
            • Poderá haver compartilhamento apenas com provedores de serviços técnicos (autenticação, banco de dados, hospedagem), para viabilizar o funcionamento do app, com medidas de segurança e confidencialidade.
            • Caso haja integração futura com hemocentros ou parceiros, o usuário será informado de forma clara e, quando exigido, será solicitado novo consentimento.

            7. Direitos do usuário
            O usuário poderá:
            • acessar seus dados pessoais;
            • solicitar correção de dados;
            • solicitar exclusão de dados, quando aplicável;
            • revogar consentimentos, ciente de que isso pode limitar funcionalidades.

            Para exercer esses direitos, o usuário poderá entrar em contato pelo e-mail:
            contato@vitalgest.app

            8. Armazenamento e segurança
            • Os dados são armazenados em serviços de nuvem com padrões de segurança reconhecidos.
            • Medidas técnicas e organizacionais são adotadas para reduzir riscos de acesso não autorizado, vazamento ou alteração indevida.
            • Em eventual incidente relevante, as medidas cabíveis serão adotadas e, quando exigido, usuários e autoridades serão informados.

            9. Menores de idade
            • O uso do app por menores de 18 anos deve ocorrer com conhecimento e acompanhamento de pais ou responsáveis.
            • Quando exigido por normas específicas, o uso dependerá de consentimento do responsável legal.

            10. Atualizações destes Termos
            Estes Termos de Uso e a Política de Privacidade poderão ser atualizados periodicamente.
            Mudanças relevantes poderão ser comunicadas por meio do aplicativo ou por e-mail, quando adequado.

            11. Contato
            Em caso de dúvidas, solicitações ou reclamações sobre estes Termos ou sobre o tratamento de dados pessoais, o usuário poderá entrar em contato pelo e-mail:
            contato@vitalgest.app

            Ao utilizar o aplicativo e aceitar os Termos de Uso e a Política de Privacidade, o usuário declara estar ciente e de acordo com as condições acima.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Termos de Uso e Política de Privacidade")
            .setMessage(termosTexto)
            .setPositiveButton("Fechar", null)
            .show()
    }
}