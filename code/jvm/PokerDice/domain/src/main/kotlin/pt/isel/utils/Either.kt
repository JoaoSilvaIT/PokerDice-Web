package pt.isel.utils

sealed class Either<out F, out S> {
    data class Failure<out L>(
        val value: L,
    ) : Either<L, Nothing>()

    data class Success<out R>(
        val value: R,
    ) : Either<Nothing, R>()
}

fun <S> success(value: S) = Either.Success(value)

fun <F> failure(error: F) = Either.Failure(error)
