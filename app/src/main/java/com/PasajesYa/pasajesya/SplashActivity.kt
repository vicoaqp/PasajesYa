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


class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        // Debe ejecutarse antes de super.onCreate().
        installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Animación del logo.
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

        // Animación del nombre.
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

        // Animación de la descripción.
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

        // Animación del cargador.
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

            /*
             * Temporalmente abre MainActivity.
             * Cuando creemos LoginActivity, cambiaremos
             * MainActivity::class.java por LoginActivity::class.java.
             */
            val intent = Intent(
                this,
                MainActivity::class.java
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

        }, TIEMPO_SPLASH)
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }

    companion object {

        private const val TIEMPO_SPLASH = 1_800L
    }
}