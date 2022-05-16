package com.example.bookappkotlin.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.bookappkotlin.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    //viewbinding

    private lateinit var binding: ActivityLoginBinding


//    firebase auth

    private lateinit var firebaseAuth: FirebaseAuth

//    progress dialog

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

//        init progress dialog, will show while login user

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Molim pricekajte")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle click, not have account, goto register screen
        binding.noAccountTv.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        //handle click, begin login
        binding.loginBtn.setOnClickListener {
            /*Steps
            * 1) Input data
            * 2) validate data
            * 3) Login - Firebase auth
            * 4) check user type - Firebase Auth
            * if user - move to user dashboard
            * if admin - move to admin dashboard*/

            validateData()
        }

        binding.forgotTv.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

    }

    private var email = ""
    private var password = ""


    private fun validateData() {
        //1) Input data
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()

        //2) validate data

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Nevažeći email format...", Toast.LENGTH_SHORT).show()
        }
        else if (password.isEmpty()){
            Toast.makeText(this, "Unesite zaporku...", Toast.LENGTH_SHORT).show()
        }
        else{
            loginUser()
        }
    }

    private fun loginUser() {
        //3) Login - Firebase auth
        //show progress
        progressDialog.setMessage("Prijavljivanje...")
        progressDialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //account created, now add user info in database
                checkUser()
            }
            .addOnFailureListener{ e->
                //failed login
                progressDialog.dismiss()
                Toast.makeText(this, "Prijavljivanje na račun nije uspjelo ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUser() {
        //4) check user type - Firebase Auth
        //            * if user - move to user dashboard
        //            * if admin - move to admin dashboard

        progressDialog.setMessage("Provjera korisnika...")

        val firebaseUser = firebaseAuth.currentUser!!

        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseUser.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()

                    //get user type e.g. user/admin
                    val userType = snapshot.child("userType").value
                    if (userType == "user"){
                        //it's user, open user dashboard
                        startActivity(Intent(this@LoginActivity, DashboardUserActivity::class.java))
                        finish()
                    }
                    else if (userType == "admin") {
                        //it's admin, open admin dashboard
                        startActivity(Intent(this@LoginActivity, DashboardAdminActivity::class.java))
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}