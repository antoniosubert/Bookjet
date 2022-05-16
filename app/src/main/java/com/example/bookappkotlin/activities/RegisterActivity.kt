package com.example.bookappkotlin.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.bookappkotlin.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

//    view binding

    private lateinit var binding:ActivityRegisterBinding

//    firebase auth

    private lateinit var firebaseAuth: FirebaseAuth

//    progress dialog

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

//        init progress dialog, will show while creating account | register user

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Molimo pričekajte")
        progressDialog.setCanceledOnTouchOutside(false)

//        handle back button click

        binding.backBtn.setOnClickListener{
            onBackPressed() //goto previous activity
        }

//        handle click, begin register

        binding.registerBtn.setOnClickListener{
            /*Steps
            * 1) Input data
            * 2) validate data
            * 3) Create account - Firebase auth
            * 4) Save user info - Firebase Realtime Database*/

            validateData()
        }

    }

    private var name = ""
    private var email = ""
    private var password = ""

    private fun validateData() {
//        * 1) Input data
        name = binding.nameEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        val cPassword = binding.cPasswordEt.text.toString().trim()

//        * 2) validate data
        if (name.isEmpty()){
            //empty name...
            Toast.makeText(this, "Unesite svoje ime...", Toast.LENGTH_SHORT).show()
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //invalid email pattern
            Toast.makeText(this, "Neispravan email unos...", Toast.LENGTH_SHORT).show()
        }
        else if (password.isEmpty()){
            //empty password
            Toast.makeText(this, "Unesite zaporku...", Toast.LENGTH_SHORT).show()
        }
        else if (cPassword.isEmpty()){
            //empty password
            Toast.makeText(this, "Potvrdite zaporku...", Toast.LENGTH_SHORT).show()
        }
        else if (password != cPassword){
            Toast.makeText(this, "Zaporka se ne podudara...", Toast.LENGTH_SHORT).show()
        }
        else{
            createUserAccount()
        }
    }

    private fun createUserAccount() {
        //* 3) Create account - Firebase auth
        //show progress
        progressDialog.setMessage("Izrada računa...")
        progressDialog.show()

        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //account created, now add user info in database
                updateUserInfo()
            }
            .addOnFailureListener{ e->
                //failed creating account
                progressDialog.dismiss()
                Toast.makeText(this, "Kreiranje računa nije uspjelo ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserInfo() {
        //* 4) Save user info - Firebase Realtime Database
        progressDialog.setMessage("Spremanje podataka o korisniku...")

        //timestamp
        val timestamp = System.currentTimeMillis()

        //get current user uid, since user is registered so we can get it now

        val uid = firebaseAuth.uid

        //setup data to add in database
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["uid"] = uid
        hashMap["email"] = email
        hashMap["name"] = name
        hashMap["profileImage"] = "" //add empty, will do in profile edit
        hashMap["userType"] = "user" //possible values are admin/user, will change value to admin manually on firebase database
        hashMap["timestamp"] = timestamp

        //set data to database
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(uid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                //user info saved, open user dashboard
                progressDialog.dismiss()
                Toast.makeText(this, "Account created...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, DashboardUserActivity::class.java))
                finish()

            }
            .addOnFailureListener { e->
                //failed adding data to database
                progressDialog.dismiss()
                Toast.makeText(this, "Failed saving user info due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
