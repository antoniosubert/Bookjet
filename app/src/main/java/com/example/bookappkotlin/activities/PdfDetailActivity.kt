package com.example.bookappkotlin.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.bookappkotlin.Constants
import com.example.bookappkotlin.MyApplication
import com.example.bookappkotlin.R
import com.example.bookappkotlin.adapters.AdapterComment
import com.example.bookappkotlin.databinding.ActivityPdfDetailBinding
import com.example.bookappkotlin.databinding.DialogCommentAddBinding
import com.example.bookappkotlin.models.ModelComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.FileOutputStream
import java.lang.Exception

class PdfDetailActivity : AppCompatActivity() {
    //view binding
    private lateinit var binding: ActivityPdfDetailBinding

    private companion object{
        //TAG
        const val TAG = "BOOK_DETAILS_TAG"
    }

    //book id, get from intent
    private var  bookId = ""
    //get from firebase
    private var bookTitle = ""
    private var bookUrl = ""

    //will hold a boolean value false/true to indicate is in current user's favorite list or not
    private var isInMyFavorite = false

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: ProgressDialog

    //arraylist to hold comments
    private lateinit var commentArrayList: ArrayList<ModelComment>

    //adapter to be set to recyclerview
    private lateinit var adapterComment: AdapterComment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get book id from intent
        bookId = intent.getStringExtra("bookId")!!

