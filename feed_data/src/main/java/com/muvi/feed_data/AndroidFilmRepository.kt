package com.muvi.feed_data

import com.muvi.feed_domain.Film
import com.muvi.feed_domain.FilmRepository
import com.muvi.remote.Clock

class AndroidFilmRepository internal constructor(private val letterboxdApi: LetterboxdApi) : FilmRepository {

    override suspend fun getFilms(): List<Film> {
        return letterboxdApi.films().let { filmsResponseModel ->
            filmsResponseModel.items.map { Film(it.id, it.name) }
        }
    }

    companion object {

        fun create(apiKey: String, apiSecret: String): AndroidFilmRepository {
            val clock = object : Clock {
                override fun currentTimeMillis(): Long = System.currentTimeMillis()
            }
            val api = LetterboxdApiFactory(apiKey, apiSecret, clock).remoteLetterboxdApi()
            return AndroidFilmRepository(api)
        }
    }
}
