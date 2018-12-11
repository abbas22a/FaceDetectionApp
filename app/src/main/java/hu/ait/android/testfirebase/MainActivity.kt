package hu.ait.android.testfirebase

import android.content.ContentValues
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.content.pm.PackageManager
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.support.v4.content.ContextCompat
import android.app.Activity
import android.support.v4.app.ActivityCompat
import android.provider.MediaStore
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import com.google.firebase.firestore.*
import hu.ait.android.testfirebase.data.Songs
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private var smileProb = 0.toFloat()
    private lateinit var postsListener: ListenerRegistration
    private var songUrls = mutableListOf<String>()
    private var songIds = mutableListOf<String>()
    private var hypeUrls = mutableListOf<String>()
    private var mildUrls = mutableListOf<String>()
    private var sadUrls = mutableListOf<String>()

    private lateinit var imageView: ImageView  // variable to hold the image view in our activity_main.xml
    //private var resultText: TextView? = null // variable to hold the text view in our activity_main.xml

    private val RESULT_LOAD_IMAGE = 100

    private val REQUEST_PERMISSION_CODE = 200

    companion object {
        private const val CAMERA_REQUEST_CODE = 102
    }

    private lateinit var uploadBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPosts()

        btnAttach.setOnClickListener{
            startActivityForResult(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE), MainActivity.CAMERA_REQUEST_CODE)
        }

        btnAttach.isEnabled = false
        requestNeededPermission()



        btnGallery.setOnClickListener{
            // not sure if it works?
            getImage()
        }

        btnAnalyze.setOnClickListener{
            if(uploadBitmap!= null){

                var smile_prop = getSongs(analyze().toDouble())
                Toast.makeText(this@MainActivity, "Smile Percentage: ${analyze().toDouble()}", Toast.LENGTH_LONG).show()
                val i = Intent(this@MainActivity, songActivity::class.java)
                i.putExtra("SMILE_VALUE", smile_prop)
                startActivity(i)
            } else {
                Toast.makeText(this@MainActivity, "Please upload an image", Toast.LENGTH_LONG).show()

            }
        }
    }

    private fun analyze(): Float {
        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()
        val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(highAccuracyOpts)
        // import a bitmap
        val image = FirebaseVisionImage.fromBitmap(uploadBitmap)

        val result = detector.detectInImage(image)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        // If classification was enabled:
                        if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            smileProb = face.smilingProbability
                            Log.d(ContentValues.TAG, "The smiling prob is $smileProb")
                            var str = "The smiling prob is $smileProb"
                            tvResult.text = str

                        } else {
                            Log.d(ContentValues.TAG, "No smiling prob TAT")
                            var str = "Not able to find smilling prob"
                            tvResult.text = str
                            smileProb = (-1).toFloat()
                        }
                    }
                }
                .addOnFailureListener { Log.e(ContentValues.TAG, "The ML Kit is not working!") }

        return smileProb
    }

    private fun requestNeededPermission() {
        if (ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            android.Manifest.permission.CAMERA)) {
                Toast.makeText(this,
                        "I need it for camera", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    1001)
        } else {
            // WE ALREADY HAVE IT
            btnAttach.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "CAMERA perm granted", Toast.LENGTH_SHORT).show()
                    btnAttach.isEnabled = true
                } else {
                    Toast.makeText(this, "CAMERA perm NOT granted", Toast.LENGTH_SHORT).show()
                    btnAttach.isEnabled = false
                }
            }
        }
    }


    fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED

    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)

    }

    // when the "GET IMAGE" Button is clicked this function is executed
    fun getImage() {
        // check if user has given us permission to access the gallery
        if (checkPermission()) {
            val choosePhotoIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(choosePhotoIntent, RESULT_LOAD_IMAGE)
        } else {
            requestPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // get the photo URI from the gallery, find the file path from URI and send the file path to ConfirmPhoto
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {

            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])

            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            uploadBitmap = BitmapFactory.decodeFile(picturePath)
            ivPicture.setImageBitmap(uploadBitmap)
        }

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                uploadBitmap = it.extras.get("data") as Bitmap
                ivPicture.setImageBitmap(uploadBitmap)
            }
        }
    }


    fun initPosts() {
        val db = FirebaseFirestore.getInstance()
        val postsCollection = db.collection("songUrls")

        postsListener = postsCollection.addSnapshotListener(object: EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?, p1: FirebaseFirestoreException?) {
                if (p1 != null) {
                    Toast.makeText(this@MainActivity, "Error: ${p1.message}",
                            Toast.LENGTH_LONG).show()
                    return
                }
                for (docChange in querySnapshot!!.documentChanges) {
                    when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            val post = docChange.document.toObject(Songs::class.java)
                            val id = docChange.document.id
                            songIds.add(id)
                            songUrls.add(post.url)
                        }

                    }
                }

                for (i in 0 until songUrls.size) {
                    when {
                        songIds[i].first() == 'h' -> hypeUrls.add(songUrls[i])
                        songIds[i].first() == 'm' -> mildUrls.add(songUrls[i])
                        songIds[i].first() == 's' -> sadUrls.add(songUrls[i])
                    }
                }
            }
        })
    }

    private fun getSongs(smile_percent: Double): String{


        return when {
            (smile_percent <= 0.333) -> {
                val randomIndex = (0 until sadUrls.size).shuffled().first()
                sadUrls[randomIndex]
            }
            ( smile_percent>0.333 && smile_percent <= 0.666)-> {
                val randomIndex = (0 until mildUrls.size).shuffled().first()
                mildUrls[randomIndex]
            }
            else -> {
                val randomIndex = (0 until hypeUrls.size).shuffled().first()
                hypeUrls[randomIndex]
            }
        }
    }







}
