/*
 * Copyright (c) 2020 fuzamei-33cn Group Holding Limited. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. fuzamei designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package cn.chain33.jvm.api;

/**
 * All chain33 state database operations are in this class
 */
public class LocalDB {
    // set value to local db
    public native boolean setLocal(byte[] key, byte[] value);

    // get value from local db
    public native byte[] getFromLocal(byte[] key);

    // set value to local db in format of string
    public native boolean setLocalInStr(String key, String value);

    // get value from local db in format of string
    public native String getFromLocalInStr(String key);

    private static native void registerNatives0();
}