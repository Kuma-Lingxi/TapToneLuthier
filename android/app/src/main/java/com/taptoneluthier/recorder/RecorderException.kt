package com.taptoneluthier.recorder

/**
 * Exception spécifique au module d'enregistrement audio.
 */
class RecorderException (
    message : String,
    cause : Throwable? = null
): Exception(message,cause)