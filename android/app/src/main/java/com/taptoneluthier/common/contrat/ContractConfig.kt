package com.taptoneluthier.common.contrat

object ContractConfig {
    const val ENABLED: Boolean = true
}

inline fun assertion(condition: Boolean, lazyExpr: () -> String) {
    if (ContractConfig.ENABLED && !condition) {
        throw AssertionException(lazyExpr())
    }
}

inline fun precondition(condition: Boolean, lazyExpr: () -> String) {
    if (ContractConfig.ENABLED && !condition) {
        throw PreconditionException(lazyExpr())
    }
}

inline fun postcondition(condition: Boolean, lazyExpr: () -> String) {
    if (ContractConfig.ENABLED && !condition) {
        throw PostconditionException(lazyExpr())
    }
}

inline fun invariant(condition: Boolean, lazyExpr: () -> String) {
    if (ContractConfig.ENABLED && !condition) {
        throw InvariantException(lazyExpr())
    }
}