        //init progress bar
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Molimo pričekajte")
        progressDialog.setCanceledOnTouchOutside(false)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser != null){
            //user is logged in, check if book is in favorites of not
            checkIsFavorite()
        }

        //increment book view count, whenever this page starts
        MyApplication.incrementBookViewCount(bookId)

        loadBookDetalis()
        showComments()

        //handle back button click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        //handle click, open pdf view activity
        binding.readBookBtn.setOnClickListener{
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("bookId", bookId)
            startActivity(intent)
        }

        //handle click, download book/pdf
        binding.downloadBookBtn.setOnClickListener{
            //first check storage permission, if granted book download book, if not granted request permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "onCreate: STORAGE PREMISSION is already granted")
                downloadBook()
            }
            else{
                Log.d(TAG, "onCreate: STORAGE PREMISSION was not granted")
                requestStoragePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        //handle click, add/remove from favorites
        binding.favoriteBtn.setOnClickListener{
            //we can add only if the user is logged in
            //1. check if user is logged in of not
            if (firebaseAuth.currentUser == null){
                //user not logged in, can't do favorite functionality
                Toast.makeText(this, "Niste prijavljeni", Toast.LENGTH_SHORT).show()
            }
            else{
                //user is logged in, we can do favorite functionality
                if (isInMyFavorite){
                    //already in favorites, remove
                    MyApplication.removeFromFavorite(this, bookId)
                }
                else{
                    //not in favorites, add
                    addToFavorite()
                }
            }

        }

        //handle click, show add comment dialog
        binding.addCommentBtn.setOnClickListener {
            /*to add a comment, user must be logged in, if not just show a message Niste prijavljeni*/
            if (firebaseAuth.currentUser == null){
                //user not logged in, don't allow adding comment
                Toast.makeText(this, "Niste prijavljeni", Toast.LENGTH_SHORT).show()
            }
            else{
                //user logged in, allow adding comment
                addCommentDialog()
            }
        }

    }

    private fun showComments() {
        //init arraylist
        commentArrayList = ArrayList()

        //database path to load comments
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list
                    commentArrayList.clear()
                    for (ds in snapshot.children){
                        //get data s model, be careful of spellings and data type
                        val model = ds.getValue(ModelComment::class.java)
                        //add to list
                        commentArrayList.add(model!!)
                    }
                    //setup adapter
                    adapterComment = AdapterComment(this@PdfDetailActivity, commentArrayList)
                    //set adapter to recyclerview
                    binding.commentRv.adapter = adapterComment

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private var comment = ""

    private fun addCommentDialog() {
        //inflate/bind view for dialog dialog_comment_add.xml
        val commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this))

        //setup alert
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
        builder.setView(commentAddBinding.root)

        //create and show alert dialog
        val alertDialog = builder.create()
        alertDialog.show()

        //handle click, dismiss dialog
        commentAddBinding.backBtn.setOnClickListener { alertDialog.dismiss() }

        //handle click, add dialog
        commentAddBinding.submitBtn.setOnClickListener{
            //get data
            comment = commentAddBinding.commentEt.text.toString().trim()
            //validate data
            if (comment.isEmpty()){
                Toast.makeText(this,"Dodajte komentar", Toast.LENGTH_SHORT).show()
            }
            else{
                alertDialog.dismiss()
                addComment()
            }
        }
    }

    private fun addComment() {
        //show progress
        progressDialog.setMessage("Dodavanje komentara")
        progressDialog.show()

        //timestamp for comment id, comment timestamp etc.
        val timestamp = "${System.currentTimeMillis()}"

        //setup data to add in database for comment
        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["bookId"] = "$bookId"
        hashMap["timestamp"] = "$timestamp"
        hashMap["comment"] = "$comment"
        hashMap["id"] = "${firebaseAuth.uid}"

        //database path to add data into it
        //Books >> bookId >> Comments >> commentId >> commentData
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId).child("Comments").child(timestamp)
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this,"Komentar dodan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                progressDialog.dismiss()
                Toast.makeText(this,"Failed to add comment due to ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private val requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean ->
        //lets check if granted or not
        if (isGranted){
            Log.d(TAG, "onCreate: STORAGE PREMISSION is granted")
            downloadBook()
        }
        else{
            Log.d(TAG, "onCreate: STORAGE PREMISSION is denied")
            Toast.makeText(this, "Dozvola odbijena", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadBook(){
        Log.d(TAG, "downloadBook: Downloading book")
        progressDialog.setMessage("Preuzimanje knjige")
        progressDialog.show()

        //lets download book from firebase storage using url
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener {bytes->
                Log.d(TAG, "downloadBook: Book downloaded...")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener {e->
                Log.d(TAG, "downloadBook: Failed to download book due to ${e.message}")
                Toast.makeText(this, "Failed to download book due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray?) {
        Log.d(TAG, "saveToDownloadsFolder: saving downloaded book")

        val nameWithExtention = "${System.currentTimeMillis()}.pdf"

        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs() //create folder if not exists

            val filePath = downloadsFolder.path + "/" + nameWithExtention

            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()

            Toast.makeText(this, "Spremljeno u mapu za preuzimanja", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "saveToDownloadsFolder: Saved to Downloads Folder")

            progressDialog.dismiss()
            incrementDownloadCount()

        }
        catch (e: Exception){
            progressDialog.dismiss()
            Log.d(TAG, "saveToDownloadsFolder: failed to save due to ${e.message}")
            Toast.makeText(this, "failed to save due to ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun incrementDownloadCount() {
        //increment downloads count to firebase database
        Log.d(TAG, "incrementDownloadCount: ")

        //get previous downloads count
        var ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get downloads count
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDataChange: Current Downloads Count: $downloadsCount")

                    if (downloadsCount == "" || downloadsCount == "null"){
                        downloadsCount = "0"
                    }

                    //convert to long increment 1
                    val newDownloadCount: Long = downloadsCount.toLong() + 1
                    Log.d(TAG, "onDataChange: Current Downloads Count: $newDownloadCount")

                    //setup data to update to database
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap[""] = newDownloadCount

                    //update new incremented downloads to database
                    val dbRef = FirebaseDatabase.getInstance().getReference("Books")
                    dbRef.child(bookId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: downloads count incremented ")
                        }
                        .addOnFailureListener {e->
                            Log.d(TAG, "onDataChange: Failed to increment due to ${e.message}")
                        }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }

    private fun loadBookDetalis() {
        //Books > bookId > Details
        val ref = FirebaseDatabase.getInstance().getReference("Books")
        ref.child(bookId)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    bookTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    bookUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    //format date
                    val date = MyApplication.formatTimeStamp(timestamp.toLong())

                    //load pdf category
                    MyApplication.loadCategory(categoryId, binding.categoryTv)

                    //load pdf thumbnail, pages count
                    MyApplication.loadPdfFromUrlSinglePage(
                        "$bookUrl",
                        "$bookTitle",
                        binding.pdfView,
                        binding.progressBar,
                        binding.pagesTv
                    )
                    //load pdf size
                    MyApplication.loadPdfSize("$bookUrl", "$bookTitle", binding.sizeTv)

                    //set data
                    binding.titleTv.text = bookTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun checkIsFavorite(){
        Log.d(TAG, "checkIsFavorite: Checking if book is in favorites or not")

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInMyFavorite = snapshot.exists()
                    if (isInMyFavorite){
                        //available in favorite
                        Log.d(TAG, "onDataChange: Available in favorites")
                            //set drawable bottom icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0,
                            R.drawable.ic_favorite_white
                        )
                        binding.favoriteBtn.text = "Ukloni iz favorita"
                    }
                    else{
                        //not available in favorite
                        Log.d(TAG, "onDataChange: Not available in favorites")
                        //set drawable bottom icon
                        binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0,
                            R.drawable.ic_favorite_border_white
                        )
                        binding.favoriteBtn.text = "Dodaj u favorite"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun addToFavorite(){
        Log.d(TAG, "addToFavorite: Adding to favorites")
        val timestamp = System.currentTimeMillis()

        //setup data to add in database
        val hashMap = HashMap<String, Any>()
        hashMap["bookId"] = bookId
        hashMap["timestamp"] = timestamp

        //save to database
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(bookId)
            .setValue(hashMap)
            .addOnSuccessListener {
                //added to favorites
                Log.d(TAG, "addToFavorite: Added to favorites")
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                //failed to add to favorites
                Log.d(TAG, "addToFavorite: Failed to add to favorites due to ${e.message}")
                Toast.makeText(this, "Failed to add to favorites due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}