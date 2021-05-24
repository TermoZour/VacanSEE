package com.punchy.pmt.vacansee

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.punchy.pmt.vacansee.searchJobs.*
import com.punchy.pmt.vacansee.httpRequests.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.net.URL

//import com.punchy.pmt.vacansee.httpRequests.getSavedJobs

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
var savedJobsList = mutableListOf<Job>()
var pinnedTouchDown = true

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // remove action bar shadow
        (activity as AppCompatActivity?)!!.supportActionBar?.elevation = 0f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var userProfile: Profile
        var response: List<String>

        val trashBinIcon = resources.getDrawable(
            R.drawable.ic_baseline_delete_forever_24,
            null
        )

        val profileView: View = inflater.inflate(R.layout.fragment_profile, container, false)

        val bottomSheetView = profileView.findViewById<LinearLayout>(R.id.savedJobsBackdrop)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)

        // set bottom sheet state as expanded by default
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // Inflate the layout for this fragment
        val profileRecycler = profileView.findViewById<RecyclerView>(R.id.savedJobsRecyclerView)
//        Initializing the type of layout, here I have used LinearLayoutManager you can try GridLayoutManager
//        Based on your requirement to allow vertical or horizontal scroll , you can change it in  LinearLayout.VERTICAL
        profileRecycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        // Inflate the layout for this fragment

        profileView.findViewById<Button>(R.id.goToJobsSearch).setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_jobsFragment)
        }

        fun loadSavedJobs(parentFragment: Fragment) = CoroutineScope(Dispatchers.Main).launch {
            if (checkWIFI(context)) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                val task = async(Dispatchers.IO) {
                    getSavedJobs()
                }
                savedJobsList = task.await()

                if (savedJobsList.isEmpty()) {
                    // get progress bar and hide it after the jobs load.
                    bottomSheetView.findViewById<ProgressBar>(R.id.savedJobsProgressBar).visibility =
                        View.VISIBLE

                    // get error view and make it visible if the fetching fails
                    bottomSheetView.findViewById<TextView>(R.id.savedJobsErrorText).text =
                        "Couldn't connect to endpoint"
                    bottomSheetView.findViewById<LinearLayout>(R.id.savedJobsErrorView).visibility =
                        View.VISIBLE
                } else {
                    // get progress bar and hide it after the jobs load.
                    bottomSheetView.findViewById<ProgressBar>(R.id.savedJobsProgressBar).visibility =
                        View.GONE
                }

                val rvAdapter = JobsRvAdapter(savedJobsList, parentFragment)
                profileRecycler.adapter = rvAdapter

                val backdropTitle =
                    bottomSheetView.findViewById<TextView>(R.id.savedJobsBackdropTitle)
                backdropTitle.text = "Saved jobs found (${savedJobsList.size})"
                bottomSheetView.findViewById<ProgressBar>(R.id.savedJobsProgressBar).visibility =
                    View.GONE
                rvAdapter.notifyDataSetChanged()

                val saveColor = Color.rgb(183f, 28f, 28f)
                //Save to swipe logic
                val myCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean = false

                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int
                    ) {
                        // More code here
                        pinnedTouchDown = false

                        fun assSave() = CoroutineScope(Dispatchers.Main).launch {
                            val save = async(Dispatchers.IO) {
                                unpinJob(savedJobsList[viewHolder.adapterPosition].jobId.toString())
                            }
                            val aSave = save.await()
                            //rvAdapter?.notifyItemRemoved(viewHolder.adapterPosition)

                        }
                        assSave()
                        rvAdapter.notifyItemChanged(viewHolder.adapterPosition)
                        savedJobsList.removeAt(viewHolder.adapterPosition)
                        val rvAdapter = JobsRvAdapter(savedJobsList, parentFragment)
                        profileRecycler.adapter = rvAdapter


                        Toast.makeText(context, "Job saved", Toast.LENGTH_SHORT).show()
                        return
                    }

                    override fun onChildDraw(
                        c: Canvas,
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        dX: Float,
                        dY: Float,
                        actionState: Int,
                        isCurrentlyActive: Boolean
                    ) {
                        // More code here
                        if (touchDown) {
                            touchDown = false
                        }
                        c.clipRect(
                            15f, viewHolder.itemView.top.toFloat() + 20f,
                            dX + 50f, viewHolder.itemView.bottom.toFloat() - 20f
                        )

                        //making the save thingy change color

                        c.drawColor(saveColor)
                        val textMargin = 100
                        trashBinIcon.bounds = Rect(
                            textMargin,
                            viewHolder.itemView.top + (dX * 1.5).toInt() + textMargin,
                            textMargin + trashBinIcon.intrinsicWidth,
                            viewHolder.itemView.top + (dX * 1.5).toInt() + trashBinIcon.intrinsicHeight
                                    + textMargin
                        )
                        trashBinIcon.draw(c)
                        if (dX >= 214) {//SAVE THRESHOLD
                            if (pinnedTouchDown) {
                                fun assSave() = CoroutineScope(Dispatchers.Main).launch {
                                    val save = async(Dispatchers.IO) {
                                        unpinJob(savedJobsList[viewHolder.adapterPosition].jobId.toString())
                                    }
                                    val aSave = save.await()
                                }
                                assSave()
                                rvAdapter.notifyItemChanged(viewHolder.adapterPosition)
                                savedJobsList.removeAt(viewHolder.adapterPosition)
                                val rvAdapter = JobsRvAdapter(savedJobsList, parentFragment)
                                profileRecycler.adapter = rvAdapter



                                println("limit hit")
                                Toast.makeText(context, "Job saved", Toast.LENGTH_SHORT).show()
                                pinnedTouchDown = false

                            }
                            return

                        }
                        super.onChildDraw(
                            c, recyclerView, viewHolder,
                            dX, dY, actionState, isCurrentlyActive
                        )

                    }


                }
                val myHelper = ItemTouchHelper(myCallback)
                myHelper.attachToRecyclerView(profileRecycler)


                bottomSheetView.findViewById<LinearLayout>(R.id.savedJobsErrorView).visibility =
                    View.GONE
            } else {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }
        loadSavedJobs(this)

        fun loadProfileData() = CoroutineScope(Dispatchers.Main).launch {
            val task = async(Dispatchers.IO) {
                getProfile()
            }
            response = task.await()

            val profileParseTemplate = object : TypeToken<Profile>() {}.type
            val profileDataString = response[1].removePrefix("[").removeSuffix("]")

            userProfile = Gson().fromJson(profileDataString, profileParseTemplate)
            Log.d("ProfileFragment", userProfile.toString())

            val userFirstName = profileView.findViewById<TextView>(R.id.userFirstName)
            val userLastName = profileView.findViewById<TextView>(R.id.userLastName)
            val userEmail = profileView.findViewById<TextView>(R.id.userEmail)
            val userProfilePicture = profileView.findViewById<ImageView>(R.id.userProfilePicture)

            userFirstName.text = userProfile.first_name
            userLastName.text = userProfile.last_name
            userEmail.text = userProfile.email

            var url: URL? = null
            try {
                url = URL(userProfile.profile_url)
            } catch (e: MalformedURLException) {
                Log.d("JobsRvAdapter", "Image url is empty")
            }

//        val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())

            fun loadImage() = CoroutineScope(Dispatchers.Main).launch {
                val task = async(Dispatchers.IO) {
                    try {
                        BitmapFactory.decodeStream(url?.openConnection()?.getInputStream())
                    } catch (e: FileNotFoundException) {
                        Log.e("JobsRvAdapter - Icon grab", "$e for URL: ${userProfile.profile_url}")
                        null
                    }
                }

                val bmp = task.await()
                if (bmp != null) {
                    userProfilePicture.setImageBitmap(bmp)
                } else {
                    userProfilePicture.setImageResource(R.drawable.ic_baseline_android_24)
                }
            }
            loadImage()
        }
        loadProfileData()

        val backdropTitle = bottomSheetView.findViewById<TextView>(R.id.savedJobsBackdropTitle)
        backdropTitle.text = "Saved jobs found (${savedJobsList.size})"

//        pass the values to RvAdapter
        val rvAdapter = JobsRvAdapter(savedJobsList, this)

//        set the recyclerView to the adapter
        profileRecycler.adapter = rvAdapter

        profileRecycler.setOnTouchListener { _, event ->
            if (MotionEvent.ACTION_UP == event.action) {
                println("Up")
                pinnedTouchDown = false
            }
            if (MotionEvent.ACTION_DOWN == event.action) {
                println("Down")
                pinnedTouchDown = true
            }
            false // return is important...
        }


        val deleteProfileButton = profileView.findViewById<Button>(R.id.deleteAccountButton)
        deleteProfileButton.setOnClickListener {
            Log.d("ProfileFragment", "Clicked on DELETE ACCOUNT")
            if (checkWIFI(context)) {
                val delAccResponse = deleteAccount()

                if (delAccResponse == 200) {
                    Toast.makeText(context, "Account deleted. Restart app.", Toast.LENGTH_LONG)
                } else {
                    Toast.makeText(
                        context,
                        "Failed to delete account. $delAccResponse",
                        Toast.LENGTH_LONG
                    )
                }
            } else {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }

        }


        return profileView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}