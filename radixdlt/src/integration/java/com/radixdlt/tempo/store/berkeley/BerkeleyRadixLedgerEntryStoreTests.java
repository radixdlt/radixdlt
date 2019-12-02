package com.radixdlt.tempo.store.berkeley;

import com.google.common.collect.ImmutableSet;
import com.radixdlt.crypto.CryptoException;
import com.radixdlt.crypto.ECKeyPair;
import com.radixdlt.ledger.LedgerIndex;
import com.radixdlt.ledger.LedgerSearchMode;
import com.radixdlt.serialization.Serialization;
import com.radixdlt.ledger.LedgerEntry;
import com.radixdlt.tempo.LedgerEntryGenerator;
import com.radixdlt.tempo.Tempo;
import com.radixdlt.tempo.store.LedgerEntryStatus;
import com.radixdlt.utils.Ints;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.radix.database.DatabaseEnvironment;
import org.radix.exceptions.ValidationException;
import org.radix.integration.RadixTestWithStores;
import org.radix.logging.Logger;
import org.radix.logging.Logging;
import org.radix.modules.Modules;
import org.radix.universe.system.LocalSystem;
import org.radix.utils.SystemProfiler;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assume.assumeTrue;

public class BerkeleyRadixLedgerEntryStoreTests extends RadixTestWithStores {

    private static final Logger LOGGER = Logging.getLogger("BerkeleyTempoAtomStoreTests");

    private LedgerEntryGenerator ledgerEntryGenerator = new LedgerEntryGenerator();
    private LocalSystem localSystem = LocalSystem.getInstance();
    private Serialization serialization = Serialization.getDefault();
    private SystemProfiler profiler = SystemProfiler.getInstance();
    private BerkeleyLedgerEntryStore tempoAtomStore;

    private List<LedgerEntry> ledgerEntries;

    private ECKeyPair identity;

	@BeforeClass
	public static void checkForTempo() {
		assumeTrue(Modules.isAvailable(Tempo.class)); // Otherwise databases are not reset, and key conflicts occur and tests fail
	}

    @Before
    public void setup() throws CryptoException, ValidationException {
        tempoAtomStore = new BerkeleyLedgerEntryStore(localSystem.getNID(), serialization, profiler, Modules.get(DatabaseEnvironment.class));

        identity = new ECKeyPair();
        ledgerEntries = ledgerEntryGenerator.createLedgerEntries(identity, 5);
    }

    @After
    public void teardown() {
    	if (tempoAtomStore != null) {
    		tempoAtomStore.close();
    	}
    }

    @Test
    public void storePendingCommitTest() {
        SoftAssertions.assertSoftly(softly -> {
            //atom added to store successfully
            softly.assertThat(tempoAtomStore.store(ledgerEntries.get(0), ImmutableSet.of(), ImmutableSet.of()).isSuccess()).isTrue();

            //atom added to store is pending
            softly.assertThat(tempoAtomStore.getStatus(ledgerEntries.get(0).getAID())).isEqualTo(LedgerEntryStatus.PENDING);

            // atom added to store should be in pending
            softly.assertThat(tempoAtomStore.getPending()).containsExactly(ledgerEntries.get(0).getAID());

            //added atom is present in store
            softly.assertThat(tempoAtomStore.contains(ledgerEntries.get(0).getAID())).isTrue();

            // commit atom
            tempoAtomStore.commit(ledgerEntries.get(0).getAID());

            // committed atom is committed
            softly.assertThat(tempoAtomStore.getStatus(ledgerEntries.get(0).getAID())).isEqualTo(LedgerEntryStatus.COMMITTED);

            // committed atom is not in pending
            softly.assertThat(tempoAtomStore.getPending()).doesNotContain(ledgerEntries.get(0).getAID());

            //not added atom is absent in store
            softly.assertThat(tempoAtomStore.contains(ledgerEntries.get(1).getAID())).isFalse();
        });
    }

