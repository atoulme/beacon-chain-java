package org.ethereum.beacon.consensus.verifier.operation;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;

/**
 * Verifies {@link Attestation} beacon chain operation.
 *
 * @see Attestation
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.1/specs/core/0_beacon-chain.md#attestations">Attesations</a>
 *     in the spec.
 */
public class AttestationVerifier implements OperationVerifier<Attestation> {

  private BeaconChainSpec spec;

  public AttestationVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(Attestation attestation, BeaconState state) {
    try {
      return spec.verify_attestation(state, attestation)
          ? VerificationResult.PASSED
          : VerificationResult.failedResult("Attestation verification failed");
    } catch (SpecCommons.SpecAssertionFailed e) {
      return VerificationResult.failedResult(e.getMessage());
    }
  }
}
