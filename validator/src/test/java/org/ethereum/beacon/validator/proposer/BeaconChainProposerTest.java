package org.ethereum.beacon.validator.proposer;

import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.mockProposer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.chain.util.PendingOperationsTestUtil;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.Proposal;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.util.AttestationTestUtil;
import org.ethereum.beacon.core.util.AttesterSlashingTestUtil;
import org.ethereum.beacon.core.util.DepositTestUtil;
import org.ethereum.beacon.core.util.Eth1DataTestUtil;
import org.ethereum.beacon.core.util.ExitTestUtil;
import org.ethereum.beacon.core.util.ProposerSlashingTestUtil;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.pow.util.DepositContractTestUtil;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.util.MessageSignerTestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconChainProposerTest {

  @Test
  public void proposeABlock() {
    Random random = new Random();

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(ChainSpec.DEFAULT, () -> 0L);
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    StateTransition<BeaconStateEx> perEpochTransition =
        StateTransitionTestUtil.createStateWithNoTransition();
    BeaconChainProposer proposer =
        mockProposer(perBlockTransition, perEpochTransition, depositContract, specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers);
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateEx(initialState, Hash32.ZERO), block);

    Assert.assertEquals(
        specHelpers.hash_tree_root(stateAfterBlock.getCanonicalState()), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, initialState, block, signer));
  }

  @Test
  public void proposeABlockWithOperations() {
    Random random = new Random();

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(ChainSpec.DEFAULT, () -> 0L);
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    StateTransition<BeaconStateEx> perEpochTransition =
        StateTransitionTestUtil.createStateWithNoTransition();
    BeaconChainProposer proposer =
        mockProposer(perBlockTransition, perEpochTransition, depositContract, specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    List<Attestation> attestations =
        AttestationTestUtil.createRandomList(
            random, specHelpers.getChainSpec().getMaxAttestations());
    List<ProposerSlashing> proposerSlashings =
        ProposerSlashingTestUtil.createRandomList(
            random, specHelpers.getChainSpec().getMaxProposerSlashings());
    List<AttesterSlashing> casperSlashings =
        AttesterSlashingTestUtil.createRandomList(
            random, specHelpers.getChainSpec().getMaxAttesterSlashings());
    List<Exit> exits =
        ExitTestUtil.createRandomList(random, specHelpers.getChainSpec().getMaxExits());

    PendingOperations pendingOperations =
        PendingOperationsTestUtil.mockPendingOperations(
            attestations, attestations, proposerSlashings, casperSlashings, exits);
    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, pendingOperations);
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    Mockito.verify(pendingOperations)
        .peekAggregatedAttestations(
            specHelpers.getChainSpec().getMaxAttestations(),
            initialState
                .getSlot()
                .minus(specHelpers.getChainSpec().getMinAttestationInclusionDelay()));

    Mockito.verify(pendingOperations)
        .peekProposerSlashings(specHelpers.getChainSpec().getMaxProposerSlashings());
    Mockito.verify(pendingOperations)
        .peekAttesterSlashings(specHelpers.getChainSpec().getMaxAttesterSlashings());
    Mockito.verify(pendingOperations).peekExits(specHelpers.getChainSpec().getMaxExits());

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateEx(initialState, Hash32.ZERO), block);

    Assert.assertEquals(
        specHelpers.hash_tree_root(stateAfterBlock.getCanonicalState()), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, initialState, block, signer));

    Assert.assertEquals(attestations, block.getBody().getAttestations().listCopy());
    Assert.assertEquals(proposerSlashings, block.getBody().getProposerSlashings().listCopy());
    Assert.assertEquals(casperSlashings, block.getBody().getAttesterSlashings().listCopy());
    Assert.assertEquals(exits, block.getBody().getExits().listCopy());
  }

  @Test
  public void proposeABlockWithDeposits() {
    Random random = new Random();

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(ChainSpec.DEFAULT, () -> 0L);

    List<Deposit> deposits =
        DepositTestUtil.createRandomList(
            random,
            specHelpers.getChainSpec(),
            UInt64.ZERO,
            specHelpers.getChainSpec().getMaxDeposits());
    Eth1Data eth1Data = Eth1DataTestUtil.createRandom(random);
    List<DepositInfo> depositInfos =
        deposits.stream()
            .map(deposit -> new DepositInfo(deposit, eth1Data))
            .collect(Collectors.toList());
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, depositInfos);
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    StateTransition<BeaconStateEx> perEpochTransition =
        StateTransitionTestUtil.createStateWithNoTransition();
    BeaconChainProposer proposer =
        mockProposer(perBlockTransition, perEpochTransition, depositContract, specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, specHelpers, PendingOperationsTestUtil.createEmptyPendingOperations());
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    Mockito.verify(depositContract)
        .peekDeposits(
            Mockito.eq(specHelpers.getChainSpec().getMaxDeposits()),
            Mockito.any(),
            Mockito.eq(initialState.getLatestEth1Data()));

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateEx(initialState, Hash32.ZERO), block);

    Assert.assertEquals(
        specHelpers.hash_tree_root(stateAfterBlock.getCanonicalState()), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, initialState, block, signer));

    Assert.assertEquals(
        depositInfos.stream().map(DepositInfo::getDeposit).collect(Collectors.toList()),
        block.getBody().getDeposits().listCopy());
    Assert.assertEquals(depositContract.getLatestEth1Data(), Optional.of(block.getEth1Data()));
  }

  @Test
  public void proposeABlockWithEpochTransition() {
    Random random = new Random();

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(ChainSpec.DEFAULT, () -> 0L);
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();

    final Eth1Data eth1Data = Eth1DataTestUtil.createRandom(random);
    StateTransition<BeaconStateEx> perEpochTransition =
        source -> {
          MutableBeaconState newState = source.getCanonicalState().createMutableCopy();
          newState.setLatestEth1Data(eth1Data);
          return new BeaconStateEx(newState, source.getLatestChainBlockHash());
        };

    BeaconChainProposer proposer =
        mockProposer(perBlockTransition, perEpochTransition, depositContract, specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers);

    // set slot to the end of the epoch
    MutableBeaconState modifiedState =
        initialObservedState.getLatestSlotState().createMutableCopy();
    modifiedState.setSlot(specHelpers.getChainSpec().getEpochLength().decrement());

    ObservableBeaconState endOfTheEpoch =
        new ObservableBeaconState(
            initialObservedState.getHead(),
            modifiedState,
            initialObservedState.getPendingOperations());

    BeaconBlock block = proposer.propose(endOfTheEpoch, signer);

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateEx(modifiedState, Hash32.ZERO), block);
    BeaconStateEx stateAfterEpoch = perEpochTransition.apply(stateAfterBlock);

    Assert.assertEquals(
        specHelpers.hash_tree_root(stateAfterEpoch.getCanonicalState()), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, modifiedState, block, signer));
  }

  private boolean verifySignature(
      SpecHelpers specHelpers,
      BeaconState initialState,
      BeaconBlock block,
      MessageSigner<BLSSignature> signer) {

    Proposal signedData =
        new Proposal(
            initialState.getSlot(),
            specHelpers.getChainSpec().getBeaconChainShardNumber(),
            specHelpers.signed_root(block, "signature"),
            block.getSignature());
    BLSSignature expectedSignature =
        signer.sign(
            specHelpers.signed_root(signedData,"signature"),
            specHelpers.get_domain(
                initialState.getForkData(),
                specHelpers.get_current_epoch(initialState),
                SignatureDomains.PROPOSAL));

    return expectedSignature.equals(block.getSignature());
  }
}
