package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.state.ValidatorRegistryUpdater;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;

/**
 * Produces initial beacon state.
 *
 * <p>Requires input {@code block} to be a Genesis block, {@code state} parameter is ignored.
 * Preferred input for {@code state} parameter is EMPTY.
 *
 * <p>Uses {@link DepositContract} to fetch registration data from the PoW chain.
 *
 * @see DepositContract
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#on-startup">On
 *     startup in the spec</a>
 */
public class InitialStateTransition implements StateTransition<BeaconState> {

  private DepositContract depositContract;
  private ChainSpec chainSpec;

  public InitialStateTransition(DepositContract depositContract, ChainSpec chainSpec) {
    this.depositContract = depositContract;
    this.chainSpec = chainSpec;
  }

  @Override
  public BeaconState apply(BeaconBlock block, BeaconState state) {
    assert block.getSlot() == chainSpec.getGenesisSlot();

    ChainStart chainStart = depositContract.getChainStart();

    MutableBeaconState initialState = MutableBeaconState.createNew();

    // Misc
    initialState
        .withSlot(chainSpec.getGenesisSlot())
        .withGenesisTime(chainStart.getTime())
        .withForkData(
            new ForkData(
                chainSpec.getGenesisForkVersion(),
                chainSpec.getGenesisForkVersion(),
                chainSpec.getGenesisSlot()));

    // Validator registry
    initialState
        .withValidatorRegistry(emptyList())
        .withValidatorBalances(emptyList())
        .withValidatorRegistryLatestChangeSlot(chainSpec.getGenesisSlot())
        .withValidatorRegistryExitCount(UInt64.ZERO)
        .withValidatorRegistryDeltaChainTip(Hash32.ZERO);

    // Randomness and committees
    initialState
        .withLatestRandaoMixes(
            nCopies(chainSpec.getLatestRandaoMixesLength().getIntValue(), Hash32.ZERO))
        .withLatestVdfOutputs(
            nCopies(
                chainSpec
                    .getLatestRandaoMixesLength()
                    .dividedBy(chainSpec.getEpochLength())
                    .getIntValue(),
                Hash32.ZERO))
        .withShardCommitteesAtSlots(emptyList());

    // Proof of custody
    initialState.withCustodyChallenges(emptyList());

    // Finality
    initialState
        .withPreviousJustifiedSlot(chainSpec.getGenesisSlot())
        .withJustifiedSlot(chainSpec.getGenesisSlot())
        .withJustificationBitfield(UInt64.ZERO)
        .withFinalizedSlot(chainSpec.getGenesisSlot());

    // Recent state
    initialState
        .withLatestCrosslinks(
            nCopies(chainSpec.getShardCount().getIntValue(), CrosslinkRecord.EMPTY))
        .withLatestBlockRoots(
            nCopies(chainSpec.getLatestBlockRootsLength().getIntValue(), Hash32.ZERO))
        .withLatestPenalizedExitBalances(
            nCopies(chainSpec.getLatestPenalizedExitLength().getIntValue(), UInt64.ZERO))
        .withLatestAttestations(emptyList())
        .withBatchedBlockRoots(emptyList());

    // PoW receipt root
    initialState.withLatestDepositRoot(chainStart.getReceiptRoot()).withDepositRootVotes(emptyList());

    // handle initial deposits and activations
    final List<Deposit> initialDeposits = depositContract.getInitialDeposits();
    final ValidatorRegistryUpdater registryUpdater =
        ValidatorRegistryUpdater.fromState(initialState, chainSpec);

    initialDeposits.forEach(
        deposit -> {
          UInt24 index = registryUpdater.processDeposit(deposit);
          UInt64 balance = registryUpdater.getEffectiveBalance(index);

          // initial validators must have a strict deposit value
          if (chainSpec.getMaxDeposit().toGWei().equals(balance)) {
            registryUpdater.activate(index);
          }
        });


    BeaconState validatorsState = registryUpdater.applyTo(initialState);

    SpecHelpers specHelpers = new SpecHelpers(chainSpec);
    ShardCommittee[][] shuffling = specHelpers.get_shuffling(
        Hash32.ZERO,
        validatorsState.getValidatorRegistry().toArray(new ValidatorRecord[0]),
        chainSpec.getGenesisStartShard().getIntValue(),
        chainSpec.getGenesisSlot());
    ShardCommittee[][] doubleShuffling = Stream.concat(
        Arrays.stream(shuffling),
        Arrays.stream(shuffling))
        .toArray(ShardCommittee[][]::new);

    return initialState
        .withShardCommitteesAtSlots(doubleShuffling)
        .validate();
  }
}
