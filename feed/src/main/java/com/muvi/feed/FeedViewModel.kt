package com.muvi.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muvi.base_domain.FilmSummary
import com.muvi.feed_domain.GetFilmsUseCase
import com.muvi.navigation.EventWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class FeedViewModel(private val getFilms: GetFilmsUseCase) : ViewModel() {

    private val _films = MutableLiveData<List<FilmSummaryUiModel>>()
    val films: LiveData<List<FilmSummaryUiModel>> = _films

    private val _events = MutableLiveData<EventWrapper<Event>>()
    val events: LiveData<EventWrapper<Event>> = _events

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val uiModels = getFilms().map {
                FilmSummaryUiModel(it.id, it.title) {
                    _events.value = EventWrapper(Event.NavigateToFilmDetail(it.id))
                }
            }
            withContext(Dispatchers.Main) {
                _films.value = uiModels
            }
        }
    }

    sealed class Event {
        data class NavigateToFilmDetail(val filmId: String) : Event()
    }
}

data class FilmSummaryUiModel(
        val id: String,
        val title: String,
        val onClick: () -> Unit
)