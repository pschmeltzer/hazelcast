/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.query.impl.predicates;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.query.impl.Comparables;
import com.hazelcast.query.impl.Index;
import com.hazelcast.query.impl.QueryContext;
import com.hazelcast.query.impl.QueryableEntry;

import java.io.IOException;
import java.util.Set;

import static com.hazelcast.query.impl.predicates.PredicateUtils.isNull;

/**
 * Range predicate that is bounded on both sides.
 * <p>
 * Instances of this class are never transferred between members, the
 * serialization is disabled.
 */
public class BoundedRangePredicate extends AbstractIndexAwarePredicate implements RangePredicate {

    private Comparable from;
    private boolean fromInclusive;
    private Comparable to;
    private boolean toInclusive;

    /**
     * Creates a new instance of bounded range predicate.
     *
     * @param attribute     the attribute to act on.
     * @param from          the lower/left range bound.
     * @param fromInclusive {@code true} if the range is left-closed,
     *                      {@code false} otherwise.
     * @param to            the upper/right range bound.
     * @param toInclusive   {@code true} if the range is right-closed,
     *                      {@code false} otherwise.
     */
    public BoundedRangePredicate(String attribute, Comparable from, boolean fromInclusive, Comparable to, boolean toInclusive) {
        super(attribute);

        if (isNull(from) || isNull(to)) {
            throw new IllegalArgumentException("range must be bounded");
        }

        this.from = from;
        this.fromInclusive = fromInclusive;
        this.to = to;
        this.toInclusive = toInclusive;
    }

    public BoundedRangePredicate() {
    }

    @Override
    public Set<QueryableEntry> filter(QueryContext queryContext) {
        Index index = matchIndex(queryContext, QueryContext.IndexMatchHint.PREFER_ORDERED);
        if (index == null) {
            return null;
        }
        return index.getRecords(from, fromInclusive, to, toInclusive);
    }

    @Override
    protected boolean applyForSingleAttributeValue(Comparable value) {
        if (value == null) {
            return false;
        }
        Comparable convertedValue = (Comparable) convertEnumValue(value);

        Comparable from = convert(value, this.from);
        int order = Comparables.compare(convertedValue, from);
        if (order < 0 || !fromInclusive && order == 0) {
            return false;
        }

        Comparable to = convert(value, this.to);
        order = Comparables.compare(convertedValue, to);
        return order < 0 || toInclusive && order == 0;
    }

    @Override
    public int getClassId() {
        return PredicateDataSerializerHook.BOUNDED_RANGE_PREDICATE;
    }

    @Override
    public String getAttribute() {
        return attributeName;
    }

    @Override
    public Comparable getFrom() {
        return from;
    }

    @Override
    public boolean isFromInclusive() {
        return fromInclusive;
    }

    @Override
    public Comparable getTo() {
        return to;
    }

    @Override
    public boolean isToInclusive() {
        return toInclusive;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        super.writeData(out);
        out.writeObject(from);
        out.writeBoolean(fromInclusive);
        out.writeObject(to);
        out.writeBoolean(toInclusive);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        super.readData(in);
        from = in.readObject();
        fromInclusive = in.readBoolean();
        to = in.readObject();
        toInclusive = in.readBoolean();
    }

    @Override
    public String toString() {
        return from + (fromInclusive ? " >= " : " > ") + attributeName + (toInclusive ? " <= " : " < ") + to;
    }
}
