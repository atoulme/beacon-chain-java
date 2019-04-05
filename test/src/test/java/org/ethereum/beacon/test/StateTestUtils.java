package org.ethereum.beacon.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.type.StateTestCase;
import org.ethereum.beacon.test.type.StateTestCase.BeaconStateData;
import org.ethereum.beacon.test.type.StateTestCase.BeaconStateData.BlockHeaderData;
import org.ethereum.beacon.test.type.StateTestCase.BeaconStateData.CrossLinkData;
import org.ethereum.beacon.test.type.StateTestCase.BeaconStateData.ValidatorData;
import org.ethereum.beacon.test.type.StateTestCase.BlockData.BlockBodyData.Eth1;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

/** Various utility methods aiding state tests development. */
public abstract class StateTestUtils {
  private StateTestUtils() {}

  public static Pair<BeaconBlock, Optional<String>> parseBlockData(
      StateTestCase.BlockData blockData) {
    Eth1Data eth1Data1 =
        new Eth1Data(
            Hash32.fromHexString(blockData.getBody().getEth1Data().getDepositRoot()),
            Hash32.fromHexString(blockData.getBody().getEth1Data().getBlockHash()));

    // Attestations
    List<Attestation> attestations = new ArrayList<>();
    for (StateTestCase.BeaconStateData.AttestationData attestationData :
        blockData.getBody().getAttestations()) {
      AttestationData attestationData1 =
          new AttestationData(
              SlotNumber.castFrom(UInt64.valueOf(attestationData.getData().getSlot())),
              Hash32.fromHexString(attestationData.getData().getBeaconBlockRoot()),
              EpochNumber.castFrom(UInt64.valueOf(attestationData.getData().getSourceEpoch())),
              Hash32.fromHexString(attestationData.getData().getSourceRoot()),
              Hash32.fromHexString(attestationData.getData().getTargetRoot()),
              ShardNumber.of(attestationData.getData().getShard()),
              new Crosslink(
                  EpochNumber.castFrom(
                      UInt64.valueOf(attestationData.getData().getPreviousCrosslink().getEpoch())),
                  Hash32.fromHexString(
                      attestationData.getData().getPreviousCrosslink().getCrosslinkDataRoot())),
              Hash32.fromHexString(attestationData.getData().getCrosslinkDataRoot()));
      Attestation attestation =
          new Attestation(
              Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
              attestationData1,
              Bitfield.of(BytesValue.fromHexString(attestationData.getCustodyBitfield())),
              BLSSignature.wrap(Bytes96.fromHexString(attestationData.getAggregateSignature())));
      attestations.add(attestation);
    }

    if (!blockData.getBody().getAttesterSlashings().isEmpty()) {
      return Pair.with(null, Optional.of("Implement block attestation slashings!"));
    }

    // Deposits
    List<Deposit> deposits = new ArrayList<>();
    for (StateTestCase.BlockData.BlockBodyData.DepositData depositData :
        blockData.getBody().getDeposits()) {
      Deposit deposit =
          new Deposit(
              depositData.getProof().stream()
                  .map(Hash32::fromHexString)
                  .collect(Collectors.toList()),
              UInt64.valueOf(depositData.getIndex()),
              new DepositData(
                  Gwei.castFrom(UInt64.valueOf(depositData.getDepositData().getAmount())),
                  Time.of(depositData.getDepositData().getTimestamp()),
                  new DepositInput(
                      BLSPubkey.fromHexString(
                          depositData.getDepositData().getDepositInput().getPubkey()),
                      Hash32.fromHexString(
                          depositData
                              .getDepositData()
                              .getDepositInput()
                              .getWithdrawalCredentials()),
                      BLSSignature.wrap(
                          Bytes96.fromHexString(
                              depositData
                                  .getDepositData()
                                  .getDepositInput()
                                  .getProofOfPossession())))));
      deposits.add(deposit);
    }

    // Proposer slashings
    List<ProposerSlashing> proposerSlashings = new ArrayList<>();
    for (StateTestCase.BlockData.BlockBodyData.SlashingData slashingData :
        blockData.getBody().getProposerSlashings()) {
      BeaconBlockHeader header1 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(slashingData.getHeader1().getSlot())),
              Hash32.fromHexString(slashingData.getHeader1().getPreviousBlockRoot()),
              Hash32.fromHexString(slashingData.getHeader1().getStateRoot()),
              Hash32.fromHexString(slashingData.getHeader1().getBlockBodyRoot()),
              BLSSignature.wrap(Bytes96.fromHexString(slashingData.getHeader1().getSignature())));
      BeaconBlockHeader header2 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(slashingData.getHeader2().getSlot())),
              Hash32.fromHexString(slashingData.getHeader2().getPreviousBlockRoot()),
              Hash32.fromHexString(slashingData.getHeader2().getStateRoot()),
              Hash32.fromHexString(slashingData.getHeader2().getBlockBodyRoot()),
              BLSSignature.wrap(Bytes96.fromHexString(slashingData.getHeader2().getSignature())));
      ProposerSlashing proposerSlashing =
          new ProposerSlashing(
              ValidatorIndex.of(slashingData.getProposerIndex()), header1, header2);
      proposerSlashings.add(proposerSlashing);
    }

    // Transfers
    List<Transfer> transfers = new ArrayList<>();
    for (StateTestCase.BlockData.BlockBodyData.TransferData transferData :
        blockData.getBody().getTransfers()) {
      Transfer transfer =
          new Transfer(
              ValidatorIndex.of(transferData.getSender()),
              ValidatorIndex.of(transferData.getRecipient()),
              Gwei.castFrom(UInt64.valueOf(transferData.getAmount())),
              Gwei.castFrom(UInt64.valueOf(transferData.getFee())),
              SlotNumber.castFrom(UInt64.valueOf(transferData.getSlot())),
              BLSPubkey.fromHexString(transferData.getPubkey()),
              BLSSignature.wrap(Bytes96.fromHexString(transferData.getSignature())));
      transfers.add(transfer);
    }

    // Voluntary exits
    if (!blockData.getBody().getVoluntaryExits().isEmpty()) {
      return Pair.with(null, Optional.of("Implement block voluntary exits!"));
    }

    // Finally, creating a block
    BeaconBlockBody blockBody =
        new BeaconBlockBody(
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getBody().getRandaoReveal())),
            eth1Data1,
            proposerSlashings,
            Collections.emptyList(),
            attestations,
            deposits,
            Collections.emptyList(),
            transfers);
    BeaconBlock block =
        new BeaconBlock(
            SlotNumber.castFrom(UInt64.valueOf(blockData.getSlot())),
            Hash32.fromHexString(blockData.getPreviousBlockRoot()),
            Hash32.fromHexString(blockData.getStateRoot()),
            blockBody,
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getSignature())));

    return Pair.with(block, Optional.empty());
  }

  public static MutableBeaconState parseBeaconState(BeaconStateData data) {
    MutableBeaconState state = BeaconState.getEmpty().createMutableCopy();

    state.setSlot(SlotNumber.castFrom(UInt64.valueOf(data.getSlot())));
    state.setGenesisTime(Time.of(data.getGenesisTime()));
    state.setFork(parseFork(data.getFork()));
    state.setValidatorRegistryUpdateEpoch(
        EpochNumber.castFrom(UInt64.valueOf(data.getValidatorRegistryUpdateEpoch())));
    state.setPreviousShufflingStartShard(ShardNumber.of(data.getPreviousShufflingStartShard()));
    state.setCurrentShufflingStartShard(ShardNumber.of(data.getCurrentShufflingStartShard()));
    state.setPreviousShufflingEpoch(
        EpochNumber.castFrom(UInt64.valueOf(data.getPreviousShufflingEpoch())));
    state.setCurrentShufflingEpoch(
        EpochNumber.castFrom(UInt64.valueOf(data.getCurrentShufflingEpoch())));
    state.setPreviousShufflingSeed(Hash32.fromHexString(data.getPreviousShufflingSeed()));
    state.setCurrentShufflingSeed(Hash32.fromHexString(data.getCurrentShufflingSeed()));
    state.setPreviousJustifiedEpoch(
        EpochNumber.castFrom(UInt64.valueOf(data.getPreviousJustifiedEpoch())));
    state.setCurrentJustifiedEpoch(
        EpochNumber.castFrom(UInt64.valueOf(data.getCurrentJustifiedEpoch())));
    state.setPreviousJustifiedRoot(Hash32.fromHexString(data.getPreviousJustifiedRoot()));
    state.setCurrentJustifiedRoot(Hash32.fromHexString(data.getCurrentJustifiedRoot()));
    state.setJustificationBitfield(new Bitfield64(UInt64.valueOf(data.getJustificationBitfield())));
    state.setFinalizedEpoch(EpochNumber.castFrom(UInt64.valueOf(data.getFinalizedEpoch())));
    state.setFinalizedRoot(Hash32.fromHexString(data.getFinalizedRoot()));
    state.setLatestBlockHeader(parseBeaconBlockHeader(data.getLatestBlockHeader()));
    state.setLatestEth1Data(parseEth1Data(data.getLatestEth1Data()));
    state.setDepositIndex(UInt64.valueOf(data.getDepositIndex()));

    state.getValidatorRegistry().addAll(parseValidatorRegistry(data.getValidatorRegistry()));
    state.getValidatorBalances().addAll(parseBalances(data.getValidatorBalances()));
    state.getLatestRandaoMixes().addAll(parseHashes(data.getLatestRandaoMixes()));
    state.getPreviousEpochAttestations().addAll(
        parsePendingAttestations(data.getPreviousEpochAttestations()));
    state.getCurrentEpochAttestations().addAll(
        parsePendingAttestations(data.getCurrentEpochAttestations()));
    state.getCurrentCrosslinks().addAll(parseCrosslinks(data.getLatestCrosslinks()));
    state.getLatestBlockRoots().addAll(parseHashes(data.getLatestBlockRoots()));
    state.getLatestStateRoots().addAll(parseHashes(data.getLatestStateRoots()));
    state.getLatestActiveIndexRoots().addAll(parseHashes(data.getLatestActiveIndexRoots()));
    state.getHistoricalRoots().addAll(parseHashes(data.getHistoricalRoots()));
    state.getLatestSlashedBalances().addAll(parseBalances(data.getLatestSlashedBalances()));

    return state;
  }

  public static List<Crosslink> parseCrosslinks(List<CrossLinkData> data) {
    return data.stream().map(StateTestUtils::parseCrosslink).collect(Collectors.toList());
  }

  public static List<PendingAttestation> parsePendingAttestations(
      List<BeaconStateData.AttestationData> data) {
    return data.stream().map(StateTestUtils::parsePendingAttestation).collect(Collectors.toList());
  }

  public static List<Hash32> parseHashes(List<String> data) {
    return data.stream().map(Hash32::fromHexString).collect(Collectors.toList());
  }

  public static List<Gwei> parseBalances(List<String> data) {
    return data.stream().map(b -> Gwei.castFrom(UInt64.valueOf(b))).collect(Collectors.toList());
  }

  public static List<ValidatorRecord> parseValidatorRegistry(List<ValidatorData> data) {
    return data.stream().map(StateTestUtils::parseValidatorRecord).collect(Collectors.toList());
  }

  public static ValidatorRecord parseValidatorRecord(ValidatorData data) {
    return new ValidatorRecord(
        BLSPubkey.fromHexString(data.getPubkey()),
        Hash32.fromHexString(data.getWithdrawalCredentials()),
        EpochNumber.castFrom(UInt64.valueOf(data.getActivationEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getExitEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getWithdrawableEpoch())),
        data.getInitiatedExit(),
        data.getSlashed());
  }

  public static Eth1Data parseEth1Data(Eth1 data) {
    return new Eth1Data(
        Hash32.fromHexString(data.getDepositRoot()), Hash32.fromHexString(data.getBlockHash()));
  }

  public static BeaconBlockHeader parseBeaconBlockHeader(BlockHeaderData data) {
    return new BeaconBlockHeader(
        SlotNumber.castFrom(UInt64.valueOf(data.getSlot())),
        Hash32.fromHexString(data.getPreviousBlockRoot()),
        Hash32.fromHexString(data.getStateRoot()),
        Hash32.fromHexString(data.getBlockBodyRoot()),
        data.getSignature() == null
            ? BLSSignature.ZERO
            : BLSSignature.wrap(Bytes96.fromHexString(data.getSignature())));
  }

  public static Fork parseFork(BeaconStateData.Fork data) {
    return new Fork(
        Bytes4.fromHexString(data.getPreviousVersion()),
        Bytes4.fromHexString(data.getCurrentVersion()),
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())));
  }

  public static Crosslink parseCrosslink(CrossLinkData data) {
    return new Crosslink(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        Hash32.fromHexString(data.getCrosslinkDataRoot()));
  }

  public static PendingAttestation parsePendingAttestation(
      StateTestCase.BeaconStateData.AttestationData attestationData) {
    AttestationData attestationData1 =
        new AttestationData(
            SlotNumber.castFrom(UInt64.valueOf(attestationData.getData().getSlot())),
            Hash32.fromHexString(attestationData.getData().getBeaconBlockRoot()),
            EpochNumber.castFrom(UInt64.valueOf(attestationData.getData().getSourceEpoch())),
            Hash32.fromHexString(attestationData.getData().getSourceRoot()),
            Hash32.fromHexString(attestationData.getData().getTargetRoot()),
            ShardNumber.of(attestationData.getData().getShard()),
            new Crosslink(
                EpochNumber.castFrom(
                    UInt64.valueOf(attestationData.getData().getPreviousCrosslink().getEpoch())),
                Hash32.fromHexString(
                    attestationData.getData().getPreviousCrosslink().getCrosslinkDataRoot())),
            Hash32.fromHexString(attestationData.getData().getCrosslinkDataRoot()));

    return new PendingAttestation(
        Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
        attestationData1,
        Bitfield.of(BytesValue.fromHexString(attestationData.getCustodyBitfield())),
        SlotNumber.castFrom(UInt64.valueOf(attestationData.getInclusionSlot())));
  }
}
