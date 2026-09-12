package com.rumahsehat.ui.result

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import com.rumahsehat.data.model.ScoreItem
import com.rumahsehat.ui.AssessmentViewModel
import com.rumahsehat.ui.ReviewActivity
import com.rumahsehat.ui.theme.RumahSehatTheme

/**
 * Layar Hasil Penilaian v1.7 — adaptasi prototype/hasil.png ke basis 17 item /
 * 810 poin. UI di ResultScreen.kt.
 */
class ResultActivity : ComponentActivity() {
    private val viewModel: AssessmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assessmentId = intent.getStringExtra(EXTRA_ID)
        if (assessmentId == null) {
            finish()
            return
        }
        viewModel.loadReview(assessmentId)
        enableEdgeToEdge()
        setContent {
            RumahSehatTheme {
                ResultScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onOpenDetail = { ReviewActivity.start(this, assessmentId) }
                )
            }
        }
    }

    companion object {
        const val EXTRA_ID = "assessment_id"
        fun start(context: Context, assessmentId: String) {
            context.startActivity(Intent(context, ResultActivity::class.java).putExtra(EXTRA_ID, assessmentId))
        }
    }
}
