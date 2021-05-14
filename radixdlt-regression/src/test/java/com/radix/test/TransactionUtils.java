package com.radix.test;

import com.radix.test.account.Account;
import com.radixdlt.client.lib.api.AccountAddress;
import com.radixdlt.client.lib.api.TransactionRequest;
import com.radixdlt.client.lib.api.ValidatorAddress;
import com.radixdlt.client.lib.dto.FinalizedTransaction;
import com.radixdlt.client.lib.dto.TxDTO;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.utils.UInt256;
import com.radixdlt.utils.functional.Result;

public final class TransactionUtils {

    private TransactionUtils() {

    }

    public static TransactionRequest createTransferRequest(AccountAddress from, AccountAddress to, String tokenRri, UInt256 amount,
                                                           String message) {
        return TransactionRequest.createBuilder()
                .transfer(from, to, amount, tokenRri)
                .message(message)
                .build();
    }

    public static TransactionRequest createStakingRequest(AccountAddress from, ValidatorAddress to, UInt256 stake) {
        return TransactionRequest.createBuilder()
                .stake(from, to, stake)
                .build();
    }

    public static Result<TxDTO> performNativeTokenTransfer(Account sender, Account receiver, int amount) {
        var request = TransactionUtils.createTransferRequest(sender.getAddress(), receiver.getAddress(),
                sender.getNativeToken().getRri(), Utils.fromMajorToMinor(amount), "");
        return performTransaction(sender, request);
    }

    public static Result<TxDTO> performNativeTokenTransfer(Account sender, Account receiver, int amount, String message) {
        var request = TransactionUtils.createTransferRequest(sender.getAddress(), receiver.getAddress(),
                sender.getNativeToken().getRri(), Utils.fromMajorToMinor(amount), message);
        return performTransaction(sender, request);
    }

    public static Result<TxDTO> performNativeTokenTransfer(Account sender, Account receiver, UInt256 amount, String message) {
        var request = TransactionUtils.createTransferRequest(sender.getAddress(), receiver.getAddress(),
                sender.getNativeToken().getRri(), amount, message);
        return performTransaction(sender, request);
    }

    public static Result<TxDTO> performTransaction(Account account, TransactionRequest request) {
        ECKeyPair keyPair = account.getKeyPair();
        return account.buildTransaction(request).flatMap(builtTransactionDTO -> {
            FinalizedTransaction finalizedTransaction = builtTransactionDTO.toFinalized(keyPair);
            return account.finalizeTransaction(finalizedTransaction)
                    .flatMap(finalTxTdo -> account.submitTransaction(finalizedTransaction.withTxId(finalTxTdo.getTxId())));
        }).onFailure(Utils::toRuntimeException);
    }

}
