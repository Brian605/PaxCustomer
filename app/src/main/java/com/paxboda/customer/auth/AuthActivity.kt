package com.paxboda.customer.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.paxboda.customer.MainActivity
import com.paxboda.customer.R
import com.paxboda.customer.databinding.ActivityAuthBinding

class AuthActivity:AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var navController:NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding= ActivityAuthBinding.inflate(layoutInflater)
        hideSystemUI()
        setContentView(binding.root)
        val navhostFragment=supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController=navhostFragment.navController

    }

    private fun hideSystemUI(){
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /* override fun onBackPressed() {
         if (navController.currentDestination != null && navController.currentDestination!!.id == R.id.nav_home) {
             moveTaskToBack(true)
         } else {
             super.onBackPressed()
         }
     }*/
    override fun onStart() {
        super.onStart()
        if(Firebase.auth.currentUser!=null){
            startActivity(Intent(this,MainActivity::class.java))
        }
    }

}