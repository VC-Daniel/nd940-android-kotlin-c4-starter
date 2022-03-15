package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity. Uses a customized login screen based on the documentation
 * https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout
 */
class AuthenticationActivity : AppCompatActivity() {

    /** Facilitates using a custom layout for the Firebase Login screen */
    private lateinit var customLoginLayout: AuthMethodPickerLayout

    /** Bindings for the layout */
    private lateinit var binding: ActivityAuthenticationBinding

    companion object {
        /** Identifies a request to sign in */
        const val SIGN_IN_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_authentication
        )

        // Use a custom layout for logging in which gives the user the option to login using an
        // email or a Google account.
        customLoginLayout = AuthMethodPickerLayout.Builder(R.layout.login_authentication)
            .setEmailButtonId(R.id.emailSignInButton)
            .setGoogleButtonId(R.id.googleSignInButton)
            .build()

        binding.loginButton.setOnClickListener {
            launchSignInFlow()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the results of a login request
        if (requestCode == SIGN_IN_REQUEST_CODE) {

            if (resultCode == Activity.RESULT_OK) {
                // User successfully signed in so show them their reminders
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)


            } else {
                // If there is no logged-in user then take them to a register screen to
                // create an account
                customLoginLayout =
                    AuthMethodPickerLayout.Builder(R.layout.register_authentication)
                        .setEmailButtonId(R.id.emailRegisterButton)
                        .setGoogleButtonId(R.id.googleRegisterButton)
                        .build()

                binding.loginButton.text = getString(R.string.register)
            }
        }
    }

    /**
     * Give users the option to sign in or register with their email or Google account.
     */
    private fun launchSignInFlow() {

        // Allow logging in with an email or Google account
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch a Firebase sign-in intent. Once the user has either logged in or
        // cancelled and returned to this activity the appropriate response will be
        // performed in onActivityResult
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setAuthMethodPickerLayout(customLoginLayout)
                .build(),
            SIGN_IN_REQUEST_CODE
        )
    }
}