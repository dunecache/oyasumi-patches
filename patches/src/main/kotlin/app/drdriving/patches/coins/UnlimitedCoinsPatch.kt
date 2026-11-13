package app.drdriving.patches.coins

import app.drdriving.patches.shared.Constants.COMPATIBILITY_DRDRIVING
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val unlimitedCoinsPatch = bytecodePatch(
    name = "Unlimited coins",
    description = "Make every coin purchase succeed regardless of how much gold is held, " +
        "and make every coin spend add coins instead of removing them, so the balance " +
        "never decreases.",
    default = true
) {
    compatibleWith(COMPATIBILITY_DRDRIVING)

    execute {
        // Five coin-spend methods on `d` debit both the reserve (`iCoinC`) and the
        // spendable balance (`iCoin`) by the same amount, using the same three-instruction
        // pattern:
        //
        //   iget  vN, vR, o;->iCoinC:I
        //   sub-int/2addr vN, vS
        //   iput  vN, vR, o;->iCoinC:I
        //   iget  vN, vR, o;->iCoin:I
        //   sub-int/2addr vN, vS
        //   iput  vN, vR, o;->iCoin:I
        //
        // Each fingerprint's two filters land on the two `iget` reads, so
        // `instructionMatches[0]` is the `iCoinC` decrement and
        // `instructionMatches[1]` is the `iCoin` decrement. Flipping both
        // `sub-int/2addr` to `add-int/2addr` makes the balance grow on every spend.
        //
        // The opcode guard before each replacement re-reads the instruction at the
        // matched index. If a future version reorders the method so the matched index
        // no longer points at a `SUB_INT_2ADDR`, the patch fails at build time instead
        // of silently applying a no-op or corrupting a different instruction.
        //
        // `buyCoin(II)V` is separate: it debits gold (`iGoldC` / `iGold`) and credits
        // coins, so its `iCoin` write is already an increment. Only its gold balance
        // guard needs to change. `iGold` is read twice in the method -- the guard
        // read at instruction index 3 and the debit read later in the purchase block
        // -- and `iCoin` is read once (the credit). The `iGold` filter matches the
        // first read (the guard), so `instructionMatches[0]` is the balance guard and
        // `instructionMatches[1]` is the `iCoin` credit read.
        val spendMethods = listOf(
            BuyCarFingerprint,
            ProcessCarSelectFingerprint,
            ProcessMultiEndingFingerprint,
            RentCarFingerprint,
            UpgradeCarFingerprint
        )

        spendMethods.forEach { fingerprint ->
            val reserveDebit = fingerprint.instructionMatches[0]
            val coinDebit = fingerprint.instructionMatches[1]

            check(reserveDebit.getInstruction<Instruction>().opcode == Opcode.SUB_INT_2ADDR) {
                "Expected SUB_INT_2ADDR at iCoinC debit in ${fingerprint.name}, " +
                    "found ${reserveDebit.getInstruction<Instruction>().opcode}"
            }
            check(coinDebit.getInstruction<Instruction>().opcode == Opcode.SUB_INT_2ADDR) {
                "Expected SUB_INT_2ADDR at iCoin debit in ${fingerprint.name}, " +
                    "found ${coinDebit.getInstruction<Instruction>().opcode}"
            }

            fingerprint.method.replaceInstruction(
                reserveDebit.index,
                "add-int/2addr"
            )
            fingerprint.method.replaceInstruction(
                coinDebit.index,
                "add-int/2addr"
            )
        }

        // `buyCoin` computes `v0 = o.iGold - DrDrivingActivity.rs` and rejects the
        // purchase with an early `return-void` when `v0 < p2`. The guard read is the
        // first `iGold` read in the method; the debit read (`iGoldC` / `iGold -= p2`)
        // comes later, after the balance check. Replacing the guard read with
        // `const/16 v0, 32767` keeps the instruction width identical (both are 2 code
        // units) and leaves the destination register and the following `sub-int/2addr`
        // untouched. The balance then reads `32767 - rs`, far above any purchase
        // cost, so the guard always falls through to the purchase block.
        //
        // `rs` is `random.nextInt(1000) + 1` (1..1000), so `32767 - rs` is 31767..32766.
        BuyCoinFingerprint.let { fingerprint ->
            // instructionMatches[0] is the `iGold` balance-guard read, [1] is the
            // `iCoin` credit read.
            val guardRead = fingerprint.instructionMatches[0]

            check(guardRead.getInstruction<Instruction>().opcode == Opcode.IGET) {
                "Expected IGET at buyCoin balance guard, " +
                    "found ${guardRead.getInstruction<Instruction>().opcode}"
            }

            fingerprint.method.replaceInstruction(
                guardRead.index,
                "const/16 v0, 32767"
            )
        }
    }
}