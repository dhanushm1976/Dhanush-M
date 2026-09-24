package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    fun ensureInitialized(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    try {
                        FirebaseApp.initializeApp(context)
                        Log.d(TAG, "Firebase initialized with google-services config")
                    } catch (e: Exception) {
                        Log.w(TAG, "Default init failed, initializing with fallback options: ${e.message}")
                        val fallbackOptions = FirebaseOptions.Builder()
                            .setApplicationId("com.aistudio.studysync.app")
                            .setProjectId("studysync-cloud")
                            .setApiKey("AIzaSyB3A9B7C5D1E2F3G4H5I6J7K8L9M0N1P2Q")
                            .build()
                        FirebaseApp.initializeApp(context, fallbackOptions)
                    }
                }
                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase", e)
            }
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        ensureInitialized(context)
        return try {
            val firestore = FirebaseFirestore.getInstance()
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
            } catch (_: Exception) {
                // Settings might have already been set
            }
            firestore
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Firestore instance", e)
            null
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        ensureInitialized(context)
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining FirebaseAuth instance", e)
            null
        }
    }
}
