package com.fake.cashflow.ui

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fake.cashflow.R
import com.fake.cashflow.data.Expense
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.io.InputStream
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException


class ExpenseRecyclerAdapter(
    private var expenses: List<Expense>,
    private val categoryNameResolver: (Long?) -> String,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseRecyclerAdapter.ExpenseViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    //private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteExpense)
        val expenseImageView: ImageView = itemView.findViewById(R.id.expenseImageView)
        val downloadButton: Button = itemView.findViewById(R.id.downloadButton)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun getItemCount(): Int = expenses.size

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.tvDescription.text = expense.description
        holder.tvAmount.text = currencyFormatter.format(expense.amount)
        holder.tvDate.text = dateFormatter.format(Date(expense.date))

        // Get category name using the provided resolver
        holder.tvCategory.text = categoryNameResolver(expense.categoryId)

        // Set up delete button
        holder.btnDelete.setOnClickListener {
            onDeleteClick(expense)
        }

        // Load image if available
        if (!expense.imagePath.isNullOrEmpty()) {
            try {
                Log.d("ExpenseRecyclerAdapter", "Processing image for expense: ${expense.id}")
                
                // Force placeholder first to ensure we see something
                holder.expenseImageView.setImageResource(R.drawable.placeholder_image)
                
                // Check if it's a base64 encoded image (base64 strings are typically longer)
                if (expense.imagePath.length > 100 && !expense.imagePath.startsWith("http")) {
                    try {
                        // Decode base64 string to bitmap
                        val imageBytes = android.util.Base64.decode(expense.imagePath, android.util.Base64.DEFAULT)
                        val decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        
                        if (decodedBitmap != null) {
                            Log.d("ExpenseRecyclerAdapter", "Successfully decoded base64 image")
                            holder.expenseImageView.setImageBitmap(decodedBitmap)
                            holder.downloadButton.visibility = View.VISIBLE
                        } else {
                            Log.e("ExpenseRecyclerAdapter", "Failed to decode base64 image")
                            holder.expenseImageView.setImageResource(R.drawable.placeholder_image)
                        }
                    } catch (e: Exception) {
                        Log.e("ExpenseRecyclerAdapter", "Error decoding base64 image: ${e.message}", e)
                        holder.expenseImageView.setImageResource(R.drawable.placeholder_image)
                    }
                } else {
                    // Use Glide for http URLs or other formats
                    Glide.with(holder.itemView.context)
                        .load(expense.imagePath)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(holder.expenseImageView)
                    
                    holder.downloadButton.visibility = View.VISIBLE
                }
                
            } catch (e: Exception) {
                // Log error and show placeholder
                Log.e("ExpenseRecyclerAdapter", "Failed to load image: ${e.message}", e)
                holder.expenseImageView.setImageResource(R.drawable.placeholder_image)
                holder.downloadButton.visibility = View.GONE
            }
        } else {
            // No image path available
            Log.d("ExpenseRecyclerAdapter", "No image path available for expense: ${expense.id}")
            holder.expenseImageView.setImageResource(R.drawable.placeholder_image) 
            holder.downloadButton.visibility = View.GONE
        }

// Download button click
        holder.downloadButton.setOnClickListener {
            if (!expense.imagePath.isNullOrEmpty()) {
                // Check if it's a base64 image
                if (expense.imagePath.length > 100 && !expense.imagePath.startsWith("http")) {
                    // Save base64 image directly
                    saveBase64ImageToDownloads(holder.itemView.context, expense.imagePath)
                } else {
                    // Handle URL or URI
                    saveImageToDownloads(holder.itemView.context, Uri.parse(expense.imagePath))
                }
            } else {
                Toast.makeText(holder.itemView.context, "No image available to download", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }

    private fun saveImageToDownloads(context: Context, imageUri: Uri) {
        try {
            // Create a file in Downloads folder
            val fileName = "expense_image_${System.currentTimeMillis()}.jpg"
            val downloadsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsFolder, fileName)
            
            Log.d("ExpenseRecyclerAdapter", "Saving image to: ${file.absolutePath}")
            Log.d("ExpenseRecyclerAdapter", "From URI: $imageUri")
            
            // Check if it's a Firebase URL (starts with http)
            if (imageUri.toString().startsWith("http")) {
                Toast.makeText(context, "Downloading image...", Toast.LENGTH_SHORT).show()
                downloadFirebaseImageToFile(context, imageUri.toString(), file)
            } else {
                try {
                    // Show toast that download is starting
                    Toast.makeText(context, "Saving image...", Toast.LENGTH_SHORT).show()
                    
                    // Try to use Glide to get the image (more reliable)
                    Glide.with(context)
                        .asBitmap()
                        .load(imageUri)
                        .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                            override fun onResourceReady(
                                resource: android.graphics.Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
                            ) {
                                try {
                                    // Save bitmap to file
                                    val outputStream = FileOutputStream(file)
                                    resource.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                                    outputStream.flush()
                                    outputStream.close()
                                    
                                    // Notify the user that the image was saved
                                    Toast.makeText(context, "Image saved to Downloads", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("ExpenseRecyclerAdapter", "Failed to save bitmap: ${e.message}", e)
                                    Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                                // Do nothing
                            }
                            
                            override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                Log.e("ExpenseRecyclerAdapter", "Failed to load image with Glide")
                                // Fall back to direct method
                                saveImageDirectly(context, imageUri, file)
                            }
                        })
                } catch (e: Exception) {
                    Log.e("ExpenseRecyclerAdapter", "Error with Glide: ${e.message}", e)
                    // Fall back to direct method
                    saveImageDirectly(context, imageUri, file)
                }
            }
        } catch (e: Exception) {
            Log.e("ExpenseRecyclerAdapter", "Failed to save image: ${e.message}", e)
            Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveImageDirectly(context: Context, imageUri: Uri, file: File) {
        try {
            // Get InputStream from content URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            
            // Check if the input stream is not null
            if (inputStream != null) {
                // Use FileOutputStream to save the file
                val outputStream = FileOutputStream(file)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                
                // Notify the user that the image was saved
                Toast.makeText(context, "Image saved to Downloads", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to retrieve image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ExpenseRecyclerAdapter", "Failed to save image directly: ${e.message}", e)
            Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Saves a base64 encoded image string to the Downloads folder
     */
    private fun saveBase64ImageToDownloads(context: Context, base64Image: String) {
        try {
            Toast.makeText(context, "Saving image...", Toast.LENGTH_SHORT).show()
            
            // Create a file in Downloads folder
            val fileName = "expense_image_${System.currentTimeMillis()}.jpg"
            val downloadsFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsFolder, fileName)
            
            Log.d("ExpenseRecyclerAdapter", "Saving base64 image to: ${file.absolutePath}")
            
            // Decode base64 to bitmap
            val imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            if (bitmap != null) {
                // Save bitmap to file
                val outputStream = FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                
                // Notify user
                Toast.makeText(context, "Image saved to Downloads", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("ExpenseRecyclerAdapter", "Failed to decode base64 to bitmap")
                Toast.makeText(context, "Failed to save image: Invalid image data", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ExpenseRecyclerAdapter", "Failed to save base64 image: ${e.message}", e)
            Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun downloadFirebaseImageToFile(context: Context, url: String, destinationFile: File) {
        try {
            // Create temporary local file to download to
            val localFile = File.createTempFile("image", "jpg")
            
            // Show toast that download is starting
            Toast.makeText(context, "Downloading image...", Toast.LENGTH_SHORT).show()
            
            // Reference to firebase storage from URL
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            
            // Download file
            storageRef.getFile(localFile)
                .addOnSuccessListener {
                    try {
                        // Copy from temp file to destination file
                        localFile.inputStream().use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        // Delete temp file
                        localFile.delete()
                        // Notify user
                        Toast.makeText(context, "Image saved to Downloads", Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error saving downloaded file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failed download
                    Toast.makeText(context, "Download failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error setting up download: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


}