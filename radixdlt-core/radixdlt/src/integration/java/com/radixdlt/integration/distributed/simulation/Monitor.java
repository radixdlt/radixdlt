package com.radixdlt.integration.distributed.simulation;

public enum Monitor {
    SAFETY,
    LIVENESS,
    NO_TIMEOUTS,
    DIRECT_PARENTS,
    NONE_COMMITTED,
    CONSENSUS_TO_LEDGER_PROCESSED,
    EPOCH_CEILING_VIEW,
    LEDGER_IN_ORDER,
    TIMESTAMP_CHECK,
    MEMPOOL_COMMITTED,
    VERTEX_REQUEST_RATE,
    VALIDATOR_REGISTERED
}
