package com.paxboda.customer.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.paxboda.customer.MainActivity
import com.paxboda.customer.R
import com.paxboda.customer.constants.Db
import com.paxboda.customer.databinding.FragmentRegisterBinding
import com.paxboda.customer.models.User
import com.paxboda.customer.utils.Commons
import com.paxboda.customer.utils.showSnackBar

class RegisterFragment : Fragment() {
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var user: User
   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
user=RegisterFragmentArgs.fromBundle(requireArguments()).user
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding= FragmentRegisterBinding.inflate(inflater,container,false)
        binding.btnRegister.setOnClickListener {
            getInput()
        }
        return binding.root
    }
    private fun getInput() {
        val userName=binding.nameInput.text.toString().trim()
         if (userName.isEmpty()){
            showFailedMessage("Please provide your name")
            return
        }

        user.user_name=userName
        user.icons_id= Commons().generateIconsId()
        user.status="Active"
        user.user_roles= listOf("User")
        registerUser(user)
    }

    private fun registerUser(user: User) {
        binding.progressBar.show()
        Firebase.auth.createUserWithEmailAndPassword(user.emailAddress,user.user_phone)
            .addOnSuccessListener {
               user.user_id=it.user!!.uid
                if (requireActivity().intent.getIntExtra("agentId",0)!=0){
                    updateAgent(user.icons_id,requireActivity().intent.getIntExtra("agentId",0))
                }
                saveUser(user)

            }.addOnFailureListener {
                binding.progressBar.hide()
                showSnackBar(binding.root,it.message!!)

            }
    }

    private fun updateAgent(iconsId: Int,agentId:Int) {
        Firebase.firestore.collection(Db.USERS)
            .whereEqualTo("icons_id",agentId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot !=null && !snapshot.isEmpty){
                val agent=snapshot.documents.first().toObject(User::class.java)
                  val refs=agent!!.user_referrals as MutableList
                  refs.removeAll {
                      it==iconsId
                  }
                    refs.add(iconsId)
                    snapshot.first().reference.update("user_referrals",refs)
              }
            }
    }

     private fun saveUser(user: User) {
         if (requireActivity().intent.getIntExtra("agentId",0)!=0){
             user.promoter_id=requireActivity().intent.getIntExtra("agentId",0)
         }
        Firebase.firestore.collection(Db.USERS)
            .document(user.user_id)
            .set(user)
            .addOnSuccessListener {
                startActivity(Intent(requireActivity(), MainActivity::class.java))
                binding.progressBar.hide()

            }.addOnFailureListener {
                binding.progressBar.hide()
                showSnackBar(binding.root,it.message!!)
            }
    }

    private fun showFailedMessage(s: String) {
        MaterialAlertDialogBuilder(requireActivity())
            .setBackground(ContextCompat.getDrawable(requireActivity(), R.drawable.bg_white_rounded_10))
            .setTitle("You missed something")
            .setMessage(s)
            .create()
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (Firebase.auth.currentUser!=null){
            startActivity(Intent(requireActivity(),MainActivity::class.java))
        }
    }
}