package com.PasajesYa.pasajesya
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ItemViajeBinding
import java.util.Locale

class ViajeAdapter(
    private val onReservar: (Viaje) -> Unit
) : RecyclerView.Adapter<ViajeAdapter.ViajeViewHolder>() {

    private val viajes = mutableListOf<Viaje>()

    private var colorPrincipal: Int =
        Color.parseColor("#1261A6")

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViajeViewHolder {

        val binding = ItemViajeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViajeViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViajeViewHolder,
        position: Int
    ) {
        holder.mostrar(viajes[position])
    }

    override fun getItemCount(): Int {
        return viajes.size
    }

    fun actualizarViajes(nuevosViajes: List<Viaje>) {

        viajes.clear()
        viajes.addAll(nuevosViajes)

        notifyDataSetChanged()
    }

    fun actualizarColorEmpresa(colorHexadecimal: String) {

        colorPrincipal = try {
            Color.parseColor(colorHexadecimal)
        } catch (_: IllegalArgumentException) {
            Color.parseColor("#1261A6")
        }

        notifyDataSetChanged()
    }

    inner class ViajeViewHolder(
        private val binding: ItemViajeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun mostrar(viaje: Viaje) {

            val capacidadSegura =
                viaje.capacidad.coerceAtLeast(1L)

            val ocupadosSeguros =
                viaje.asientosOcupados.coerceIn(
                    0L,
                    capacidadSegura
                )

            val disponibles =
                (capacidadSegura - ocupadosSeguros)
                    .coerceAtLeast(0L)

            binding.tvRutaViaje.text =
                "${viaje.origen} → ${viaje.destino}"

            binding.tvFechaHoraViaje.text =
                obtenerFechaHora(viaje)

            binding.tvVehiculoViaje.text =
                obtenerVehiculo(viaje)

            binding.tvEmbarqueViaje.text =
                if (viaje.puntoEmbarque.isBlank()) {
                    "Punto de embarque por confirmar"
                } else {
                    "Embarque: ${viaje.puntoEmbarque}"
                }

            binding.progressOcupacion.max =
                capacidadSegura.toInt()

            binding.progressOcupacion.progress =
                ocupadosSeguros.toInt()

            binding.progressOcupacion.progressTintList =
                ColorStateList.valueOf(colorPrincipal)

            binding.tvOcupacionViaje.text =
                "$ocupadosSeguros de $capacidadSegura asientos ocupados"

            binding.tvDisponiblesViaje.text =
                when (disponibles) {
                    0L -> "Completo"
                    1L -> "Queda 1"
                    else -> "Quedan $disponibles"
                }

            binding.tvPrecioViaje.text =
                String.format(
                    Locale("es", "PE"),
                    "S/ %.2f",
                    viaje.precio
                )

            binding.btnReservarViaje.backgroundTintList =
                ColorStateList.valueOf(colorPrincipal)

            val disponible = disponibles > 0L

            binding.btnReservarViaje.isEnabled = disponible

            binding.btnReservarViaje.text =
                if (disponible) {
                    "RESERVAR"
                } else {
                    "COMPLETO"
                }

            binding.tvEstadoViaje.text =
                if (disponible) {
                    "DISPONIBLE"
                } else {
                    "COMPLETO"
                }

            binding.btnReservarViaje.setOnClickListener {

                if (disponible) {
                    onReservar(viaje)
                }
            }

            binding.cardViaje.setOnClickListener {

                if (disponible) {
                    onReservar(viaje)
                }
            }
        }

        private fun obtenerFechaHora(
            viaje: Viaje
        ): String {

            val fecha = viaje.fechaSalida.trim()
            val hora = viaje.horaSalida.trim()

            return when {

                fecha.isNotBlank() && hora.isNotBlank() ->
                    "$fecha · Salida $hora"

                hora.isNotBlank() ->
                    "Salida aproximada $hora"

                fecha.isNotBlank() ->
                    fecha

                else ->
                    "Salida cuando se complete la unidad"
            }
        }

        private fun obtenerVehiculo(
            viaje: Viaje
        ): String {

            val vehiculo =
                viaje.vehiculoDescripcion.trim()

            val placa =
                viaje.placa.trim().uppercase(Locale.ROOT)

            return when {

                vehiculo.isNotBlank() &&
                        placa.isNotBlank() ->
                    "$vehiculo · $placa"

                vehiculo.isNotBlank() ->
                    vehiculo

                placa.isNotBlank() ->
                    "Vehículo · $placa"

                else ->
                    "Vehículo asignado"
            }
        }
    }
}