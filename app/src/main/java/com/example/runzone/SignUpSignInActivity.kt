package com.example.runzone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.runzone.databinding.ActivitySignUpSignInBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SignUpSignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpSignInBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {
            // User is already signed in, redirect to main activity or dashboard
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

    }

    private fun signInWithGoogle() {
        val googleSignInClient = GoogleSignIn.getClient(this, gso())
        val googleSignInIntent = googleSignInClient.signInIntent
        startActivityForResult(googleSignInIntent, RC_SIGN_IN)
    }


    private fun gso(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                // Google sign-in was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account)

            } catch (e: ApiException) {
                // Google sign-in failed, handle the error
                Toast.makeText(this, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT)
                    .show()
                // Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign-in with Firebase using Google credentials is successful
                    val currentUser = firebaseAuth.currentUser
                    Toast.makeText(
                        this,
                        "Signed in as ${currentUser?.displayName}",
                        Toast.LENGTH_SHORT
                    ).show()



                    // Proceed to the main activity or dashboard
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Sign-in with Firebase using Google credentials failed, handle the error
                    Toast.makeText(
                        this,
                        "Firebase authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
