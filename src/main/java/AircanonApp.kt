import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val myScope = CoroutineScope(Dispatchers.Default + Job())

fun main() {
    runBlocking {
        doSomething()
    }
}

suspend fun doSomething() {
    println("Before")
    val job = myScope.launch {

        //Concurrent
//        println("Executing launchNetworkCallBySwitchingThreadAsync")
//        val deferred = launchNetworkCallBySwitchingThreadAsync()
//        deferred.await()

        //Suspended, will wait to return result b4 next line executes
        //Not able to cancel call in the middle
//        println("Executing launchNetworkCallNonCancellable")
//        launchNetworkCallNonCancellable()

        //Suspended, will wait to return result b4 next line executes
        //Able to cancel call in the middle
        println("Executing launchNetworkCallCancellable")
        launchNetworkCallCancellable()
    }

    delay(1000)
     println("Cancelling suspended coroutines")
     job.cancel()
    job.join()
    println("After")
}

suspend fun launchNetworkCallNonCancellable() {
    println("In launchNetworkCall")

    try{
        //Invoke retrofit's suspend function
        networkInterface.fetchFromNetwork()
            .filter {
                it.status == "online"
            }
            .forEach {
                println("Printing in launchNetworkCall ${it.name}: ${it.date}")
            }
    }catch (e: CancellationException){
        println("Cancellation exception occured")
    }finally {
        println("Before delay of 1 sec")

        //Since job is cancelled, suspension at delay(1000) wont happen and println("After.. wont be executed
        //So we use withContext(NonCancellable) to force delay
        withContext(NonCancellable){
            delay(1000)
            println("After delay of 1 sec")
        }
    }
}

fun launchNetworkCallBySwitchingThreadAsync() =

    myScope.async(Dispatchers.IO) {
        println("In launchNetworkCallBySwitchingThread")

        val response = networkInterface.fetchFromNetworkNonSuspend().execute()
        response.body()?.filter {
            it.status == "online"
        }
            ?.forEach {
                println("Printing in launchNetworkCallBySwitchingThread ${it.name}: ${it.date}")
            }
        println("Done with launchNetworkCallBySwitchingThread")
    }

suspend fun launchNetworkCallCancellable() {
    println("In launchNetworkCallCancellable")

    val response = networkInterface.fetchFromNetworkNonSuspend().await()
    response.filter {
        it.status == "online"
    }
        .forEach {
            println("Printing in launchNetworkCallCancellable ${it.name}: ${it.date}")
        }


    println("Done with launchNetworkCallCancellable")
}

//Extend retrofit Call and add a wrapper to cancel and consume coroutine [Similar to Observable.create]
suspend fun <T : Any> Call<T>.await(): T {
    return suspendCancellableCoroutine { cancellableContinuation ->
        cancellableContinuation.invokeOnCancellation {
            println("Cancelling......")
            cancel()
        }

        enqueue(object : Callback<T> {
            override fun onFailure(call: Call<T>, t: Throwable) {
                cancellableContinuation.resumeWithException(t)
            }

            override fun onResponse(call: Call<T>, response: Response<T>) {
                cancellableContinuation.resume(response.body() as T)
            }

        })
    }
}


//We arent switching any thread with implementation
//Call stack gets suspended while retrofit makes call
