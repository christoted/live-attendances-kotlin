package com.example.liveattendances.views.fragment

import android.opengl.Visibility
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.applandeo.materialcalendarview.utils.calendar
import com.applandeo.materialcalendarview.utils.midnightCalendar
import com.example.liveattendances.MyDate.fromTimeStampToDate
import com.example.liveattendances.MyDate.toCalendar
import com.example.liveattendances.MyDate.toDate
import com.example.liveattendances.MyDate.toDay
import com.example.liveattendances.MyDate.toMonth
import com.example.liveattendances.MyDate.toTime
import com.example.liveattendances.R
import com.example.liveattendances.databinding.FragmentHistoryBinding
import com.example.liveattendances.dialog.MyDialog
import com.example.liveattendances.hawkstorage.HawkStorage
import com.example.liveattendances.model.History
import com.example.liveattendances.model.HistoryResponse
import com.example.liveattendances.networking.ApiServices
import com.example.liveattendances.networking.LiveAttendanceApiServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
class HistoryFragment : Fragment() {


    private var binding: FragmentHistoryBinding? = null

    companion object {
        private val TAG: String = HistoryFragment::class.java.simpleName
    }

    private val events = mutableListOf<EventDay>()
    private var dataHistories: List<History?>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        //Request Data History
        requestDataHistory()
        //Setup Calender Swipe
        setupCalender()
        //OnClick
        onClick()
    }

    private fun onClick() {
        binding?.calenderViewHistory?.setOnDayClickListener( object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                val clickedDayCalendar = eventDay.calendar
                binding?.tvCurrentDate?.text = clickedDayCalendar.toDate().toDay()
                binding?.tvCurrentMonth?.text = clickedDayCalendar.toDate().toMonth()

                if ( dataHistories != null) {
                    for ( dataHistory in dataHistories!!) {
                        val checkInTime: String
                        val checkOutTime: String
                        val updateDate = dataHistory?.updatedAt
                        val calenderUpdated = updateDate?.fromTimeStampToDate()?.toCalendar()

                        if ( clickedDayCalendar.get(Calendar.DAY_OF_MONTH) == calenderUpdated?.get(Calendar.DAY_OF_MONTH)) {
                            if ( dataHistory.status == 1) {
                                checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                checkOutTime = dataHistory.detail?.get(1)?.createdAt.toString()

                                binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                binding?.tvTimeCheckOut?.text = checkOutTime.fromTimeStampToDate()?.toTime()
                                break
                            } else {
                                checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                break
                            }
                        } else {

                            binding?.tvTimeCheckIn?.text = "-"
                            binding?.tvTimeCheckOut?.text = "-"
                        }
                    }
                }
            }

        })
    }

    private fun setupCalender() {
        binding?.calenderViewHistory?.setOnPreviousPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                requestDataHistory()
            }
        })

        binding?.calenderViewHistory?.setOnForwardPageChangeListener(object : OnCalendarPageChangeListener {
            override fun onChange() {
                requestDataHistory()
            }
        })

    }

    private fun requestDataHistory() {
        val midnightCalendar = binding?.calenderViewHistory?.currentPageDate
        val lastDay = midnightCalendar?.getActualMaximum(Calendar.DAY_OF_MONTH)
        val month = midnightCalendar?.get(Calendar.MONTH)?.plus(1)
        val year = midnightCalendar?.get(Calendar.YEAR)

        val fromDate ="$year-$month-1"
        val toDate = "$year-$month-$lastDay"

        getDataHistory(fromDate, toDate)
    }

    private fun getDataHistory(fromDate: String, toDate: String) {
        val token = HawkStorage.instance(context).getToken()
        binding?.pbHistory?.visibility = View.VISIBLE
        ApiServices.getLiveAttendanceServices().getHistoryAttendance(token, fromDate, toDate)
            .enqueue(object : Callback<HistoryResponse> {
                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {

                }

                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                    binding?.pbHistory?.visibility = View.GONE
                    dataHistories = response.body()?.histories

                    if ( dataHistories != null) {
                        for ( dataHistory in dataHistories!!) {
                            val status = dataHistory?.status
                            val checkInTime: String
                            val checkOutTime: String
                            val calendarHistoryCheckIn: Calendar?
                            val calendarHistoryCheckOut: Calendar?
                            val currentDate = Calendar.getInstance()

                            if (status == 1 ) {
                                checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                checkOutTime = dataHistory.detail?.get(1)?.createdAt.toString()


                                calendarHistoryCheckOut = checkOutTime.fromTimeStampToDate()?.toCalendar()

                                if ( calendarHistoryCheckOut != null) {
                                    events.add(EventDay(calendarHistoryCheckOut, R.drawable.ic_baseline_check_circle_primary_24))
                                }

                                if (currentDate.get(Calendar.DAY_OF_MONTH) == calendarHistoryCheckOut?.get(Calendar.DAY_OF_MONTH)) {
                                    binding?.tvCurrentDate?.text = checkInTime.fromTimeStampToDate()?.toDay()
                                    binding?.tvCurrentMonth?.text = checkInTime.fromTimeStampToDate()?.toMonth()
                                    binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                    binding?.tvTimeCheckOut?.text = checkOutTime.fromTimeStampToDate()?.toTime()
                                }
                            } else {
                                checkInTime = dataHistory?.detail?.get(0)?.createdAt.toString()
                                calendarHistoryCheckIn = checkInTime.fromTimeStampToDate()?.toCalendar()

                                if (calendarHistoryCheckIn != null){
                                    events.add(EventDay(calendarHistoryCheckIn, R.drawable.ic_baseline_check_circle_yellow_light_24))
                                }

                                if (currentDate.get(Calendar.DAY_OF_MONTH) == calendarHistoryCheckIn?.get(Calendar.DAY_OF_MONTH)){
                                    binding?.tvCurrentDate?.text = checkInTime.fromTimeStampToDate()?.toDay()
                                    binding?.tvCurrentMonth?.text = checkInTime.fromTimeStampToDate()?.toMonth()
                                    binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                }
                            }
                        }

                        binding?.calenderViewHistory?.setEvents(events)
                    } else {
                        MyDialog.dynamicDialog(context, "Error", "Error Bro")
                    }


                }

            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


}