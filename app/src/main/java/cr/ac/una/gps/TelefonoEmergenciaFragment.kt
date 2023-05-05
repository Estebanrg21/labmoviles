package cr.ac.una.gps

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cr.ac.una.gps.databinding.FragmentAreaRestringidaBinding
import cr.ac.una.gps.databinding.FragmentTelefonoEmergenciaBinding

class TelefonoEmergenciaFragment : Fragment() {
    private var _binding: FragmentTelefonoEmergenciaBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTelefonoEmergenciaBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.saveBtn.setOnClickListener{
            try {
                (activity as MainActivity).telefonoEmergencia = (binding.phoneEt.text.toString()).toInt()
            } catch (ex: java.lang.Exception) {

            }

        }
    }

}