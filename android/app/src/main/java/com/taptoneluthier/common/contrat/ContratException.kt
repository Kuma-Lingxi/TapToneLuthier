package com.taptoneluthier.common.contrat

open class ContratException(
    val condition: String,
    val contractType: String,
    message: String = contractType
) : IllegalStateException(message){
    fun reqTexteException(): String {
        return buildString {
            appendLine("Message : $message")
            appendLine("Type    : $contractType")
            appendLine("Test    : $condition")
        }
    }
}

class AssertionException(condition: String) :
    ContratException(condition, "ERREUR D'ASSERTION")

class PreconditionException(condition: String) :
    ContratException(condition, "ERREUR DE PRECONDITION")

class PostconditionException(condition: String) :
    ContratException(condition, "ERREUR DE POSTCONDITION")

class InvariantException(condition: String) :
    ContratException(condition, "ERREUR D'INVARIANT")