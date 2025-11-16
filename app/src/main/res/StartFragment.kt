package com.example.qr_scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.qr_scan.databinding.FragmentStartBinding

class StartFragment : Fragment() {
    private var _binding: FragmentStartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as MainActivity).showBottomNavigation(false)

        binding.startButton.setOnClickListener {
            val pass = binding.edPass.text.toString()
            val prev = binding.previevText

            if(pass == "2468") {
                (activity as MainActivity).replaceFragment(ScannerFragment())
                (activity as MainActivity).showBottomNavigation(true)
            } else {
                prev.text = "Введён неправильный пароль\nПовторите попытку"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}