    @Test
    public void storeContainsTest() {
        SoftAssertions.assertSoftly(softly -> {
            //atom added to store successfully
            softly.assertThat(tempoAtomStore.store(ledgerEntries.get(0), ImmutableSet.of(), ImmutableSet.of()).isSuccess()).isTrue();

            //added atom is present in store
            softly.assertThat(tempoAtomStore.contains(ledgerEntries.get(0).getAID())).isTrue();

            //not added atom is absent in store
            softly.assertThat(tempoAtomStore.contains(ledgerEntries.get(1).getAID())).isFalse();
        });
    }

    @Test
    public void storeGetTest() {
        SoftAssertions.assertSoftly(softly -> {
            //atom added to store successfully
            softly.assertThat(tempoAtomStore.store(ledgerEntries.get(0), ImmutableSet.of(), ImmutableSet.of()).isSuccess()).isTrue();

            //added atom is present in store
            softly.assertThat(tempoAtomStore.get(ledgerEntries.get(0).getAID()).get()).isEqualTo(ledgerEntries.get(0));

            //not added atom is absent in store
            softly.assertThat(tempoAtomStore.get(ledgerEntries.get(1).getAID()).isPresent()).isFalse();
        });
    }

    @Test
    public void storeGetReplaceTest() {
        SoftAssertions.assertSoftly(softly -> {
            //atom added to store successfully
            softly.assertThat(tempoAtomStore.store(ledgerEntries.get(0), ImmutableSet.of(), ImmutableSet.of()).isSuccess()).isTrue();

            //added atom is present in store
            softly.assertThat(tempoAtomStore.get(ledgerEntries.get(0).getAID()).isPresent()).isTrue();

            //not added atom is absent in store
            softly.assertThat(tempoAtomStore.get(ledgerEntries.get(1).getAID()).isPresent()).isFalse();

            //atom replaced successfully
            softly.assertThat(tempoAtomStore.replace(ImmutableSet.of(ledgerEntries.get(0).getAID()), ledgerEntries.get(1), ImmutableSet.of(), ImmutableSet.of()).isSuccess()).isTrue();

            //replaced atom gone
            softly.assertThat(tempoAtomStore.get(ledgerEntries.get(0).getAID()).isPresent()).isFalse();

            //new atom is present in store
            softly.assertThat(tempoAtomStore.get(ledgerEntries.get(1).getAID()).isPresent()).isTrue();
        });
    }

    @Test
    public void searchDuplicateExactTest() {
        storeAndCommitAtoms();
        // LedgerIndex for shard 200
        LedgerIndex ledgerIndex = new LedgerIndex((byte) 200, Ints.toByteArray(200));
        validateShard200(() -> (BerkeleyCursor) tempoAtomStore.search(LedgerIndex.LedgerIndexType.DUPLICATE, ledgerIndex, LedgerSearchMode.EXACT));
    }

    @Test
    public void searchDuplicateRangeTest() {
        storeAndCommitAtoms();
        LedgerIndex ledgerIndex = new LedgerIndex((byte) 200, Ints.toByteArray(150));
        // LedgerIndex pointing to not existing shard 150. But because ofLedgerSearchMode.RANGE Cursor will point it to next available shard - shard 200
        validateShard200(() -> (BerkeleyCursor) tempoAtomStore.search(LedgerIndex.LedgerIndexType.DUPLICATE, ledgerIndex, LedgerSearchMode.RANGE));
    }

