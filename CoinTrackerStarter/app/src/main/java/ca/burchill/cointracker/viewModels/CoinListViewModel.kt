package ca.burchill.cointracker.viewModels

import android.app.Application
import androidx.lifecycle.*
import ca.burchill.cointracker.database.getDatabase
import ca.burchill.cointracker.network.CoinApi
import ca.burchill.cointracker.network.CoinApiResponse
import ca.burchill.cointracker.network.NetworkCoin
import ca.burchill.cointracker.repository.CoinsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.await
import java.io.IOException


enum class CoinApiStatus { LOADING, ERROR, DONE }


class CoinListViewModel(application: Application) : ViewModel() {

    private val coinsRepository = CoinsRepository(getDatabase(application))

    // The internal MutableLiveData that stores the status of the most recent request
    private val _status = MutableLiveData<CoinApiStatus>()
    val status: LiveData<CoinApiStatus>
        get() = _status


    private val _coins = MutableLiveData<List<NetworkCoin>>()
    val coins: LiveData<List<NetworkCoin>>
        get() = _coins


    /**
     * A playlist of videos displayed on the screen.
     */
    val coinList = coinsRepository.coins

    /**
     * Event triggered for network error. This is private to avoid exposing a
     * way to set this value to observers.
     */
    private var _eventNetworkError = MutableLiveData<Boolean>(false)

    /**
     * Event triggered for network error. Views should use this to get access
     * to the data.
     */
    val eventNetworkError: LiveData<Boolean>
        get() = _eventNetworkError

    /**
     * Flag to display the error message. This is private to avoid exposing a
     * way to set this value to observers.
     */
    private var _isNetworkErrorShown = MutableLiveData<Boolean>(false)

    /**
     * Flag to display the error message. Views should use this to get access
     * to the data.
     */
    val isNetworkErrorShown: LiveData<Boolean>
        get() = _isNetworkErrorShown


    // or use viewModelScope
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)


    init {
        getCoins()
    }

    private fun getCoins() {

       coroutineScope.launch {
            try {
                var coinResult = CoinApi.retrofitService.getCoins()
                if (coinResult.coins.size > 0) {
                    _coins.value = coinResult.coins
                }
            } catch (t: Throwable) {
               _status.value = CoinApiStatus.ERROR
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private fun refreshDataFromRepository() {
        viewModelScope.launch {
            try {
                coinsRepository.refreshCoins()
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false

            } catch (networkError: IOException) {
                // Show a Toast error message and hide the progress bar.
                if(coinList.value.isNullOrEmpty())
                    _eventNetworkError.value = true
            }
        }
    }
}