package pt.isel.utils

sealed class Either<out F, out S> {
    data class Failure<out F>(
        val value: F,
    ) : Either<F, Nothing>()

    data class Success<out S>(
        val value: S,
    ) : Either<Nothing, S>()
}

fun <S> success(value: S) = Either.Success(value)

fun <F> failure(error: F) = Either.Failure(error)
