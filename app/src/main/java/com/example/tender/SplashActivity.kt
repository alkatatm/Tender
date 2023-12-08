package com.example.tender

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class SplashActivity : AppCompatActivity() {

    private val RC_SIGN_IN = 1
    private var mGoogleSignInClient: GoogleSignInClient? = null
    lateinit var sign_in_button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("userdata", Context.MODE_PRIVATE)
        val name = prefs.getString("name",null)
        val email = prefs.getString("email",null)

        if (name!= null){
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email",email)
            intent.putExtra("name",name )
            startActivity(intent)

        }
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        sign_in_button = findViewById(R.id.sign_in_button)
        sign_in_button.setOnClickListener {
            val intent = mGoogleSignInClient!!.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)
        }
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task =
                GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account : GoogleSignInAccount? = task.getResult(ApiException::class.java)
                updateUI(account)

            } catch (e: ApiException) {
                // The ApiException status code indicates the detailed failure reason.
                // Please refer to the GoogleSignInStatusCodes class reference for more information.
                Log.e("TAG","signInResult:failed code=" + e.statusCode)
            }
        }
    }

    fun updateUI(account: GoogleSignInAccount?) {
        if(account != null){
            val email = account.email
            val name = account.displayName

            val sharedPreferences = getSharedPreferences("userdata", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("email",account.email)
            editor.putString("name",account.displayName)

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email",email)
            intent.putExtra("name",name)
            startActivity(intent)

        }

    }
}