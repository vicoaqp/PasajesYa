package com.PasajesYa.pasajesya

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ItemEncomiendaBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EncomiendaAdapter(
    private val alVerDetalle:
        (EncomiendaLocal) -> Unit
) : RecyclerView.Adapter<
        EncomiendaAdapter.EncomiendaViewHolder
        >() {

    private val encomiendas =
        mutableListOf<EncomiendaLocal>()


    fun actualizar(
        nuevaLista: List<EncomiendaLocal>
    ) {

        encomiendas.clear()

        encomiendas.addAll(
            nuevaLista
        )

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EncomiendaViewHolder {

        val binding =
            ItemEncomiendaBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return EncomiendaViewHolder(
            binding
        )
    }


    override fun onBindViewHolder(
        holder: EncomiendaViewHolder,
        position: Int
    ) {

        holder.bind(
            encomiendas[position]
        )
    }


    override fun getItemCount(): Int {

        return encomiendas.size
    }


    inner class EncomiendaViewHolder(
        private val binding:
        ItemEncomiendaBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            encomienda: EncomiendaLocal
        ) {

            binding.tvDescripcion.text =
                encomienda.descripcion
                    .ifBlank {
                        "Encomienda local"
                    }


            binding.tvRecojo.text =
                encomienda.direccionRecojo
                    .ifBlank {
                        "Sin dirección"
                    }


            binding.tvEntrega.text =
                encomienda.direccionEntrega
                    .ifBlank {
                        "Sin dirección"
                    }


            binding.tvCantidadOfertas.text =
                when (
                    encomienda.cantidadOfertas
                ) {

                    0 ->
                        "Sin ofertas"

                    1 ->
                        "1 recibida"

                    else ->
                        "${encomienda.cantidadOfertas} recibidas"
                }


            binding.tvTipoUnidad.text =
                when (
                    encomienda.tipoUnidad
                        .trim()
                        .lowercase()
                ) {

                    "mototaxi" ->
                        "🛺 Mototaxi"

                    "taxi" ->
                        "🚕 Taxi / vehículo"

                    else ->
                        "🚘 Cualquier unidad"
                }


            mostrarFecha(
                encomienda
            )


            configurarEstado(
                encomienda.estado
            )


            mostrarPrecioYConductor(
                encomienda
            )


            binding.btnVerDetalle
                .setOnClickListener {

                    alVerDetalle(
                        encomienda
                    )
                }
        }


        private fun mostrarFecha(
            encomienda: EncomiendaLocal
        ) {

            val timestamp =
                encomienda.fechaPublicacion


            if (
                timestamp == null
            ) {

                binding.tvFecha.text =
                    "Fecha no disponible"

                return
            }


            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy - h:mm a",
                    Locale("es", "PE")
                )


            binding.tvFecha.text =
                formato.format(
                    timestamp.toDate()
                )
        }


        private fun configurarEstado(
            estadoOriginal: String
        ) {

            val estado =
                estadoOriginal
                    .trim()
                    .lowercase()


            val contexto =
                binding.root.context


            when (
                estado
            ) {

                "asignado" -> {

                    binding.tvEstado.text =
                        "🟡 CONDUCTOR SELECCIONADO"

                    binding.tvEstado.backgroundTintList =
                        ColorStateList.valueOf(
                            Color.parseColor(
                                "#E5A000"
                            )
                        )
                }


                "recogido" -> {

                    binding.tvEstado.text =
                        "📦 ENCOMIENDA RECOGIDA"

                    binding.tvEstado.backgroundTintList =
                        ColorStateList.valueOf(
                            Color.parseColor(
                                "#7B61FF"
                            )
                        )
                }


                "en_camino" -> {

                    binding.tvEstado.text =
                        "🚗 EN CAMINO"

                    binding.tvEstado.backgroundTintList =
                        ColorStateList.valueOf(
                            Color.parseColor(
                                "#1976D2"
                            )
                        )
                }


                "entregado" -> {

                    binding.tvEstado.text =
                        "🟢 ENTREGADA"

                    binding.tvEstado.backgroundTintList =
                        ColorStateList.valueOf(
                            contexto.getColor(
                                R.color.pasajes_success
                            )
                        )
                }


                "cancelado" -> {

                    binding.tvEstado.text =
                        "🔴 CANCELADA"

                    binding.tvEstado.backgroundTintList =
                        ColorStateList.valueOf(
                            Color.parseColor(
                                "#D64545"
                            )
                        )
                }


                else -> {

                    binding.tvEstado.text =
                        if (
                            estado ==
                            "publicado"
                        ) {

                            if (
                                bindingAdapterPosition !=
                                RecyclerView.NO_POSITION &&
                                encomiendas[
                                    bindingAdapterPosition
                                ].cantidadOfertas > 0
                            ) {

                                "🔵 OFERTAS RECIBIDAS"

                            } else {

                                "🔵 BUSCANDO OFERTAS"
                            }

                        } else {

                            "🔵 BUSCANDO OFERTAS"
                        }


                    binding.tvEstado.backgroundTintList =
                        ColorStateList.valueOf(
                            contexto.getColor(
                                R.color.pasajes_primary
                            )
                        )
                }
            }
        }


        private fun mostrarPrecioYConductor(
            encomienda: EncomiendaLocal
        ) {

            val tienePrecio =
                encomienda.precioAceptado > 0.0


            binding.contenedorPrecioAceptado.visibility =
                if (
                    tienePrecio
                ) {

                    View.VISIBLE

                } else {

                    View.GONE
                }


            if (
                tienePrecio
            ) {

                binding.tvPrecioAceptado.text =
                    String.format(
                        Locale.US,
                        "S/ %.2f",
                        encomienda.precioAceptado
                    )
            }


            val tieneConductor =
                encomienda
                    .conductorSeleccionadoNombre
                    .isNotBlank()


            binding.tvConductor.visibility =
                if (
                    tieneConductor
                ) {

                    View.VISIBLE

                } else {

                    View.GONE
                }


            if (
                tieneConductor
            ) {

                binding.tvConductor.text =
                    "Conductor: " +
                            encomienda
                                .conductorSeleccionadoNombre
            }
        }
    }
}