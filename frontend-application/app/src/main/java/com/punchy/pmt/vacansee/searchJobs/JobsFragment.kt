package com.punchy.pmt.vacansee.searchJobs

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.punchy.pmt.vacansee.R
import com.punchy.pmt.vacansee.checkWIFI
import com.punchy.pmt.vacansee.searchJobs.httpRequests.Job
import com.punchy.pmt.vacansee.searchJobs.httpRequests.getJobs
import com.punchy.pmt.vacansee.searchJobs.httpRequests.saveJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

var savedItems: MutableList<Int> = mutableListOf()
var touchDown = true
var jobsList = mutableListOf<Job>()

var searchParam: String? = null

var locationParam: String? = null
var partTimeParam: Boolean? = null
var fullTimeParam: Boolean? = null

var locationDistanceParam = 10
var minSalaryParam = 50
var maxSalaryParam = 1000

/**
 * A simple [Fragment] subclass.
 * Use the [JobsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JobsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        super.onCreate(savedInstanceState)
        retainInstance = true;


        // remove action bar shadow
        (activity as AppCompatActivity?)!!.supportActionBar?.elevation = 0f

        // don't destroy the contents when switching fragments
        retainInstance = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // add options menu support for action bar
        setHasOptionsMenu(true)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_menu, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.profileButton -> {
            findNavController().navigate(R.id.action_jobsFragment_to_profileFragment)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val trashBinIcon = resources.getDrawable(
            R.drawable.ic_baseline_archive_24,
            null
        )

        val jobsView: View = inflater.inflate(R.layout.fragment_jobs, container, false)

        val bottomSheetView = jobsView.findViewById<LinearLayout>(R.id.jobsForegroundView)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        val backdropTitle = bottomSheetView.findViewById<TextView>(R.id.jobsBackdropTitle)

        // set bottom sheet state as expanded by default
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Inflate the layout for this fragment
        val jobsRecyclerView = jobsView.findViewById<RecyclerView>(R.id.jobsRecyclerView)
//        Initializing the type of layout, here I have used LinearLayoutManager you can try GridLayoutManager
//        Based on your requirement to allow vertical or horizontal scroll , you can change it in  LinearLayout.VERTICAL
        jobsRecyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )


        fun loadData(parentFragment: Fragment) =
            CoroutineScope(Dispatchers.Main).launch {
                if (checkWIFI(context)) {
                    val task = async(Dispatchers.IO) {
                        getJobs(
                            searchParam,
                            locationParam,
                            partTimeParam,
                            fullTimeParam,
                            locationDistanceParam,
                            minSalaryParam,
                            maxSalaryParam
                        )
                    }
                    val response = task.await()

                    if (response[0] == "200") {
                        val gson = Gson()
                        val parseTemplate = object : TypeToken<MutableList<Job>>() {}.type

                        jobsList = gson.fromJson(response[1], parseTemplate)

                        if (jobsList.isEmpty()) {
                            // get progress bar and hide it after the jobs load.
                            bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                                View.INVISIBLE

                            // get error view and make it visible if the fetching fails
                            bottomSheetView.findViewById<TextView>(R.id.errorText).text =
                                "No jobs found with given filters."

                            backdropTitle.text = "Jobs found (${jobsList.size})"

                            bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                                View.VISIBLE
                        } else {
                            // get progress bar and hide it after the jobs load.
                            bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                                View.GONE

                            backdropTitle.text = "Jobs found (${jobsList.size})"

                            bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                                View.GONE
                        }
                    } else {
                        // get progress bar and hide it after the jobs load.
                        bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                            View.INVISIBLE

                        // get error view and make it visible if the fetching fails
                        bottomSheetView.findViewById<TextView>(R.id.errorText).text =
                            "Error loading jobs."
                        bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                            View.VISIBLE
                    }

                    val rvAdapter = JobsRvAdapter(jobsList, parentFragment)
                    jobsRecyclerView.adapter = rvAdapter

                    backdropTitle.text = "Jobs found (${jobsList.size})"

                    bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                        View.GONE
                    rvAdapter.notifyDataSetChanged()

                    var saveColor = Color.rgb(3f, 218f, 198f)

                    //Save to swipe logic
                    val myCallback =
                        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
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
                                touchDown = false
                                rvAdapter.notifyItemChanged(viewHolder.adapterPosition)
                                savedItems.add(viewHolder.adapterPosition)

                                fun assSave() = CoroutineScope(Dispatchers.Main).launch {
                                    val save = async(Dispatchers.IO) {
                                        saveJob(jobsList[viewHolder.adapterPosition].jobId.toString())
                                    }
                                    val aSave = save.await()
                                    if (aSave != null) {
                                        jobsList[viewHolder.adapterPosition].setSaved()
                                    }
                                }
                                assSave()

                                if (!jobsList[viewHolder.adapterPosition].saved) {
                                    Toast.makeText(context, "Job saved", Toast.LENGTH_SHORT).show()
                                }
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
                                for (item in savedItems) {
                                    if (viewHolder.adapterPosition == item) {
                                        if (touchDown) {
                                            touchDown = false
                                        }
                                        return
                                    }
                                }
                                c.clipRect(
                                    15f, viewHolder.itemView.top.toFloat() + 20f,
                                    dX + 50f, viewHolder.itemView.bottom.toFloat() - 20f
                                )

                                //making the save thingy change color
                                if (218f + (dX / 5000) <= 255f && 198f + (dX / 5000) <= 255f) {
                                    saveColor =
                                        Color.rgb(3f, 218f + (dX / 5000), 198f + (dX / 5000))
                                }
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
                                    if (touchDown) {
                                        fun assSave() = CoroutineScope(Dispatchers.Main).launch {
                                            val save = async(Dispatchers.IO) {
                                                saveJob(jobsList[viewHolder.adapterPosition].jobId.toString())
                                            }
                                            val aSave = save.await()
                                            if (aSave != null) {
                                                jobsList[viewHolder.adapterPosition].setSaved()
                                            }
                                        }
                                        assSave()

                                        println("limit hit")
                                        if (!jobsList[viewHolder.adapterPosition].saved) {
                                            Toast.makeText(context, "Job saved", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                        touchDown = false
                                        rvAdapter.notifyItemChanged(viewHolder.adapterPosition)
                                        savedItems.add(viewHolder.adapterPosition)
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
                    myHelper.attachToRecyclerView(jobsRecyclerView)
                } else {
                    // get progress bar and hide it after the jobs load.
                    bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                        View.INVISIBLE

                    // get error view and make it visible if the fetching fails
                    bottomSheetView.findViewById<TextView>(R.id.errorText).text =
                        "Couldn't connect to endpoint"
                    bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                        View.VISIBLE

                    Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                }

            }

        bottomSheetView.findViewById<TextView>(R.id.errorText).text =
            "Loading jobs..."
        loadData(this)


        if (jobsList.isNotEmpty()) {
            // get progress bar and hide it after the jobs load.
            bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility = View.GONE

            backdropTitle.text = "Jobs found (${jobsList.size})"

            bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                View.GONE
        }

        val rvAdapter = JobsRvAdapter(jobsList, this)
        jobsRecyclerView.adapter = rvAdapter

        jobsRecyclerView.setOnTouchListener { _, event ->
            if (MotionEvent.ACTION_UP == event.action) {
                touchDown = false
            }
            if (MotionEvent.ACTION_DOWN == event.action) {
                touchDown = true
            }
            false // return is important...
        }

        val searchBox = jobsView.findViewById<EditText>(R.id.searchJobsBox)
        val locationBox = jobsView.findViewById<EditText>(R.id.locationSearchBox)

        val partTimeCheckbox = jobsView.findViewById<CheckBox>(R.id.partTimeCheckbox)
        val fullTimeCheckbox = jobsView.findViewById<CheckBox>(R.id.fullTimeCheckbox)

        partTimeCheckbox.setOnClickListener {
            partTimeParam = partTimeCheckbox.isChecked
        }

        fullTimeCheckbox.setOnClickListener {
            fullTimeParam = fullTimeCheckbox.isChecked
        }

        searchBox.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                searchParam = searchBox.text.toString()

                jobsList = mutableListOf()
                rvAdapter.notifyDataSetChanged()

                bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                    View.VISIBLE

                bottomSheetView.findViewById<TextView>(R.id.errorText).text =
                    "Loading jobs..."
                bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                    View.VISIBLE

                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                loadData(this)
                return@OnKeyListener true
            }
            false
        })
        searchBox.setOnTouchListener(OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (searchBox.right - searchBox.compoundDrawables[2].bounds.width())) {
                    // your action for drawable click event
                    if (locationBox.text.isNotBlank()) {
                        locationParam = locationBox.text.toString()
                    }

                    jobsList = mutableListOf()
                    rvAdapter.notifyDataSetChanged()

                    bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                        View.VISIBLE

                    bottomSheetView.findViewById<TextView>(R.id.errorText).text =
                        "Loading jobs..."
                    bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                        View.VISIBLE

                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                    loadData(this)
                    return@OnTouchListener true
                }
            }
            false
        })

        locationBox.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                locationParam = locationBox.text.toString()

                jobsList = mutableListOf()
                rvAdapter.notifyDataSetChanged()

                bottomSheetView.findViewById<ProgressBar>(R.id.jobsProgressBar).visibility =
                    View.VISIBLE

                bottomSheetView.findViewById<TextView>(R.id.errorText).text =
                    "Loading jobs..."
                bottomSheetView.findViewById<LinearLayout>(R.id.errorView).visibility =
                    View.VISIBLE

                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                loadData(this)
                return@OnKeyListener true
            }
            false
        })

        val distanceSlider = jobsView.findViewById<SeekBar>(R.id.distanceFromLocationSlider)
        val distanceSliderText = jobsView.findViewById<TextView>(R.id.distanceFromLocationText)

        distanceSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                distanceSliderText.text = "Distance from location: +${i * 10} miles"
                locationDistanceParam = i * 10
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
                // Toast.makeText(applicationContext,"start tracking",Toast.LENGTH_SHORT).show()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
                // Toast.makeText(applicationContext,"stop tracking",Toast.LENGTH_SHORT).show()
            }
        })

        val minSalarySlider = jobsView.findViewById<SeekBar>(R.id.minimumSalarySlider)
        val minSalarySliderText = jobsView.findViewById<TextView>(R.id.minimumSalaryText)
        minSalarySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                minSalarySliderText.text = "Minimum salary: £${i * 50}"
                minSalaryParam = i * 50
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
//                Toast.makeText(applicationContext,"start tracking",Toast.LENGTH_SHORT).show()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
//                Toast.makeText(applicationContext,"stop tracking",Toast.LENGTH_SHORT).show()
            }
        })

        val maxSalarySlider = jobsView.findViewById<SeekBar>(R.id.maximumSalarySlider)
        val maxSalarySliderText = jobsView.findViewById<TextView>(R.id.maximumSalaryText)
        maxSalarySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                maxSalarySliderText.text = "Maximum salary: £${i * 50}"
                maxSalaryParam = i * 50
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
//                Toast.makeText(applicationContext,"start tracking",Toast.LENGTH_SHORT).show()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
//                Toast.makeText(applicationContext,"stop tracking",Toast.LENGTH_SHORT).show()
            }
        })

        return jobsView
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment JobsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            JobsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}