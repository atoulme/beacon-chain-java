package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;
import static org.ethereum.beacon.core.spec.SignatureDomains.EXIT;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;

/**
 * Verifies {@link VoluntaryExit} beacon chain operation.
 *
 * @see VoluntaryExit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#exits-1">Exits</a>
 *     in the spec.
 */
public class ExitVerifier implements OperationVerifier<VoluntaryExit> {

  private ChainSpec chainSpec;
  private SpecHelpers specHelpers;

  public ExitVerifier(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
    this.chainSpec = specHelpers.getChainSpec();
  }

  @Override
  public VerificationResult verify(VoluntaryExit voluntaryExit, BeaconState state) {
    specHelpers.checkIndexRange(state, voluntaryExit.getValidatorIndex());

    ValidatorRecord validator = state.getValidatorRegistry().get(voluntaryExit.getValidatorIndex());

    // Verify that validator.exit_epoch > get_delayed_activation_exit_epoch(get_current_epoch(state))
    if (!validator.getExitEpoch().greater(
        specHelpers.get_delayed_activation_exit_epoch(specHelpers.get_current_epoch(state)))) {
      return failedResult(
          "ACTIVATION_EXIT_DELAY exceeded, min exit epoch %s, got %s",
          state.getSlot().plus(chainSpec.getActivationExitDelay()), validator.getExitEpoch());
    }

    // Verify that get_current_epoch(state) >= exit.epoch
    if (!specHelpers.get_current_epoch(state).greaterEqual(voluntaryExit.getEpoch())) {
      return failedResult(
          "exit.epoch must be greater or equal to current_epoch");
    }

    // Let exit_message = hash_tree_root(
    //    Exit(
    //        epoch=exit.epoch,
    //        validator_index=exit.validator_index,
    //        signature=EMPTY_SIGNATURE)).
    // Verify that bls_verify(
    //    pubkey=validator.pubkey,
    //    message=exit_message,
    //    signature=exit.signature,
    //    domain=get_domain(state.fork, exit.epoch, DOMAIN_EXIT)).
    if (!specHelpers.bls_verify(
        validator.getPubKey(),
        specHelpers.signed_root(voluntaryExit, "signature"),
        voluntaryExit.getSignature(),
        specHelpers.get_domain(state.getForkData(), voluntaryExit.getEpoch(), EXIT))) {
      return failedResult("failed to verify signature");
    }

    return PASSED;
  }
}
