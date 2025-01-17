/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.spi.block;

import io.airlift.slice.Slice;
import io.airlift.slice.SliceOutput;
import io.airlift.slice.Slices;
import io.airlift.slice.XxHash64;

import static io.airlift.slice.Slices.EMPTY_SLICE;
import static io.trino.spi.block.BlockUtil.checkReadablePosition;

public abstract class AbstractVariableWidthBlock
        implements Block
{
    protected abstract Slice getRawSlice(int position);

    protected abstract int getPositionOffset(int position);

    protected abstract boolean isEntryNull(int position);

    @Override
    public String getEncodingName()
    {
        return VariableWidthBlockEncoding.NAME;
    }

    @Override
    public byte getByte(int position, int offset)
    {
        checkReadablePosition(this, position);
        return getRawSlice(position).getByte(getPositionOffset(position) + offset);
    }

    @Override
    public short getShort(int position, int offset)
    {
        checkReadablePosition(this, position);
        return getRawSlice(position).getShort(getPositionOffset(position) + offset);
    }

    @Override
    public int getInt(int position, int offset)
    {
        checkReadablePosition(this, position);
        return getRawSlice(position).getInt(getPositionOffset(position) + offset);
    }

    @Override
    public long getLong(int position, int offset)
    {
        checkReadablePosition(this, position);
        return getRawSlice(position).getLong(getPositionOffset(position) + offset);
    }

    @Override
    public Slice getSlice(int position, int offset, int length)
    {
        checkReadablePosition(this, position);
        return getRawSlice(position).slice(getPositionOffset(position) + offset, length);
    }

    @Override
    public void writeSliceTo(int position, int offset, int length, SliceOutput output)
    {
        checkReadablePosition(this, position);
        output.writeBytes(getRawSlice(position), getPositionOffset(position) + offset, length);
    }

    @Override
    public boolean equals(int position, int offset, Block otherBlock, int otherPosition, int otherOffset, int length)
    {
        checkReadablePosition(this, position);
        Slice rawSlice = getRawSlice(position);
        if (getSliceLength(position) < length) {
            return false;
        }
        return otherBlock.bytesEqual(otherPosition, otherOffset, rawSlice, getPositionOffset(position) + offset, length);
    }

    @Override
    public boolean bytesEqual(int position, int offset, Slice otherSlice, int otherOffset, int length)
    {
        checkReadablePosition(this, position);
        return getRawSlice(position).equals(getPositionOffset(position) + offset, length, otherSlice, otherOffset, length);
    }

    @Override
    public long hash(int position, int offset, int length)
    {
        checkReadablePosition(this, position);
        return XxHash64.hash(getRawSlice(position), getPositionOffset(position) + offset, length);
    }

    @Override
    public int compareTo(int position, int offset, int length, Block otherBlock, int otherPosition, int otherOffset, int otherLength)
    {
        checkReadablePosition(this, position);
        Slice rawSlice = getRawSlice(position);
        if (getSliceLength(position) < length) {
            throw new IllegalArgumentException("Length longer than value length");
        }
        return -otherBlock.bytesCompare(otherPosition, otherOffset, otherLength, rawSlice, getPositionOffset(position) + offset, length);
    }

    @Override
    public int bytesCompare(int position, int offset, int length, Slice otherSlice, int otherOffset, int otherLength)
    {
        checkReadablePosition(this, position);
        return getRawSlice(position).compareTo(getPositionOffset(position) + offset, length, otherSlice, otherOffset, otherLength);
    }

    @Override
    public Block getSingleValueBlock(int position)
    {
        if (isNull(position)) {
            return new VariableWidthBlock(0, 1, EMPTY_SLICE, new int[] {0, 0}, new boolean[] {true});
        }

        int offset = getPositionOffset(position);
        int entrySize = getSliceLength(position);

        Slice copy = Slices.copyOf(getRawSlice(position), offset, entrySize);

        return new VariableWidthBlock(0, 1, copy, new int[] {0, copy.length()}, null);
    }

    @Override
    public long getEstimatedDataSizeForStats(int position)
    {
        return isNull(position) ? 0 : getSliceLength(position);
    }

    @Override
    public boolean isNull(int position)
    {
        checkReadablePosition(this, position);
        return isEntryNull(position);
    }
}
