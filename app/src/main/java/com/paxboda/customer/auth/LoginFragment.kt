package com.paxboda.customer.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.paxboda.customer.MainActivity
import com.paxboda.customer.R
import com.paxboda.customer.databinding.FragmentLoginBinding
import com.paxboda.customer.models.User
import com.paxboda.customer.utils.Commons

class LoginFragment: Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private lateinit var user: User

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= FragmentLoginBinding.inflate(inflater,container,false)
        user= User()

        binding.btnLogin.setOnClickListener {
            getInput()
        }
        return binding.root
    }

    private fun getInput() {
        val phoneNumber=binding.phoneInput.text.toString().trim()

        if (phoneNumber.length!=10 || !phoneNumber.isDigitsOnly()){
            MaterialAlertDialogBuilder(requireContext())
                .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_white_rounded_10))
                .setTitle("You missed something")
                .setMessage("Please provide a valid phone number")
                .setPositiveButton("OK"){
                    dialog,_->
                    dialog?.dismiss()
                }.create()
                .show()
            return
        }
        user.user_phone=phoneNumber
        user.emailAddress= Commons().generateEmail(phoneNumber)
        loginUser(phoneNumber)
    }

    private fun loginUser(phoneNumber: String){
        binding.progressBar.show()
        Firebase.auth.signInWithEmailAndPassword(Commons().generateEmail(phoneNumber),phoneNumber)
            .addOnSuccessListener {
                binding.progressBar.hide()
                startActivity(Intent(requireContext(), MainActivity::class.java))
            }.addOnFailureListener {
                binding.progressBar.hide()
                if (it.message==null){
                    return@addOnFailureListener
                }
                if (it.message!!.contains("no user record",true)){
                   findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegisterFragment(user))
                }
            }
    }


}