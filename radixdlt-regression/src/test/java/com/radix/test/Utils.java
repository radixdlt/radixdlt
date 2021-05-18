package com.radix.test;

import com.radix.test.account.Account;
import com.radixdlt.atommodel.tokens.TokenDefinitionUtils;
import com.radixdlt.client.lib.api.ValidatorAddress;
import com.radixdlt.client.lib.dto.ValidatorDTO;
import com.radixdlt.serialization.DeserializeException;
import com.radixdlt.utils.UInt256;
import com.radixdlt.utils.functional.Failure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Durations;
import org.awaitility.core.ConditionTimeoutException;

import static org.awaitility.Awaitility.await;

public final class Utils {

    private static final Logger logger = LogManager.getLogger();

    private Utils() {

    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Will wait until the native token balance increased by the given amount
     */
    public static void waitForBalanceToIncreaseBy(Account account, UInt256 amount) {
        UInt256 initialAmount = account.getOwnNativeTokenBalance().getAmount();
        UInt256 targetAmount = initialAmount.add(amount);
        try {
            await().atMost(Durations.ONE_MINUTE).until(() -> initialAmount.compareTo(targetAmount) >= 0);
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException("Account's balance did not increase");
        }
    }

    /**
     * Will wait until the native token balance reaches the given amount
     */
    public static void waitForBalanceToReach(Account account, UInt256 amount) {
        try {
            await().atMost(Durations.ONE_MINUTE).until(() ->
                    account.getOwnNativeTokenBalance().getAmount().compareTo(amount) >= 0);
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException("Account's balance did not reach " + amount);
        }
    }

    /**
     * Waits until the account's native balance decreases by any amount
     */
    public static void waitForBalanceToDecrease(Account account) {
        UInt256 initialAmount = account.getOwnNativeTokenBalance().getAmount();
        try {
            await().atMost(Durations.ONE_MINUTE).until(() ->
                account.getOwnNativeTokenBalance().getAmount().compareTo(initialAmount) < 0);
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException("Account's balance did not decreaase");
        }
    }

    public static UInt256 fromMinorToMajor(UInt256 minorAmount) {
        return minorAmount.divide(TokenDefinitionUtils.SUB_UNITS);
    }

    public static <R> R toRuntimeException(Failure failure) {
        throw new RuntimeException(failure.message());
    }

    public static UInt256 fromMajorToMinor(int amountMajor) {
        return UInt256.from(amountMajor).multiply(TokenDefinitionUtils.SUB_UNITS);
    }

    public static UInt256 fromMajorToMinor(long amountMajor) {
        return UInt256.from(amountMajor).multiply(TokenDefinitionUtils.SUB_UNITS);
    }

    public static UInt256 fromMajorToMinor(UInt256 amountMajor) {
        return amountMajor.multiply(TokenDefinitionUtils.SUB_UNITS);
    }

    public static ValidatorAddress createValidatorAddress(ValidatorDTO validatorDTO) {
        try {
            return ValidatorAddress.create(validatorDTO.getAddress());
        } catch (DeserializeException e) {
            throw new RuntimeException("Failed to parse validator address: " + validatorDTO.getAddress());
        }
    }
}
