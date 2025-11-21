package com.example.gestaobilhares.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gestaobilhares.R
import com.example.gestaobilhares.MainActivity

class NotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID_APPROVALS = "approvals_channel"
        const val CHANNEL_ID_ALERTS = "alerts_channel"
        const val CHANNEL_ID_GENERAL = "general_channel"
        
        const val NOTIFICATION_ID_APPROVAL = 1001
        const val NOTIFICATION_ID_META_ALERT = 1002
        const val NOTIFICATION_ID_GENERAL = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val approvalChannel = NotificationChannel(
                CHANNEL_ID_APPROVALS,
                "Aprovações",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de aprovação de colaboradores"
                enableVibration(true)
                enableLights(true)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Alertas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de metas e performance"
                enableVibration(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Geral",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações gerais do sistema"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(approvalChannel, alertChannel, generalChannel))
        }
    }

    fun showApprovalNotification(colaboradorNome: String, colaboradorId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("fragment", "colaborador_management")
            putExtra("colaborador_id", colaboradorId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_APPROVALS)
            .setSmallIcon(R.drawable.ic_person)
            .setContentTitle("Novo Colaborador Aguardando Aprovação")
            .setContentText("$colaboradorNome solicitou acesso ao sistema")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_check,
                "Aprovar",
                createApprovalAction(colaboradorId, true)
            )
            .addAction(
                R.drawable.ic_close,
                "Rejeitar",
                createApprovalAction(colaboradorId, false)
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_APPROVAL, notification)
    }

    fun showMetaAlertNotification(metaDescricao: String, valorAtual: Double, valorMeta: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("fragment", "reports")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percentual = (valorAtual / valorMeta) * 100
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Alerta de Meta")
            .setContentText("Meta '$metaDescricao': ${String.format("%.1f", percentual)}% atingida")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_META_ALERT, notification)
    }

    fun showGeneralNotification(titulo: String, mensagem: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.ic_info)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GENERAL, notification)
    }

    private fun createApprovalAction(colaboradorId: Long, aprovado: Boolean): PendingIntent {
        // TODO: Implementar NotificationActionReceiver
        val intent = Intent(context, MainActivity::class.java).apply {
            action = if (aprovado) "APPROVE_COLABORADOR" else "REJECT_COLABORADOR"
            putExtra("colaborador_id", colaboradorId)
        }

        return PendingIntent.getActivity(
            context,
            colaboradorId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}

// BroadcastReceiver para lidar com ações de notificação
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Implementação futura para lidar com ações de notificação
        // Por enquanto, apenas um stub para resolver o erro de compilação
        android.util.Log.d("NotificationActionReceiver", "Ação recebida: ${intent?.action}")
    }
}

