package com.example.runzone

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SessionsListFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var customAdapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sessions_list, container, false)

        listView = view.findViewById(R.id.listView)
        customAdapter = SessionAdapter(requireContext(), mutableListOf())
        listView.adapter = customAdapter

        // Get a reference to your Firebase Realtime Database
        val database = FirebaseDatabase.getInstance("https://plucky-balm-389709-default-rtdb.europe-west1.firebasedatabase.app/")
        val databaseReference = database.getReference("sessions")

        // Attach a ValueEventListener to listen for data changes
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val sessionList = mutableListOf<Session>()

                for (snapshot in dataSnapshot.children) {
                    val duration = snapshot.child("duration").getValue(String::class.java)
                    val date = snapshot.child("date").getValue(String::class.java)
                    val avgHR = snapshot.child("avgHR").getValue(Float::class.java) ?: 0F
                    val maxHR = snapshot.child("maxHR").getValue(Float::class.java) ?: 0F
                    val age = snapshot.child("age").getValue(Int::class.java) ?: 0
                    val missionType = snapshot.child("missionType").getValue(String::class.java)
                    val zone0 = snapshot.child("zone0").getValue(Float::class.java) ?: 0F
                    val zone1 = snapshot.child("zone1").getValue(Float::class.java) ?: 0F
                    val zone2 = snapshot.child("zone2").getValue(Float::class.java) ?: 0F
                    val zone3 = snapshot.child("zone3").getValue(Float::class.java) ?: 0F
                    val zone4 = snapshot.child("zone4").getValue(Float::class.java) ?: 0F

                    val session = Session(
                        duration ?: "",
                        date ?: "",
                        avgHR,
                        maxHR,
                        age,
                        missionType ?: "",
                        zone0,
                        zone1,
                        zone2,
                        zone3,
                        zone4
                    )

                    sessionList.add(session)
                }

                customAdapter.clear()
                customAdapter.addAll(sessionList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors here
            }
        })

        return view
    }

    inner class SessionAdapter(context: Context, private val sessions: List<Session>) :
        ArrayAdapter<Session>(context, 0, sessions) {

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val session = getItem(position)

            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_session, parent, false)

            val durationTextView: TextView = view.findViewById(R.id.durationTextView)
            val dateTextView: TextView = view.findViewById(R.id.dateTextView)
            val avgHRTextView: TextView = view.findViewById(R.id.avgHRTextView)
            val maxHRTextView: TextView = view.findViewById(R.id.maxHRTextView)
            val ageTextView: TextView = view.findViewById(R.id.ageTextView)
            val missionTypeTextView: TextView = view.findViewById(R.id.missionTypeTextView)
            val zone0TextView: TextView = view.findViewById(R.id.zone0TextView)
            val zone1TextView: TextView = view.findViewById(R.id.zone1TextView)
            val zone2TextView: TextView = view.findViewById(R.id.zone2TextView)
            val zone3TextView: TextView = view.findViewById(R.id.zone3TextView)
            val zone4TextView: TextView = view.findViewById(R.id.zone4TextView)
            // ... (find other TextViews or UI elements for other properties)

            // Set data for each property
            durationTextView.text = "Duration : "+ session?.duration
            dateTextView.text = "Date : "+ session?.date
            avgHRTextView.text = "Average Heart Rate : "+ session?.avgHR.toString()
             maxHRTextView.text= "Max Heart Rate : " + session?.maxHR.toString()
             ageTextView.text= "Age : " + session?.age.toString()
             missionTypeTextView.text = "Mission Type :" + session?.missionType.toString()
             zone0TextView.text = "Zone 0 = " + session?.zone0.toString()+ " seconds"
             zone1TextView.text = "Zone 1 = " + session?.zone1.toString()+ " seconds"
             zone2TextView.text = "Zone 2 = " + session?.zone2.toString()+ " seconds"
             zone3TextView.text = "Zone 3 = " + session?.zone3.toString()+ " seconds"
             zone4TextView.text = "Zone 4 = " + session?.zone4.toString()+ " seconds"

            return view
        }
    }
}
