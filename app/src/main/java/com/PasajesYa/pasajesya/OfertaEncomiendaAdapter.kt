package com.PasajesYa.pasajesya

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ItemOfertaEncomiendaBinding
import java.util.Locale

class OfertaEncomiendaAdapter(
    private val alAceptar:
        (OfertaEncomienda) -> Unit
) : RecyclerView.Adapter<
        OfertaEncomiendaAdapter.OfertaViewHolder
        >() {

    private val ofertas =
        mutableListOf<OfertaEncomienda>()


    private var solicitudAceptaOfertas =
        true


    fun actualizar(
        nuevaLista: List<OfertaEncomienda>,
        aceptaOfertas: Boolean
    ) {

        ofertas.clear()

        ofertas.addAll(
            nuevaLista
        )

        solicitudAceptaOfertas =
            aceptaOfertas

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OfertaViewHolder {

        val binding =
            ItemOfertaEncomiendaBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )


        return OfertaViewHolder(
            binding
        )
    }


    override fun onBindViewHolder(
        holder: OfertaViewHolder,
        position: Int
    ) {

        holder.bind(
            ofertas[position]
        )
    }


    override fun getItemCount(): Int {

        return ofertas.size
    }


    inner class OfertaViewHolder(
        private val binding:
        ItemOfertaEncomiendaBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            oferta: OfertaEncomienda
        ) {

            binding.tvNombreConductor.text =
                oferta
                    .conductorNombre
                    .ifBlank {

                        "Conductor"
                    }


            binding.tvIconoUnidad.text =
                if (
                    oferta.tipoServicio
                        .trim()
                        .lowercase() ==
                    "mototaxi"
                ) {

                    "🛺"

                } else {

                    "🚕"
                }


            val tipoUnidad =
                if (
                    oferta.tipoServicio
                        .trim()
                        .lowercase() ==
                    "mototaxi"
                ) {

                    "Mototaxi"

                } else {

                    "Taxi / vehículo"
                }


            val descripcionUnidad =
                listOf(
                    tipoUnidad,
                    oferta.marca,
                    oferta.modelo
                )
                    .filter {
                        it.isNotBlank()
                    }
                    .joinToString(
                        " "
                    )


            binding.tvUnidad.text =
                descripcionUnidad


            binding.tvPlaca.text =
                if (
                    oferta.placa.isBlank()
                ) {

                    "Placa no registrada"

                } else {

                    "Placa: ${oferta.placa}"
                }


            binding.tvPrecio.text =
                String.format(
                    Locale.US,
                    "S/ %.2f",
                    oferta.precioServicio
                )


            configurarEstado(
                oferta
            )


            binding.btnAceptarOferta
                .setOnClickListener {

                    alAceptar(
                        oferta
                    )
                }
        }


        private fun configurarEstado(
            oferta: OfertaEncomienda
        ) {

            val estado =
                oferta
                    .estadoOferta
                    .trim()
                    .lowercase()


            when (
                estado
            ) {

                "aceptada" -> {

                    binding.tvEstadoOferta.text =
                        "ACEPTADA"

                    binding.tvEstadoOferta.setTextColor(
                        binding.root.context.getColor(
                            R.color.pasajes_success
                        )
                    )


                    binding.btnAceptarOferta.visibility =
                        View.GONE
                }


                "rechazada" -> {

                    binding.tvEstadoOferta.text =
                        "NO SELECCIONADA"

                    binding.tvEstadoOferta.setTextColor(
                        android.graphics.Color.GRAY
                    )


                    binding.btnAceptarOferta.visibility =
                        View.GONE
                }


                else -> {

                    binding.tvEstadoOferta.text =
                        "PENDIENTE"

                    binding.tvEstadoOferta.setTextColor(
                        binding.root.context.getColor(
                            R.color.pasajes_primary
                        )
                    )


                    binding.btnAceptarOferta.visibility =
                        if (
                            solicitudAceptaOfertas
                        ) {

                            View.VISIBLE

                        } else {

                            View.GONE
                        }
                }
            }
        }
    }
}