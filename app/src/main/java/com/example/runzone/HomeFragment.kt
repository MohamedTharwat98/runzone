package com.example.runzone

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var titlesList = mutableListOf<String>()
    private var detailsList = mutableListOf<String>()
    private var imagesList = mutableListOf<Int>()

    private val MY_PERMISSIONS_REQUEST = 1



    private lateinit var recyclerView: RecyclerView

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        val permissionsToRequest = mutableListOf<String>()

        // Record data
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // Read data
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                permissionsToRequest.toTypedArray(),
                MY_PERMISSIONS_REQUEST
            )
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        posToList()
        recyclerView = view.findViewById(R.id.recylerView_Home)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = RecyclerAdapter(titlesList, detailsList, imagesList,this.requireContext())

        return view
    }

    private fun addToList(title: String, details: String, image: Int) {
        titlesList.add(title)
        detailsList.add(details)
        imagesList.add(image)
    }

    private fun posToList() {
        for (i in 1..2) {
            if (i == 1) {
                addToList("Escape from Dystopia", "A daring runner escapes an oppressive city, navigating tunnels, evading patrols, and outrunning pursuit to lead the resistance to victory.", R.drawable.escapefromdystopia)
            }else {
                addToList("Run Zone", "Unlock Your Potential, Embrace the Run!", R.drawable.logo2)
            }

        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}