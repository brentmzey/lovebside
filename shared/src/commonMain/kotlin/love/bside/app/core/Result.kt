package love.bside.app.core

/**
 * A generic sealed class to represent the result of an operation.
 * Provides type-safe error handling and eliminates the need for exceptions in business logic.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get value from Loading state")
    }

    fun getOrElse(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(exception)
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppException) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }
}

/**
 * Executes a block and wraps the result in a Result type.
 */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: AppException) {
    Result.Error(e)
} catch (e: Exception) {
    Result.Error(AppException.Unknown(e.message ?: "Unknown error", e))
}

/**
 * Converts a nullable value to a Result.
 */
fun <T> T?.toResult(errorMessage: String = "Value is null"): Result<T> = when (this) {
    null -> Result.Error(AppException.Unknown(errorMessage))
    else -> Result.Success(this)
}

/**
 * Fold operation for Result - similar to Kotlin's Result.fold()
 */
inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppException) -> R
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onFailure(exception)
    is Result.Loading -> onFailure(AppException.Unknown("Cannot fold Loading state"))
}
