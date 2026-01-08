package com.example.mydailyroutine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "MainActivity"

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java) as GoogleSignInAccount
                onSignedIn(account)
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed: ${e.message}")
            }
        } else {
            Log.d(TAG, "Sign-in canceled or failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // simple layout with buttons and textview

        // Configure sign-in to request the user's ID token and email
        val serverClientId = getString(R.string.server_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId) // ID token to send to your backend or exchange for Firebase credential
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // UI
        val signInBtn = findViewById<Button>(R.id.btn_sign_in)
        val signOutBtn = findViewById<Button>(R.id.btn_sign_out)
        val statusTv = findViewById<TextView>(R.id.tv_status)

        // If already signed-in, update UI
        GoogleSignIn.getLastSignedInAccount(this)?.let { account ->
            statusTv.text = "Signed in as: ${account.email}"
        }

        signInBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        signOutBtn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                statusTv.text = "Signed out"
            }
        }
    }

    private fun onSignedIn(account: GoogleSignInAccount) {
        // Access account info
        val email = account.email
        val displayName = account.displayName
        val idToken = account.idToken // IMPORTANT: send this token to your backend or use it to authenticate with Firebase
        Log.d(TAG, "Signed in: $email, idToken length=${idToken?.length ?: 0}")

        // Example: update UI
        val statusTv = findViewById<TextView>(R.id.tv_status)
        statusTv.text = "Signed in as: $displayName <$email>"

        // TODO: Use `idToken` or `account` to identify the user for syncing.
        // - If using Firestore via Firebase Auth, exchange idToken for FirebaseCredential.
        // - If using your own backend, send the idToken to your backend and verify it server-side.
    }
}