package org.ethereum.beacon.core.state;

import java.util.Map;
import java.util.function.Supplier;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.incremental.ObservableCompositeHelper;
import org.ethereum.beacon.ssz.incremental.ObservableCompositeHelper.ObsValue;
import org.ethereum.beacon.ssz.incremental.ObservableListImpl;
import org.ethereum.beacon.ssz.incremental.UpdateListener;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  private ObservableCompositeHelper obsHelper = new ObservableCompositeHelper();

  /* Misc */

  private ObsValue<SlotNumber> slot = obsHelper.newValue(SlotNumber.ZERO);
  private ObsValue<Time> genesisTime = obsHelper.newValue(Time.ZERO);
  private ObsValue<Fork> fork = obsHelper.newValue(Fork.EMPTY);

  /* Validator registry */

  private ObsValue<WriteList<ValidatorIndex, ValidatorRecord>> validatorRegistry =
      obsHelper.newValue(ObservableListImpl.create(ValidatorIndex::of));
  private ObsValue<WriteList<ValidatorIndex, Gwei>> validatorBalances =
      obsHelper.newValue(ObservableListImpl.create(ValidatorIndex::of));
  private ObsValue<EpochNumber> validatorRegistryUpdateEpoch = obsHelper.newValue(EpochNumber.ZERO);

  /* Randomness and committees */

  private ObsValue<WriteList<EpochNumber, Hash32>> latestRandaoMixes =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of));
  private ObsValue<ShardNumber> previousShufflingStartShard = obsHelper.newValue(ShardNumber.ZERO);
  private ObsValue<ShardNumber> currentShufflingStartShard = obsHelper.newValue(ShardNumber.ZERO);
  private ObsValue<EpochNumber> previousShufflingEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<EpochNumber> currentShufflingEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<Hash32> previousShufflingSeed = obsHelper.newValue(Hash32.ZERO);
  private ObsValue<Hash32> currentShufflingSeed = obsHelper.newValue(Hash32.ZERO);

  /* Finality */

  private ObsValue<WriteList<Integer, PendingAttestation>> previousEpochAttestations =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<WriteList<Integer, PendingAttestation>> currentEpochAttestations =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<EpochNumber> previousJustifiedEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<EpochNumber> currentJustifiedEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<Hash32> previousJustifiedRoot = obsHelper.newValue(Hash32.ZERO);
  private ObsValue<Hash32> currentJustifiedRoot = obsHelper.newValue(Hash32.ZERO);
  private ObsValue<Bitfield64> justificationBitfield = obsHelper.newValue(Bitfield64.ZERO);
  private ObsValue<EpochNumber> finalizedEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<Hash32> finalizedRoot = obsHelper.newValue(Hash32.ZERO);

  /* Recent state */

  private ObsValue<WriteList<ShardNumber, Crosslink>> previousCrosslinks =
      obsHelper.newValue(ObservableListImpl.create(ShardNumber::of));
  private ObsValue<WriteList<ShardNumber, Crosslink>> currentCrosslinks =
      obsHelper.newValue(ObservableListImpl.create(ShardNumber::of));
  private ObsValue<WriteList<SlotNumber, Hash32>> latestBlockRoots =
      obsHelper.newValue(ObservableListImpl.create(SlotNumber::of));
  private ObsValue<WriteList<SlotNumber, Hash32>> latestStateRoots =
      obsHelper.newValue(ObservableListImpl.create(SlotNumber::of));
  private ObsValue<WriteList<EpochNumber, Hash32>> latestActiveIndexRoots =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of));
  private ObsValue<WriteList<EpochNumber, Gwei>> latestSlashedBalances =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of));
  private ObsValue<BeaconBlockHeader> latestBlockHeader = obsHelper.newValue(BeaconBlockHeader.EMPTY);
  private ObsValue<WriteList<Integer, Hash32>> historicalRoots =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));

  /* PoW receipt root */

  private ObsValue<Eth1Data> latestEth1Data = obsHelper.newValue(Eth1Data.EMPTY);
  private ObsValue<WriteList<Integer, Eth1DataVote>> eth1DataVotes =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<UInt64> depositIndex = obsHelper.newValue(UInt64.ZERO);

  public BeaconStateImpl() {}

  BeaconStateImpl(BeaconState state) {
    slot.set(state.getSlot());
    genesisTime.set(state.getGenesisTime());
    fork.set(state.getFork());

    validatorRegistry.set(state.getValidatorRegistry().createMutableCopy());
    validatorBalances.set(state.getValidatorBalances().createMutableCopy());
    validatorRegistryUpdateEpoch.set(state.getValidatorRegistryUpdateEpoch());

    latestRandaoMixes.set(state.getLatestRandaoMixes().createMutableCopy());
    previousShufflingStartShard.set(state.getPreviousShufflingStartShard());
    currentShufflingStartShard.set(state.getCurrentShufflingStartShard());
    previousShufflingEpoch.set(state.getPreviousShufflingEpoch());
    currentShufflingEpoch.set(state.getCurrentShufflingEpoch());
    previousShufflingSeed.set(state.getPreviousShufflingSeed());
    currentShufflingSeed.set(state.getCurrentShufflingSeed());

    previousEpochAttestations.set(state.getPreviousEpochAttestations().createMutableCopy());
    currentEpochAttestations.set(state.getCurrentEpochAttestations().createMutableCopy());
    previousJustifiedEpoch.set(state.getPreviousJustifiedEpoch());
    currentJustifiedEpoch.set(state.getCurrentJustifiedEpoch());
    previousJustifiedRoot.set(state.getPreviousJustifiedRoot());
    currentJustifiedRoot.set(state.getCurrentJustifiedRoot());
    justificationBitfield.set(state.getJustificationBitfield());
    finalizedEpoch.set(state.getFinalizedEpoch());
    finalizedRoot.set(state.getFinalizedRoot());

    previousCrosslinks.set(state.getPreviousCrosslinks().createMutableCopy());
    currentCrosslinks.set(state.getCurrentCrosslinks().createMutableCopy());
    latestBlockRoots.set(state.getLatestBlockRoots().createMutableCopy());
    latestStateRoots.set(state.getLatestStateRoots().createMutableCopy());
    latestActiveIndexRoots.set(state.getLatestActiveIndexRoots().createMutableCopy());
    latestSlashedBalances.set(state.getLatestSlashedBalances().createMutableCopy());
    latestBlockHeader.set(state.getLatestBlockHeader());
    historicalRoots.set(state.getHistoricalRoots().createMutableCopy());

    latestEth1Data.set(state.getLatestEth1Data());
    eth1DataVotes.set(state.getEth1DataVotes().createMutableCopy());
    depositIndex.set(state.getDepositIndex());

    obsHelper.addAllListeners(state.getAllUpdateListeners());
  }

  @Override
  public Map<String, UpdateListener> getAllUpdateListeners() {
    return obsHelper.getAllUpdateListeners();
  }

  @Override
  public UpdateListener getUpdateListener(String observerId, Supplier<UpdateListener> listenerFactory) {
    return obsHelper.getUpdateListener(observerId, listenerFactory);
  }

  @Override
  public BeaconState createImmutable() {
    return new BeaconStateImpl(this);
  }

  @Override
  public SlotNumber getSlot() {
    return slot.get();
  }

  @Override
  public void setSlot(SlotNumber slot) {
    this.slot.set(slot);
  }

  @Override
  public Time getGenesisTime() {
    return genesisTime.get();
  }

  @Override
  public void setGenesisTime(Time genesisTime) {
    this.genesisTime.set(genesisTime);
  }

  @Override
  public Fork getFork() {
    return fork.get();
  }

  @Override
  public void setFork(Fork fork) {
    this.fork.set(fork);
  }

  @Override
  public WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return validatorRegistry.get();
  }

  public void setValidatorRegistry(
      WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry) {
    this.validatorRegistry.set(validatorRegistry);
  }

  @Override
  public WriteList<ValidatorIndex, Gwei> getValidatorBalances() {
    return validatorBalances.get();
  }

  public void setValidatorBalances(
      WriteList<ValidatorIndex, Gwei> validatorBalances) {
    this.validatorBalances.set(validatorBalances);
  }

  @Override
  public EpochNumber getValidatorRegistryUpdateEpoch() {
    return validatorRegistryUpdateEpoch.get();
  }

  @Override
  public void setValidatorRegistryUpdateEpoch(
      EpochNumber validatorRegistryUpdateEpoch) {
    this.validatorRegistryUpdateEpoch.set(validatorRegistryUpdateEpoch);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return latestRandaoMixes.get();
  }

  public void setLatestRandaoMixes(
      WriteList<EpochNumber, Hash32> latestRandaoMixes) {
    this.latestRandaoMixes.set(latestRandaoMixes);
  }

  @Override
  public ShardNumber getPreviousShufflingStartShard() {
    return previousShufflingStartShard.get();
  }

  @Override
  public void setPreviousShufflingStartShard(
      ShardNumber previousShufflingStartShard) {
    this.previousShufflingStartShard.set(previousShufflingStartShard);
  }

  @Override
  public ShardNumber getCurrentShufflingStartShard() {
    return currentShufflingStartShard.get();
  }

  @Override
  public void setCurrentShufflingStartShard(
      ShardNumber currentShufflingStartShard) {
    this.currentShufflingStartShard.set(currentShufflingStartShard);
  }

  @Override
  public EpochNumber getPreviousShufflingEpoch() {
    return previousShufflingEpoch.get();
  }

  @Override
  public void setPreviousShufflingEpoch(EpochNumber previousShufflingEpoch) {
    this.previousShufflingEpoch.set(previousShufflingEpoch);
  }

  @Override
  public EpochNumber getCurrentShufflingEpoch() {
    return currentShufflingEpoch.get();
  }

  @Override
  public void setCurrentShufflingEpoch(EpochNumber currentShufflingEpoch) {
    this.currentShufflingEpoch.set(currentShufflingEpoch);
  }

  @Override
  public Hash32 getPreviousShufflingSeed() {
    return previousShufflingSeed.get();
  }

  @Override
  public void setPreviousShufflingSeed(Hash32 previousShufflingSeed) {
    this.previousShufflingSeed.set(previousShufflingSeed);
  }

  @Override
  public Hash32 getCurrentShufflingSeed() {
    return currentShufflingSeed.get();
  }

  @Override
  public void setCurrentShufflingSeed(Hash32 currentShufflingSeed) {
    this.currentShufflingSeed.set(currentShufflingSeed);
  }

  public WriteList<Integer, PendingAttestation> getPreviousEpochAttestations() {
    return previousEpochAttestations.get();
  }

  public void setPreviousEpochAttestations(
      WriteList<Integer, PendingAttestation> previousEpochAttestations) {
    this.previousEpochAttestations.set(previousEpochAttestations);
  }

  public WriteList<Integer, PendingAttestation> getCurrentEpochAttestations() {
    return currentEpochAttestations.get();
  }

  public void setCurrentEpochAttestations(
      WriteList<Integer, PendingAttestation> currentEpochAttestations) {
    this.currentEpochAttestations.set(currentEpochAttestations);
  }

  @Override
  public EpochNumber getPreviousJustifiedEpoch() {
    return previousJustifiedEpoch.get();
  }

  @Override
  public void setPreviousJustifiedEpoch(EpochNumber previousJustifiedEpoch) {
    this.previousJustifiedEpoch.set(previousJustifiedEpoch);
  }

  @Override
  public EpochNumber getCurrentJustifiedEpoch() {
    return currentJustifiedEpoch.get();
  }

  @Override
  public void setCurrentJustifiedEpoch(EpochNumber currentJustifiedEpoch) {
    this.currentJustifiedEpoch.set(currentJustifiedEpoch);
  }

  @Override
  public Hash32 getPreviousJustifiedRoot() {
    return previousJustifiedRoot.get();
  }

  @Override
  public void setPreviousJustifiedRoot(Hash32 previousJustifiedRoot) {
    this.previousJustifiedRoot.set(previousJustifiedRoot);
  }

  @Override
  public Hash32 getCurrentJustifiedRoot() {
    return currentJustifiedRoot.get();
  }

  @Override
  public void setCurrentJustifiedRoot(Hash32 currentJustifiedRoot) {
    this.currentJustifiedRoot.set(currentJustifiedRoot);
  }

  @Override
  public Bitfield64 getJustificationBitfield() {
    return justificationBitfield.get();
  }

  @Override
  public void setJustificationBitfield(Bitfield64 justificationBitfield) {
    this.justificationBitfield.set(justificationBitfield);
  }

  @Override
  public EpochNumber getFinalizedEpoch() {
    return finalizedEpoch.get();
  }

  @Override
  public void setFinalizedEpoch(EpochNumber finalizedEpoch) {
    this.finalizedEpoch.set(finalizedEpoch);
  }

  @Override
  public Hash32 getFinalizedRoot() {
    return finalizedRoot.get();
  }

  @Override
  public void setFinalizedRoot(Hash32 finalizedRoot) {
    this.finalizedRoot.set(finalizedRoot);
  }

  @Override
  public WriteList<ShardNumber, Crosslink> getPreviousCrosslinks() {
    return previousCrosslinks.get();
  }

  public void setPreviousCrosslinks(
      WriteList<ShardNumber, Crosslink> previousCrosslinks) {
    this.previousCrosslinks.set(previousCrosslinks);
  }

  @Override
  public WriteList<ShardNumber, Crosslink> getCurrentCrosslinks() {
    return currentCrosslinks.get();
  }

  public void setCurrentCrosslinks(
      WriteList<ShardNumber, Crosslink> currentCrosslinks) {
    this.currentCrosslinks.set(currentCrosslinks);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestBlockRoots() {
    return latestBlockRoots.get();
  }

  public void setLatestBlockRoots(
      WriteList<SlotNumber, Hash32> latestBlockRoots) {
    this.latestBlockRoots.set(latestBlockRoots);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestStateRoots() {
    return latestStateRoots.get();
  }

  public void setLatestStateRoots(
      WriteList<SlotNumber, Hash32> latestStateRoots) {
    this.latestStateRoots.set(latestStateRoots);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return latestActiveIndexRoots.get();
  }

  public void setLatestActiveIndexRoots(
      WriteList<EpochNumber, Hash32> latestActiveIndexRoots) {
    this.latestActiveIndexRoots.set(latestActiveIndexRoots);
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestSlashedBalances() {
    return latestSlashedBalances.get();
  }

  public void setLatestSlashedBalances(
      WriteList<EpochNumber, Gwei> latestSlashedBalances) {
    this.latestSlashedBalances.set(latestSlashedBalances);
  }

  @Override
  public BeaconBlockHeader getLatestBlockHeader() {
    return latestBlockHeader.get();
  }

  @Override
  public void setLatestBlockHeader(BeaconBlockHeader latestBlockHeader) {
    this.latestBlockHeader.set(latestBlockHeader);
  }

  public WriteList<Integer, Hash32> getHistoricalRoots() {
    return historicalRoots.get();
  }

  public void setHistoricalRoots(
      WriteList<Integer, Hash32> historicalRoots) {
    this.historicalRoots.set(historicalRoots);
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data.get();
  }

  @Override
  public void setLatestEth1Data(Eth1Data latestEth1Data) {
    this.latestEth1Data.set(latestEth1Data);
  }

  @Override
  public WriteList<Integer, Eth1DataVote> getEth1DataVotes() {
    return eth1DataVotes.get();
  }

  public void setEth1DataVotes(
      WriteList<Integer, Eth1DataVote> eth1DataVotes) {
    this.eth1DataVotes.set(eth1DataVotes);
  }

  @Override
  public UInt64 getDepositIndex() {
    return depositIndex.get();
  }

  @Override
  public void setDepositIndex(UInt64 depositIndex) {
    this.depositIndex.set(depositIndex);
  }

  /*********  List Getters/Setter for serialization  **********/



  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }

  @Override
  public boolean equals(Object obj) {
    return equalsHelper((BeaconState) obj);
  }

  @Override
  public String toString() {
    return toStringShort(null);
  }
}
