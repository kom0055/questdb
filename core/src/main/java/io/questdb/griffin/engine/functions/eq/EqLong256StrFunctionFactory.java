/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2020 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin.engine.functions.eq;

import io.questdb.cairo.CairoConfiguration;
import io.questdb.cairo.sql.Function;
import io.questdb.cairo.sql.Record;
import io.questdb.griffin.AbstractBooleanFunctionFactory;
import io.questdb.griffin.FunctionFactory;
import io.questdb.griffin.SqlException;
import io.questdb.griffin.engine.functions.BooleanFunction;
import io.questdb.griffin.engine.functions.UnaryFunction;
import io.questdb.std.Long256;
import io.questdb.std.Long256FromCharSequenceDecoder;
import io.questdb.std.NumericException;
import io.questdb.std.ObjList;

public class EqLong256StrFunctionFactory extends AbstractBooleanFunctionFactory implements FunctionFactory {
    private static final ThreadLocal<Long256Decoder> DECODER = new ThreadLocal<>() {
        protected Long256Decoder initialValue() {
            return new Long256Decoder();
        };
    };

    @Override
    public String getSignature() {
        return "=(Hs)";
    }

    @Override
    public Function newInstance(ObjList<Function> args, int position, CairoConfiguration configuration) throws SqlException {
        final CharSequence hexLong256 = args.getQuick(1).getStr(null);
        try {
            return DECODER.get().newInstance(position, args.getQuick(0), hexLong256, isNegated);
        } catch (NumericException e) {
            throw SqlException.position(args.getQuick(1).getPosition()).put("invalid hex value for long256");
        }
    }

    private static class Func extends BooleanFunction implements UnaryFunction {
        private final boolean isNegated;
        private final Function arg;
        private final long long0;
        private final long long1;
        private final long long2;
        private final long long3;

        public Func(int position, Function arg, long long0, long long1, long long2, long long3, boolean isNegated) {
            super(position);
            this.arg = arg;
            this.long0 = long0;
            this.long1 = long1;
            this.long2 = long2;
            this.long3 = long3;
            this.isNegated = isNegated;
        }

        @Override
        public boolean getBool(Record rec) {
            final Long256 value = arg.getLong256A(rec);
            return isNegated != (value.getLong0() == long0 &&
                    value.getLong1() == long1 &&
                    value.getLong2() == long2 &&
                    value.getLong3() == long3);
        }

        @Override
        public Function getArg() {
            return arg;
        }
    }

    private static class Long256Decoder extends Long256FromCharSequenceDecoder {
        private long long0;
        private long long1;
        private long long2;
        private long long3;

        private Func newInstance(int position, Function arg, CharSequence hexLong256, boolean isNegated) throws NumericException {
            decode(hexLong256, 2, hexLong256.length());
            return new Func(position, arg, long0, long1, long2, long3, isNegated);
        }

        @Override
        protected void onDecoded(long l0, long l1, long l2, long l3) {
            long0 = l0;
            long1 = l1;
            long2 = l2;
            long3 = l3;
        }

    }
}
