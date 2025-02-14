package org.ethereum.beacon.core.spec;

import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Signature domain codes.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#signature-domains">Signature
 *     domains</a> in the spec.
 */
public interface SignatureDomains {

  UInt64 BEACON_PROPOSER = UInt64.valueOf(0);

  UInt64 RANDAO = UInt64.valueOf(1);

  UInt64 ATTESTATION = UInt64.valueOf(2);

  UInt64 DEPOSIT = UInt64.valueOf(3);

  UInt64 VOLUNTARY_EXIT = UInt64.valueOf(4);

  UInt64 TRANSFER = UInt64.valueOf(5);
}
