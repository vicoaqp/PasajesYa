package com.PasajesYa.pasajesya

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.PasajesYa.pasajesya.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth


class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        // Siempre antes de super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        configurarPantalla()
        iniciarAnimaciones()
        programarNavegacion()
    }

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_primary_dark)
    }

    private fun iniciarAnimaciones() {

        binding.contenedorLogo.apply {

            alpha = 0f
            scaleX = 0.55f
            scaleY = 0.55f
            rotation = -8f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(700L)
                .start()
        }

        binding.contenedorNombre.apply {

            alpha = 0f
            translationY = 35f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(250L)
                .setDuration(550L)
                .start()
        }

        binding.tvDescripcion.apply {

            alpha = 0f
            translationY = 22f

            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(450L)
                .setDuration(500L)
                .start()
        }

        binding.progressSplash.apply {

            alpha = 0f

            animate()
                .alpha(1f)
                .setStartDelay(700L)
                .setDuration(350L)
                .start()
        }

        binding.tvCargando.apply {

            alpha = 0f

            animate()
                .alpha(1f)
                .setStartDelay(800L)
                .setDuration(350L)
                .start()
        }
    }

    private fun programarNavegacion() {

        handler.postDelayed({

            if (isFinishing || isDestroyed) {
                return@postDelayed
            }

            verificarSesion()

        }, TIEMPO_SPLASH)
    }

    private fun verificarSesion() {

        val usuarioActual = auth.currentUser

        val pantallaDestino = if (usuarioActual == null) {

            // No inició sesión
            LoginActivity::class.java

        } else {

            // Ya inició sesión
            SeleccionarEmpresaActivity::class.java
        }

        val intent = Intent(
            this,
            pantallaDestino
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        finish()
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }

    companion object {

        private const val TIEMPO_SPLASH = 1_800L
    }
}