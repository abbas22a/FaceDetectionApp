package hu.ait.android.testfirebase.adaptor

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.android.testfirebase.data.Songs

class songAdapter(var context: Context, var uid:String) {

    private var postsList = mutableListOf<String>()
    private var postKeys = mutableListOf<String>()
    private var lastPosition = -1


    fun getItemCount(): Int {
        return postsList.size
    }

    fun addPost(post: Songs, key: String) {
        postsList.add(post.toString())
        postKeys.add(key)
        Log.d("POST", postKeys.toString())

    }



    fun removePostByKey(key: String) {
        val index = postKeys.indexOf(key)
        if (index != -1) {
            postsList.removeAt(index)
            postKeys.removeAt(index)
        }
    }


}


