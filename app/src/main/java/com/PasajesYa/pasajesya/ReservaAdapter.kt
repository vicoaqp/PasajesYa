package com.PasajesYa.pasajesya

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ItemReservaBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReservaAdapter(
    private val onVerDetalle: (Reserva) -> Unit,
    private val onCancelar: (Reserva) -> Unit
) : RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder>() {

    private val reservas =
        mutableListOf<Reserva>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReservaViewHolder {

        val binding = ItemReservaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ReservaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ReservaViewHolder,
        position: Int
    ) {

        holder.mostrar(
            reservas[position]
        )
    }

    override fun getItemCount(): Int {
        return reservas.size
    }

    fun actualizarReservas(
        nuevasReservas: List<Reserva>
    ) {

        reservas.clear()
        reservas.addAll(nuevasReservas)

        notifyDataSetChanged()
    }

    inner class ReservaViewHolder(
        private val binding: ItemReservaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun mostrar(reserva: Reserva) {

            binding.tvEmpresaReserva.text =
                reserva.empresaNombre.ifBlank {
                    "Empresa de transporte"
                }

            binding.tvRutaReserva.text =
                "${reserva.origen} → ${reserva.destino}"

            binding.tvAsientoReserva.text =
                reserva.asiento.ifBlank {
                    "—"
                }

            binding.tvPrecioReserva.text =
                String.format(
                    Locale("es", "PE"),
                    "S/ %.2f",
                    reserva.precio
                )

            binding.tvFechaReserva.text =
                obtenerFechaMostrada(reserva)

            binding.tvEstadoPago.text =
                "Pago: ${
                    reserva.estadoPago
                        .ifBlank { "pendiente" }
                        .lowercase(Locale.ROOT)
                }"

            configurarEstado(reserva)

            val puedeCancelar =
                reserva.estadoReserva
                    .trim()
                    .lowercase(Locale.ROOT) ==
                        ESTADO_PENDIENTE

            binding.btnCancelarReserva.visibility =
                if (puedeCancelar) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            binding.btnVerDetalle.setOnClickListener {
                onVerDetalle(reserva)
            }

            binding.btnCancelarReserva.setOnClickListener {
                onCancelar(reserva)
            }

            binding.cardReserva.setOnClickListener {
                onVerDetalle(reserva)
            }
        }

        private fun obtenerFechaMostrada(
            reserva: Reserva
        ): String {

            val fechaSalida =
                reserva.fechaSalida.trim()

            val horaSalida =
                reserva.horaSalida.trim()

            return when {

                fechaSalida.isNotBlank() &&
                        horaSalida.isNotBlank() -> {

                    "$fechaSalida · $horaSalida"
                }

                fechaSalida.isNotBlank() ->
                    fechaSalida

                horaSalida.isNotBlank() ->
                    "Salida: $horaSalida"

                reserva.fechaReserva != null -> {

                    val formato = SimpleDateFormat(
                        "dd MMM yyyy · hh:mm a",
                        Locale("es", "PE")
                    )

                    formato.format(
                        reserva.fechaReserva.toDate()
                    )
                }

                else ->
                    "Fecha por confirmar"
            }
        }

        private fun configurarEstado(
            reserva: Reserva
        ) {

            val contexto =
                binding.root.context

            val estado = reserva.estadoReserva
                .trim()
                .lowercase(Locale.ROOT)

            val texto: String
            val colorTexto: Int
            val colorFondo: Int

            when (estado) {

                ESTADO_PENDIENTE -> {

                    texto = "PENDIENTE"

                    colorTexto = contexto.getColor(
                        R.color.pasajes_warning
                    )

                    colorFondo = contexto.getColor(
                        R.color.pasajes_status_pending_bg
                    )
                }

                ESTADO_CONFIRMADA -> {

                    texto = "CONFIRMADA"

                    colorTexto = contexto.getColor(
                        R.color.pasajes_success
                    )

                    colorFondo = contexto.getColor(
                        R.color.pasajes_status_confirmed_bg
                    )
                }

                ESTADO_CANCELADA -> {

                    texto = "CANCELADA"

                    colorTexto = contexto.getColor(
                        R.color.pasajes_danger
                    )

                    colorFondo = contexto.getColor(
                        R.color.pasajes_status_cancelled_bg
                    )
                }

                ESTADO_COMPLETADA -> {

                    texto = "COMPLETADA"

                    colorTexto = contexto.getColor(
                        R.color.pasajes_primary
                    )

                    colorFondo = contexto.getColor(
                        R.color.pasajes_status_completed_bg
                    )
                }

                else -> {

                    texto = estado
                        .ifBlank { "SIN ESTADO" }
                        .uppercase(Locale.ROOT)

                    colorTexto = contexto.getColor(
                        R.color.pasajes_neutral
                    )

                    colorFondo = contexto.getColor(
                        R.color.pasajes_status_neutral_bg
                    )
                }
            }

            binding.tvEstadoReserva.text =
                texto

            binding.tvEstadoReserva.setTextColor(
                colorTexto
            )

            binding.tvEstadoReserva.backgroundTintList =
                ColorStateList.valueOf(
                    colorFondo
                )
        }
    }

    companion object {

        private const val ESTADO_PENDIENTE =
            "pendiente"

        private const val ESTADO_CONFIRMADA =
            "confirmada"

        private const val ESTADO_CANCELADA =
            "cancelada"

        private const val ESTADO_COMPLETADA =
            "completada"
    }
}