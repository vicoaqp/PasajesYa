package com.PasajesYa.pasajesya

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ItemConductorMovilidadBinding
import com.google.firebase.storage.FirebaseStorage

class ConductorMovilidadAdapter(
    private val alLlamar:
        (ConductorMovilidad) -> Unit
) : RecyclerView.Adapter<
        ConductorMovilidadAdapter.ConductorViewHolder
        >() {

    private val conductores =
        mutableListOf<ConductorMovilidad>()

    private val storage =
        FirebaseStorage.getInstance()


    fun actualizarConductores(
        nuevaLista: List<ConductorMovilidad>
    ) {

        conductores.clear()

        conductores.addAll(
            nuevaLista
        )

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConductorViewHolder {

        val binding =
            ItemConductorMovilidadBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return ConductorViewHolder(
            binding
        )
    }


    override fun onBindViewHolder(
        holder: ConductorViewHolder,
        position: Int
    ) {

        holder.bind(
            conductores[position]
        )
    }


    override fun getItemCount():
            Int {

        return conductores.size
    }


    inner class ConductorViewHolder(
        private val binding:
        ItemConductorMovilidadBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            conductor:
            ConductorMovilidad
        ) {

            binding.tvNombreConductor.text =
                conductor.obtenerNombre()


            binding.tvTipoServicio.text =
                if (
                    conductor.tipoServicio
                        .lowercase() ==
                    TIPO_MOTOTAXI
                ) {

                    "🛺 MOTOTAXI"

                } else {

                    "🚕 TAXI"
                }


            val vehiculo =
                listOf(
                    conductor.marca,
                    conductor.modelo
                )
                    .filter {
                        it.isNotBlank()
                    }
                    .joinToString(
                        " "
                    )


            binding.tvVehiculo.text =
                vehiculo.ifBlank {

                    "Unidad registrada"
                }


            binding.tvPlaca.text =
                "Placa: " +
                        conductor.placa
                            .ifBlank {
                                "No registrada"
                            }


            binding.tvColor.text =
                "Color: " +
                        conductor.color
                            .ifBlank {
                                "No registrado"
                            }


            binding.btnLlamar.isEnabled =
                conductor.celular
                    .isNotBlank()


            binding.btnLlamar
                .setOnClickListener {

                    alLlamar(
                        conductor
                    )
                }


            // -----------------------------------------
            // FOTO DEL CONDUCTOR
            // -----------------------------------------

            binding.imgConductor.setImageResource(
                R.drawable.ic_persona_pasajes
            )

            binding.imgConductor.tag =
                conductor.fotoPerfilUrl

            cargarImagenStorage(
                url =
                    conductor.fotoPerfilUrl,

                destino =
                    binding.imgConductor
            )


            // -----------------------------------------
            // FOTO DE LA UNIDAD
            // -----------------------------------------

            binding.imgUnidad.setImageResource(
                R.drawable.ic_empresa_transporte
            )

            binding.imgUnidad.tag =
                conductor.fotoUnidadUrl

            cargarImagenStorage(
                url =
                    conductor.fotoUnidadUrl,

                destino =
                    binding.imgUnidad
            )
        }


        private fun cargarImagenStorage(
            url: String,
            destino:
            android.widget.ImageView
        ) {

            if (url.isBlank()) {

                return
            }


            try {

                val referencia =
                    storage.getReferenceFromUrl(
                        url
                    )


                referencia
                    .getBytes(
                        MAX_IMAGEN_BYTES
                    )
                    .addOnSuccessListener {
                            bytes ->

                        if (
                            destino.tag !=
                            url
                        ) {

                            return@addOnSuccessListener
                        }


                        val bitmap =
                            BitmapFactory
                                .decodeByteArray(
                                    bytes,
                                    0,
                                    bytes.size
                                )


                        if (bitmap != null) {

                            destino.setImageBitmap(
                                bitmap
                            )
                        }
                    }
                    .addOnFailureListener {

                        // Se mantiene la imagen
                        // predeterminada.
                    }


            } catch (_: Exception) {

                // URL inválida:
                // se mantiene el placeholder.
            }
        }
    }


    companion object {

        private const val
                TIPO_MOTOTAXI =
            "mototaxi"

        private const val
                MAX_IMAGEN_BYTES =
            5L * 1024L * 1024L
    }
}