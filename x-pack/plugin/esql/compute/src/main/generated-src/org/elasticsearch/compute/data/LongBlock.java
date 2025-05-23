/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.compute.data;

// begin generated imports
import org.elasticsearch.TransportVersions;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.ReleasableIterator;
import org.elasticsearch.index.mapper.BlockLoader;

import java.io.IOException;
// end generated imports

/**
 * Block that stores long values.
 * This class is generated. Edit {@code X-Block.java.st} instead.
 */
public sealed interface LongBlock extends Block permits LongArrayBlock, LongVectorBlock, ConstantNullBlock, LongBigArrayBlock {

    /**
     * Retrieves the long value stored at the given value index.
     *
     * <p> Values for a given position are between getFirstValueIndex(position) (inclusive) and
     * getFirstValueIndex(position) + getValueCount(position) (exclusive).
     *
     * @param valueIndex the value index
     * @return the data value (as a long)
     */
    long getLong(int valueIndex);

    @Override
    LongVector asVector();

    @Override
    LongBlock filter(int... positions);

    @Override
    LongBlock keepMask(BooleanVector mask);

    @Override
    ReleasableIterator<? extends LongBlock> lookup(IntBlock positions, ByteSizeValue targetBlockSize);

    @Override
    LongBlock expand();

    static LongBlock readFrom(BlockStreamInput in) throws IOException {
        final byte serializationType = in.readByte();
        return switch (serializationType) {
            case SERIALIZE_BLOCK_VALUES -> LongBlock.readValues(in);
            case SERIALIZE_BLOCK_VECTOR -> LongVector.readFrom(in.blockFactory(), in).asBlock();
            case SERIALIZE_BLOCK_ARRAY -> LongArrayBlock.readArrayBlock(in.blockFactory(), in);
            case SERIALIZE_BLOCK_BIG_ARRAY -> LongBigArrayBlock.readArrayBlock(in.blockFactory(), in);
            default -> {
                assert false : "invalid block serialization type " + serializationType;
                throw new IllegalStateException("invalid serialization type " + serializationType);
            }
        };
    }

    private static LongBlock readValues(BlockStreamInput in) throws IOException {
        final int positions = in.readVInt();
        try (LongBlock.Builder builder = in.blockFactory().newLongBlockBuilder(positions)) {
            for (int i = 0; i < positions; i++) {
                if (in.readBoolean()) {
                    builder.appendNull();
                } else {
                    final int valueCount = in.readVInt();
                    builder.beginPositionEntry();
                    for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                        builder.appendLong(in.readLong());
                    }
                    builder.endPositionEntry();
                }
            }
            return builder.build();
        }
    }

    @Override
    default void writeTo(StreamOutput out) throws IOException {
        LongVector vector = asVector();
        final var version = out.getTransportVersion();
        if (vector != null) {
            out.writeByte(SERIALIZE_BLOCK_VECTOR);
            vector.writeTo(out);
        } else if (version.onOrAfter(TransportVersions.V_8_14_0) && this instanceof LongArrayBlock b) {
            out.writeByte(SERIALIZE_BLOCK_ARRAY);
            b.writeArrayBlock(out);
        } else if (version.onOrAfter(TransportVersions.V_8_14_0) && this instanceof LongBigArrayBlock b) {
            out.writeByte(SERIALIZE_BLOCK_BIG_ARRAY);
            b.writeArrayBlock(out);
        } else {
            out.writeByte(SERIALIZE_BLOCK_VALUES);
            LongBlock.writeValues(this, out);
        }
    }

    private static void writeValues(LongBlock block, StreamOutput out) throws IOException {
        final int positions = block.getPositionCount();
        out.writeVInt(positions);
        for (int pos = 0; pos < positions; pos++) {
            if (block.isNull(pos)) {
                out.writeBoolean(true);
            } else {
                out.writeBoolean(false);
                final int valueCount = block.getValueCount(pos);
                out.writeVInt(valueCount);
                for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                    out.writeLong(block.getLong(block.getFirstValueIndex(pos) + valueIndex));
                }
            }
        }
    }

    /**
     * Compares the given object with this block for equality. Returns {@code true} if and only if the
     * given object is a LongBlock, and both blocks are {@link #equals(LongBlock, LongBlock) equal}.
     */
    @Override
    boolean equals(Object obj);

    /** Returns the hash code of this block, as defined by {@link #hash(LongBlock)}. */
    @Override
    int hashCode();

    /**
     * Returns {@code true} if the given blocks are equal to each other, otherwise {@code false}.
     * Two blocks are considered equal if they have the same position count, and contain the same
     * values (including absent null values) in the same order. This definition ensures that the
     * equals method works properly across different implementations of the LongBlock interface.
     */
    static boolean equals(LongBlock block1, LongBlock block2) {
        if (block1 == block2) {
            return true;
        }
        final int positions = block1.getPositionCount();
        if (positions != block2.getPositionCount()) {
            return false;
        }
        for (int pos = 0; pos < positions; pos++) {
            if (block1.isNull(pos) || block2.isNull(pos)) {
                if (block1.isNull(pos) != block2.isNull(pos)) {
                    return false;
                }
            } else {
                final int valueCount = block1.getValueCount(pos);
                if (valueCount != block2.getValueCount(pos)) {
                    return false;
                }
                final int b1ValueIdx = block1.getFirstValueIndex(pos);
                final int b2ValueIdx = block2.getFirstValueIndex(pos);
                for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                    if (block1.getLong(b1ValueIdx + valueIndex) != block2.getLong(b2ValueIdx + valueIndex)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Generates the hash code for the given block. The hash code is computed from the block's values.
     * This ensures that {@code block1.equals(block2)} implies that {@code block1.hashCode()==block2.hashCode()}
     * for any two blocks, {@code block1} and {@code block2}, as required by the general contract of
     * {@link Object#hashCode}.
     */
    static int hash(LongBlock block) {
        final int positions = block.getPositionCount();
        int result = 1;
        for (int pos = 0; pos < positions; pos++) {
            if (block.isNull(pos)) {
                result = 31 * result - 1;
            } else {
                final int valueCount = block.getValueCount(pos);
                result = 31 * result + valueCount;
                final int firstValueIdx = block.getFirstValueIndex(pos);
                for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
                    long element = block.getLong(firstValueIdx + valueIndex);
                    result = 31 * result + (int) (element ^ (element >>> 32));
                }
            }
        }
        return result;
    }

    /**
     * Builder for {@link LongBlock}
     */
    sealed interface Builder extends Block.Builder, BlockLoader.LongBuilder permits LongBlockBuilder {
        /**
         * Appends a long to the current entry.
         */
        @Override
        Builder appendLong(long value);

        /**
         * Copy the values in {@code block} from {@code beginInclusive} to
         * {@code endExclusive} into this builder.
         */
        Builder copyFrom(LongBlock block, int beginInclusive, int endExclusive);

        /**
         * Copy the values in {@code block} at {@code position}. If this position
         * has a single value, this'll copy a single value. If this positions has
         * many values, it'll copy all of them. If this is {@code null}, then it'll
         * copy the {@code null}.
         */
        Builder copyFrom(LongBlock block, int position);

        @Override
        Builder appendNull();

        @Override
        Builder beginPositionEntry();

        @Override
        Builder endPositionEntry();

        @Override
        Builder copyFrom(Block block, int beginInclusive, int endExclusive);

        @Override
        Builder mvOrdering(Block.MvOrdering mvOrdering);

        @Override
        LongBlock build();
    }
}
