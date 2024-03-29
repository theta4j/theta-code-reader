/*
 * Copyright (C) 2022 theta4j project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theta4j.codereader;

import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;

import androidx.annotation.NonNull;

import java.util.Objects;

final class ImageConverter {
    private final RenderScript rs;

    private final ScriptC_filters script;

    ImageConverter(@NonNull final Context context) {
        Objects.requireNonNull(context, "context can not be null.");

        this.rs = RenderScript.create(context);
        this.script = new ScriptC_filters(rs);
    }

    void correctDistortion(
            final int width, final int height,
            @NonNull final byte[] src,
            @NonNull final byte[] dst
    ) {
        Objects.requireNonNull(src, "src can not be null.");
        Objects.requireNonNull(dst, "dst can not be null.");

        final Type srcType = new Type.Builder(rs, Element.U8(rs)).setX(width).setY(height).create();
        final Allocation srcAlloc = Allocation.createTyped(rs, srcType, Allocation.USAGE_SCRIPT);
        srcAlloc.copyFromUnchecked(src);

        final Type dstType = new Type.Builder(rs, Element.U8(rs)).setX(width).setY(height).create();
        final Allocation dstAlloc = Allocation.createTyped(rs, dstType, Allocation.USAGE_SCRIPT);

        script.set_allocIn(srcAlloc);
        script.forEach_correctDistortion(srcAlloc, dstAlloc);

        dstAlloc.copyTo(dst);

        srcAlloc.destroy();
        dstAlloc.destroy();
    }

    void correctDistortionRGBA(
            final int width, final int height,
            @NonNull final byte[] src,
            @NonNull final byte[] dst
    ) {
        Objects.requireNonNull(src, "src can not be null.");
        Objects.requireNonNull(dst, "dst can not be null.");

        final Type srcType = new Type.Builder(rs, Element.U8(rs)).setX(width).setY(height).create();
        final Allocation srcAlloc = Allocation.createTyped(rs, srcType, Allocation.USAGE_SCRIPT);
        srcAlloc.copyFromUnchecked(src);

        final Type dstType = new Type.Builder(rs, Element.U8_4(rs)).setX(width).setY(height).create();
        final Allocation dstAlloc = Allocation.createTyped(rs, dstType, Allocation.USAGE_SCRIPT);

        script.set_allocIn(srcAlloc);
        script.forEach_correctDistortionRGBA(srcAlloc, dstAlloc);

        dstAlloc.copyTo(dst);

        srcAlloc.destroy();
        dstAlloc.destroy();
    }
}
