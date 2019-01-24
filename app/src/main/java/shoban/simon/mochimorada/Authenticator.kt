package shoban.simon.mochimorada

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class Authenticator(private val context: Context) {
    // Firestore and Google Auth
    private var currentUser: FirebaseUser? = null
    private var account: GoogleSignInAccount? = null
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var mGoogleSignInClient: GoogleSignInClient

    init {
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        // Request Google ID Token
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.resources.getString(R.string.client_id))
                .requestEmail()
                .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun promptForLogin() {
        (context as Activity).startActivityForResult(mGoogleSignInClient.signInIntent, ListLocationsActivity.RC_SIGN_IN)
    }

    fun userIsLoggedIn() : Boolean = mAuth.currentUser != null

    fun refreshCurrentUser(context: Context) {
        currentUser = mAuth.currentUser
        account = GoogleSignIn.getLastSignedInAccount(context)
    }

    fun authenticate(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            // Google Sign In was successful, authenticate with Firebase
            val account = task.getResult(ApiException::class.java)

            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            // Google Sign In failed, log error
            Log.w("Bad", "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(acct?.idToken, null)

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(context as Activity) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, write Receptacle data to Firebase
                        currentUser = mAuth.currentUser
                        account = GoogleSignIn.getLastSignedInAccount(context)
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(context, "Authentication Failed", Toast.LENGTH_LONG).show()
                        //Snackbar.make(main_layout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                    }
                }
    }
}