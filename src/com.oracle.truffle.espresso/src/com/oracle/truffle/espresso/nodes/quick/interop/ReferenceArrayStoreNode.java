/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.truffle.espresso.nodes.quick.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.espresso.EspressoLanguage;
import com.oracle.truffle.espresso.bytecode.Bytecodes;
import com.oracle.truffle.espresso.nodes.BytecodeNode;
import com.oracle.truffle.espresso.nodes.OperandStack;
import com.oracle.truffle.espresso.nodes.quick.QuickNode;
import com.oracle.truffle.espresso.runtime.EspressoContext;
import com.oracle.truffle.espresso.runtime.StaticObject;

public abstract class ReferenceArrayStoreNode extends QuickNode {
    protected static final int LIMIT = 3;

    protected ReferenceArrayStoreNode(int top, int callerBCI) {
        super(top, callerBCI);
    }

    @Override
    public final int execute(VirtualFrame frame, OperandStack stack) {
        StaticObject array = nullCheck(BytecodeNode.popObject(stack, top - 3));
        int index = BytecodeNode.popInt(stack, top - 2);
        StaticObject value = BytecodeNode.popObject(stack, top - 1);
        executeStore(array, index, value);
        return Bytecodes.stackEffectOf(Bytecodes.AASTORE);
    }

    abstract void executeStore(StaticObject array, int index, StaticObject value);

    @Specialization(guards = "array.isForeignObject()")
    void doForeign(StaticObject array, int index, StaticObject value,
                    @CachedLibrary(limit = "LIMIT") InteropLibrary interop,
                    @CachedContext(EspressoLanguage.class) EspressoContext context,
                    @Cached BranchProfile exceptionProfile) {
        Object unwrappedValue = value.isForeignObject() ? value.rawForeignObject() : value;
        ForeignArrayUtils.writeForeignArrayElement(array, index, unwrappedValue, interop, context.getMeta(), exceptionProfile);
    }

    @Specialization(guards = "array.isEspressoObject()")
    void doEspresso(StaticObject array, int index, StaticObject value) {
        getBytecodesNode().getInterpreterToVM().setArrayObject(value, index, array);
    }

    @Override
    public boolean producedForeignObject(OperandStack stack) {
        return false;
    }
}