    @Test
    public void searchUniqueExactTest() {
        storeAndCommitAtoms();
        SoftAssertions.assertSoftly(softly -> {
            // LedgerIndex for Atom 3
            LedgerIndex ledgerIndex = new LedgerIndex(LedgerEntryIndices.ENTRY_INDEX_PREFIX, ledgerEntries.get(3).getAID().getBytes());

            BerkeleyCursor tempoCursor = (BerkeleyCursor) tempoAtomStore.search(LedgerIndex.LedgerIndexType.UNIQUE, ledgerIndex, LedgerSearchMode.EXACT);
            //Cursor pointing to unique single result.
            //getFirst and getLast pointing to the same value
            //getNext and getPrev are not available
            softly.assertThat(tempoCursor.get()).isEqualTo(ledgerEntries.get(3).getAID());
            softly.assertThat((tempoCursor = tempoAtomStore.getFirst(tempoCursor)).get()).isEqualTo(ledgerEntries.get(3).getAID());
            softly.assertThat((tempoCursor = tempoAtomStore.getLast(tempoCursor)).get()).isEqualTo(ledgerEntries.get(3).getAID());
            softly.assertThat((tempoAtomStore.getNext(tempoCursor))).isNull();
            softly.assertThat((tempoAtomStore.getPrev(tempoCursor))).isNull();
        });
    }

    /**
     * Method validating navigation when shard200Supplier returning BerkeleyCursor which pointing to "Shard 200" which contains TempoAtoms(2,3,4)
     *
     * @param shard200Supplier function which return BerkeleyCursor to "shard 200"
     */
    private void validateShard200(Supplier<BerkeleyCursor> shard200Supplier) {
        SoftAssertions.assertSoftly(softly -> {
            BerkeleyCursor tempoCursor = shard200Supplier.get();
            //Navigation in scope of shard 200 => (2,3,4)
            //Pointing Atom[2] - first element in shard
            softly.assertThat(tempoCursor.get()).isEqualTo(ledgerEntries.get(2).getAID());
            //Atom[2] getNext -> cursor pointing to Atom[3] - second element in shard
            softly.assertThat((tempoCursor = tempoAtomStore.getNext(tempoCursor)).get()).isEqualTo(ledgerEntries.get(3).getAID());

            //Atom[3] getNext -> cursor pointing to Atom[4] - third element in shard
            softly.assertThat((tempoCursor = tempoAtomStore.getNext(tempoCursor)).get()).isEqualTo(ledgerEntries.get(4).getAID());

            //Atom[4] getFirst -> cursor pointing to Atom[2] - first element in shard
            softly.assertThat((tempoCursor = tempoAtomStore.getFirst(tempoCursor)).get()).isEqualTo(ledgerEntries.get(2).getAID());

            //Atom[2] getPrev -> cursor is null, no previous element for first element. Cursor is not saved, tempoCursor still pointing to Atom[2] - first element
            softly.assertThat((tempoAtomStore.getPrev(tempoCursor))).isNull();

            //Atom[2] getLast -> cursor pointing to Atom[4] - last element in shard
            softly.assertThat((tempoCursor = tempoAtomStore.getLast(tempoCursor)).get()).isEqualTo(ledgerEntries.get(4).getAID());

            //Atom[4] getNext -> cursor is null, no next element for last element. Cursor is not saved, tempoCursor still pointing to Atom[4] - last element
            softly.assertThat((tempoAtomStore.getNext(tempoCursor))).isNull();

            //Atom[4] getPrev -> cursor pointing to Atom[3] - element before last one
            softly.assertThat((tempoCursor = tempoAtomStore.getPrev(tempoCursor)).get()).isEqualTo(ledgerEntries.get(3).getAID());
        });
    }

    /**
     * Method for storing and committing atoms in atomStore with sharding
     * Atoms are committed because some tests rely on them being ordered, which is currently only guaranteed for committed atoms
     * Shard 100 -> (0,1)
     * Shard 200 -> (2,3,4)
     */
    private void storeAndCommitAtoms() {
        SoftAssertions.assertSoftly(softly -> {
            for (int i = 0; i < ledgerEntries.size(); i++) {
                int shard = i < ledgerEntries.size() / 2 ? 100 : 200;
                LedgerIndex ledgerIndex = new LedgerIndex((byte) 200, Ints.toByteArray(shard));
                softly.assertThat(tempoAtomStore.store(ledgerEntries.get(i), ImmutableSet.of(), ImmutableSet.of(ledgerIndex)).isSuccess()).isTrue();
                tempoAtomStore.commit(ledgerEntries.get(i).getAID());
            }
        });
    }

}
