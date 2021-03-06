package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.storage.FullTransaction

/**
 * Enum class used for deciding to accept or ignore transaction after conflict resolution.
 */
sealed class ConflictResolution {
    class Ignore(val needToUpdate: List<Transaction>) : ConflictResolution()
    class Accept(val needToMakeInvalid: List<Transaction>) : ConflictResolution()
}

/**
 * Class for resolving general transaction conflicts. This initial implementation considers only explicit double spends,
 * meaning that for some transaction there is another transaction that spends one of its inputs.
 * In the future, can be added methods for risk analysis, eg. when there is a possibility of double spend when RBF is opt-in (BIP-125).
 */
class TransactionMediator {

    /**
     * Method containing logic for deciding to accept or ignore a transaction, received from network, which might have conflicting transactions that spend one of its inputs.
     * If received transaction is already in block, return ConflictResolution.ACCEPT and set field conflictingTxHash of conflicting transactions to its hash.
     * If any of the conflicting transactions is in block, return ConflictResolution.IGNORE and set field conflictingTxHash of conflicting transactions to NULL.
     * Also just return ConflictResolution.ACCEPT when there is no conflicting transactions.
     * @param receivedTransaction transaction that is received from network
     * @param conflictingTransactions transactions that spend one of receivedTransaction's inputs
     * @return conflict resolution type {@link ConflictResolution}
     * @see ConflictResolution
     */
    fun resolveConflicts(receivedTransaction: FullTransaction, conflictingTransactions: List<Transaction>): ConflictResolution {
        if (receivedTransaction.header.blockHash != null || conflictingTransactions.isEmpty()) {
            return ConflictResolution.Accept(conflictingTransactions)
        }

        val updateTransactions = mutableListOf<Transaction>()
        val conflictingTxHash = if (conflictingTransactions.any { it.blockHash != null }) {
            null
        } else {
            receivedTransaction.header.hash
        }

        conflictingTransactions.forEach {
            if (it.conflictingTxHash == null && conflictingTxHash != null) {
                it.conflictingTxHash = conflictingTxHash
                updateTransactions.add(it)
            }
        }

        return ConflictResolution.Ignore(updateTransactions)
    }
}
