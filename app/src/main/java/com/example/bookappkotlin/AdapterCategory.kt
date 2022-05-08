package com.example.bookappkotlin

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.bookappkotlin.databinding.ActivityCategoryAddBinding
import com.example.bookappkotlin.databinding.RowCategoryBinding
import com.google.firebase.database.FirebaseDatabase

class AdapterCategory :RecyclerView.Adapter<AdapterCategory.HolderCategory>, Filterable{

    private val context: Context
    public var categoryArrayList: ArrayList<ModelCategory>
    private var filterList: ArrayList<ModelCategory>

    private var filter: FilterCategory? = null

    private lateinit var binding: RowCategoryBinding
    //constructor
    constructor(context: Context, categoryArrayList: ArrayList<ModelCategory>) {
        this.context = context
        this.categoryArrayList = categoryArrayList
        this.filterList = categoryArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        //inflate bind row_category.xml
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderCategory(binding.root)
    }

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
//        get data, set data, handle click etc

        //get data
        val model = categoryArrayList[position]
        val id = model.id
        val category = model.category
        val uid = model.uid
        val timestamp = model.timestamp

        //set data
        holder.categoryTv.text = category

        //handle click, delete category
        holder.deleteBtn.setOnClickListener {
            //confirm before delete
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Obriši")
                .setMessage("Jeste li sigurni da zelite obrisati ovu kategoriju?")
                .setPositiveButton("Potvrdi"){a, d->
                    Toast.makeText(context, "Brisanje...", Toast.LENGTH_SHORT).show()
                    DeleteCategory(model, holder)
                }
                .setNegativeButton("Odustani"){a, d->
                    a.dismiss()
                }
                .show()
        }
    }

    private fun DeleteCategory(model: ModelCategory, holder: HolderCategory) {
        //get id of category to delete
        val id = model.id
        //Firebase DB > Categories > categoryId
        var ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child(id)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Brisanje uspješno", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                Toast.makeText(context, "Unable to delete due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int {
        return categoryArrayList.size //number of items in list
    }




    //ViewHolder class to hold/init UI views for row_category.xml
    inner class HolderCategory(itemView: View): RecyclerView.ViewHolder(itemView){
        //init ui views
        var categoryTv:TextView = binding.categoryTv
        var deleteBtn:ImageView = binding.deleteBtn
    }

    override fun getFilter(): Filter {
        if (filter == null){
            filter = FilterCategory(filterList, this)
        }
        return filter as FilterCategory
    }


}