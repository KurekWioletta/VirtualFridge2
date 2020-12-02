package com.example.virtualfridge.domain.calendar

import com.example.virtualfridge.data.api.EventsApi
import com.example.virtualfridge.data.api.FamilyApi
import com.example.virtualfridge.data.internal.UserDataStore
import com.example.virtualfridge.domain.calendar.CalendarFragment.FamilyMemberViewModel
import com.example.virtualfridge.domain.calendar.CalendarFragment.FamilyMemberViewModel.Companion.fromResponse
import com.example.virtualfridge.domain.calendar.events.EventViewModel
import com.example.virtualfridge.utils.ApiErrorParser
import com.example.virtualfridge.utils.RxTransformerManager
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CalendarFragmentPresenter @Inject constructor(
    private val view: CalendarFragment,
    private val eventsApi: EventsApi,
    private val familyApi: FamilyApi,
    private val userDataStore: UserDataStore,
    private val apiErrorParser: ApiErrorParser,
    private val rxTransformerManager: RxTransformerManager
) {
    private val events = mutableMapOf<LocalDate, List<EventViewModel>>()

    private val triggerRefresh = BehaviorSubject.createDefault<String>("")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val selectedDayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    fun init() = view.registerViewSubscription(
        Observable.combineLatest(
            familyApi.familyMembers(userDataStore.loggedInUser().id)
                .map { fromResponse(it) },
            triggerRefresh.flatMap { userId ->
                eventsApi.events(
                    if (userId.isEmpty()) {
                        userDataStore.loggedInUser().id
                    } else {
                        userId
                    }
                )
                    .map { EventViewModel.fromResponse(it) }
                    .compose { rxTransformerManager.applyIOScheduler(it) }
            },
            BiFunction { family: List<FamilyMemberViewModel>, events: List<EventViewModel> -> family to events }
        )
            .compose { rxTransformerManager.applyIOScheduler(it) }
            .doOnSubscribe { view.showLoading() }
            .doOnEach { view.hideLoading() }
            .subscribe({ (family, events) ->
                view.updateFamilyMembers(family)
                view.updateEventsOnCalendar(
                    mapOf(
                        view.currentDay.plusDays(1) to true,
                        view.currentDay.plusDays(2) to true
                    )
                )
            }, {
                view.showAlert(apiErrorParser.parse(it))
            })
    )

    fun triggerRefresh(userId: String) {
        triggerRefresh.onNext(userId)
    }

    fun monthChanged(yearMonth: YearMonth) = view.updateMonth(monthYearFormatter.format(yearMonth))

    fun dateSelected(date: LocalDate) {
        if (view.selectedDate != date) {
            view.updateDate(
                date, selectedDayFormatter.format(date)
            )
            view.updateEventsList(
                listOf(
                    EventViewModel(
                        "",
                        "titleNote2",
                        "contentNote2",
                        "placeNote2",
                        view.currentDay.toString(),
                        view.currentDay.toString()
                    ),
                    EventViewModel(
                        "",
                        "titleNote1",
                        "contentNote1",
                        "placeNote1",
                        view.currentDay.toString(),
                        view.currentDay.toString()
                    )
                )
            )
        }
    }

    fun editEvent(event: EventViewModel) {

    }

    fun deleteEvent(eventId: String) =
        view.registerViewSubscription(eventsApi.deleteEvent(eventId)
            .compose { rxTransformerManager.applyIOScheduler(it) }
            .doOnSubscribe { view.showLoading() }
            .doOnTerminate { view.hideLoading() }
            .subscribe({
                triggerRefresh(view.currentFamilyMemberId())
            }, {
                view.showAlert(apiErrorParser.parse(it))
            })
        )
}