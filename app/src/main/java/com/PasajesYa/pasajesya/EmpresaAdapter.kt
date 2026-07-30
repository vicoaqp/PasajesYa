package com.PasajesYa.pasajesya

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ItemEmpresaBinding
import java.util.Locale

class EmpresaAdapter(
    private val onEmpresaSeleccionada: (Empresa) -> Unit
) : RecyclerView.Adapter<EmpresaAdapter.EmpresaViewHolder>() {

    private val listaCompleta = mutableListOf<Empresa>()
    private val listaFiltrada = mutableListOf<Empresa>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EmpresaViewHolder {

        val binding = ItemEmpresaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return EmpresaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: EmpresaViewHolder,
        position: Int
    ) {
        holder.mostrarEmpresa(listaFiltrada[position])
    }

    override fun getItemCount(): Int {
        return listaFiltrada.size
    }

    fun actualizarEmpresas(empresas: List<Empresa>) {

        listaCompleta.clear()
        listaCompleta.addAll(empresas)

        listaFiltrada.clear()
        listaFiltrada.addAll(empresas)

        notifyDataSetChanged()
    }

    fun filtrar(textoBusqueda: String) {

        val texto = textoBusqueda
            .trim()
            .lowercase(Locale.ROOT)

        listaFiltrada.clear()

        if (texto.isBlank()) {

            listaFiltrada.addAll(listaCompleta)

        } else {

            val resultados = listaCompleta.filter { empresa ->

                empresa.nombre
                    .lowercase(Locale.ROOT)
                    .contains(texto) ||

                        empresa.descripcion
                            .lowercase(Locale.ROOT)
                            .contains(texto) ||

                        empresa.rutasResumen
                            .lowercase(Locale.ROOT)
                            .contains(texto)
            }

            listaFiltrada.addAll(resultados)
        }

        notifyDataSetChanged()
    }

    inner class EmpresaViewHolder(
        private val binding: ItemEmpresaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun mostrarEmpresa(empresa: Empresa) {

            binding.tvNombreEmpresa.text =
                empresa.nombre.ifBlank {
                    "Empresa de transporte"
                }

            binding.tvDescripcionEmpresa.text =
                empresa.descripcion.ifBlank {
                    "Servicio de transporte de pasajeros"
                }

            binding.tvRutasEmpresa.text =
                empresa.rutasResumen.ifBlank {
                    "Consulta las rutas disponibles"
                }

            /*
             * Por ahora mostramos el ícono predeterminado.
             * Después cargaremos logoUrl con Glide.
             */
            binding.imgEmpresa.setImageResource(
                R.drawable.ic_empresa_transporte
            )

            binding.cardEmpresa.setOnClickListener {
                onEmpresaSeleccionada(empresa)
            }

            binding.btnElegirEmpresa.setOnClickListener {
                onEmpresaSeleccionada(empresa)
            }
        }
    }
}