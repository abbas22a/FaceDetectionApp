package hu.ait.android.testfirebase

import android.media.MediaPlayer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import hu.ait.android.testfirebase.data.Songs
import java.io.IOException


class songActivity : AppCompatActivity(), MediaPlayer.OnPreparedListener{





    private lateinit var mp: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)



        // Initializing variables
        mp = MediaPlayer()


        val extras = intent.extras
        if (extras != null) {

            val url = extras.getString("SMILE_VALUE")
            fetchAudioUrlFromFirebase(url)
        }

    }















    private fun fetchAudioUrlFromFirebase(uri: String) {


        val storage = FirebaseStorage.getInstance()
        // Create a storage reference from our app
        val storageRef = storage.getReferenceFromUrl(uri)
        storageRef.downloadUrl.addOnSuccessListener(OnSuccessListener<Any> { uri ->
            try {
                // Download url of file
                val url = uri.toString()
                mp.setDataSource(url)
                // wait for media player to get prepare
                mp.setOnPreparedListener(this@songActivity)
                mp.prepareAsync()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        })
                .addOnFailureListener(OnFailureListener { e -> Log.i("TAG", e.message) })
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
    }

    //need to make it stop when exit from app
    override fun onStop() {
        super.onStop()
        mp.stop()
    }


    override fun onPause() {
        super.onPause()
        mp.stop()
    }





}


