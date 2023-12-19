import android.graphics.Point
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import okhttp3.*
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.tender.R
import java.io.IOException

class LoginFragment : DialogFragment() {

    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        return view
    }
    private fun playSound() {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer.create(context, R.raw.yippee)
        mediaPlayer.start()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginButton.setOnClickListener {
            performLogin(emailEditText.text.toString(), passwordEditText.text.toString())
        }
    }
    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        val size = Point()

        // Get the screen size for the device
        val display = window?.windowManager?.defaultDisplay
        display?.getSize(size)

        // Set the width of the dialog proportional to 75% of the screen width
        window?.setLayout((size.x * 0.75).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
    private fun performLogin(email: String, password: String) {
        val client = OkHttpClient()

        val formBody = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("https://github.com/login")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                playSound()
                activity?.runOnUiThread {
                    Toast.makeText(activity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {

                    playSound()
                    if (response.isSuccessful) {
                        // Handle successful response
                        Toast.makeText(activity, "Login successful", Toast.LENGTH_SHORT).show()
                    } else {
                        // Handle unsuccessful response (e.g., wrong credentials)
                        Toast.makeText(activity, "Login failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}

