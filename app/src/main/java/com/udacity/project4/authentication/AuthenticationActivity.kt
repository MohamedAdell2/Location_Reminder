package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.map
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val signingLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()){ this.onSignInResult(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authState.observe(this) { state ->
            if (state == AuthenticationState.AUTHENTICATED) {
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val binding: ActivityAuthenticationBinding = DataBindingUtil
                    .setContentView(this, R.layout.activity_authentication)
                binding.loginBt.setOnClickListener { login() }
            }
        }

    }

    private fun login() {
        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build())

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        signingLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser?.displayName
            Toast.makeText(this, "Welcome $user" , Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Log in failed" , Toast.LENGTH_SHORT).show()
        }
    }

    private val authState = FirebaseUserLiveData().map { user ->
        if (user != null){
            AuthenticationState.AUTHENTICATED
        }else{
            AuthenticationState.UNAUTHENTICATED
        }
    }

    enum class AuthenticationState{
        AUTHENTICATED , UNAUTHENTICATED
    }
}
