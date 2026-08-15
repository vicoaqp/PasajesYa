package com.PasajesYa.pasajesya

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ItemProductoCompraBinding

class ProductoCompraAdapter(
    private val alEliminar: (Int) -> Unit
) : RecyclerView.Adapter<
        ProductoCompraAdapter.ProductoViewHolder
        >() {

    private val productos =
        mutableListOf<ProductoCompra>()


    fun actualizar(
        nuevaLista: List<ProductoCompra>
    ) {

        productos.clear()

        productos.addAll(
            nuevaLista
        )

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val binding =
            ItemProductoCompraBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return ProductoViewHolder(
            binding
        )
    }


    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {

        holder.bind(
            productos[position]
        )
    }


    override fun getItemCount(): Int {

        return productos.size
    }


    inner class ProductoViewHolder(
        private val binding:
        ItemProductoCompraBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            producto: ProductoCompra
        ) {

            binding.tvNombreProducto.text =
                producto.nombre


            binding.tvCantidadProducto.text =
                "Cantidad: ${producto.cantidad}"


            if (
                producto.observacion.isBlank()
            ) {

                binding.tvObservacionProducto.visibility =
                    View.GONE

            } else {

                binding.tvObservacionProducto.visibility =
                    View.VISIBLE

                binding.tvObservacionProducto.text =
                    producto.observacion
            }


            binding.btnEliminarProducto
                .setOnClickListener {

                    val posicion =
                        bindingAdapterPosition

                    if (
                        posicion !=
                        RecyclerView.NO_POSITION
                    ) {

                        alEliminar(
                            posicion
                        )
                    }
                }
        }
    }
}