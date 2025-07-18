/* Copyright 2021 Mozilla Foundation
 *
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

// ./test/core/imports.wast

// ./test/core/imports.wast:3
let $0 = instantiate(`(module
  (func (export "func"))
  (func (export "func-i32") (param i32))
  (func (export "func-f32") (param f32))
  (func (export "func->i32") (result i32) (i32.const 22))
  (func (export "func->f32") (result f32) (f32.const 11))
  (func (export "func-i32->i32") (param i32) (result i32) (local.get 0))
  (func (export "func-i64->i64") (param i64) (result i64) (local.get 0))
  (global (export "global-i32") i32 (i32.const 55))
  (global (export "global-f32") f32 (f32.const 44))
  (global (export "global-mut-i64") (mut i64) (i64.const 66))
  (table (export "table-10-inf") 10 funcref)
  (table (export "table-10-20") 10 20 funcref)
  (table (export "table64-10-inf") i64 10 funcref)
  (table (export "table64-10-20") i64 10 20 funcref)
  (memory (export "memory-2-inf") 2)
  (memory (export "memory-2-4") 2 4)
  (memory (export "memory64-2-inf") i64 2)
  (memory (export "memory64-2-4") i64 2 4)
  (tag (export "tag"))
  (tag \$tag-i32 (param i32))
  (export "tag-i32" (tag \$tag-i32))
  (tag (export "tag-f32") (param f32))
)`);

// ./test/core/imports.wast:28
register($0, `test`);

// ./test/core/imports.wast:33
let $1 = instantiate(`(module
  (type \$func_i32 (func (param i32)))
  (type \$func_i64 (func (param i64)))
  (type \$func_f32 (func (param f32)))
  (type \$func_f64 (func (param f64)))

  (import "spectest" "print_i32" (func (param i32)))
  (func (import "spectest" "print_i64") (param i64))
  (import "spectest" "print_i32" (func \$print_i32 (param i32)))
  (import "spectest" "print_i64" (func \$print_i64 (param i64)))
  (import "spectest" "print_f32" (func \$print_f32 (param f32)))
  (import "spectest" "print_f64" (func \$print_f64 (param f64)))
  (import "spectest" "print_i32_f32" (func \$print_i32_f32 (param i32 f32)))
  (import "spectest" "print_f64_f64" (func \$print_f64_f64 (param f64 f64)))
  (func \$print_i32-2 (import "spectest" "print_i32") (param i32))
  (func \$print_f64-2 (import "spectest" "print_f64") (param f64))
  (import "test" "func-i64->i64" (func \$i64->i64 (param i64) (result i64)))

  (tag (import "test" "tag-i32") (param i32))
  (import "test" "tag-f32" (tag (param f32)))

  (func (export "p1") (import "spectest" "print_i32") (param i32))
  (func \$p (export "p2") (import "spectest" "print_i32") (param i32))
  (func (export "p3") (export "p4") (import "spectest" "print_i32") (param i32))
  (func (export "p5") (import "spectest" "print_i32") (type 0))
  (func (export "p6") (import "spectest" "print_i32") (type 0) (param i32) (result))

  (import "spectest" "print_i32" (func (type \$forward)))
  (func (import "spectest" "print_i32") (type \$forward))
  (type \$forward (func (param i32)))

  (table funcref (elem \$print_i32 \$print_f64))

  (func (export "print32") (param \$i i32)
    (local \$x f32)
    (local.set \$x (f32.convert_i32_s (local.get \$i)))
    (call 0 (local.get \$i))
    (call \$print_i32_f32
      (i32.add (local.get \$i) (i32.const 1))
      (f32.const 42)
    )
    (call \$print_i32 (local.get \$i))
    (call \$print_i32-2 (local.get \$i))
    (call \$print_f32 (local.get \$x))
    (call_indirect (type \$func_i32) (local.get \$i) (i32.const 0))
  )

  (func (export "print64") (param \$i i64)
    (local \$x f64)
    (local.set \$x (f64.convert_i64_s (call \$i64->i64 (local.get \$i))))
    (call 1 (local.get \$i))
    (call \$print_f64_f64
      (f64.add (local.get \$x) (f64.const 1))
      (f64.const 53)
    )
    (call \$print_i64 (local.get \$i))
    (call \$print_f64 (local.get \$x))
    (call \$print_f64-2 (local.get \$x))
    (call_indirect (type \$func_f64) (local.get \$x) (i32.const 1))
  )
)`);

// ./test/core/imports.wast:95
assert_return(() => invoke($1, `print32`, [13]), []);

// ./test/core/imports.wast:96
assert_return(() => invoke($1, `print64`, [24n]), []);

// ./test/core/imports.wast:98
assert_invalid(
  () => instantiate(`(module 
    (type (func (result i32)))
    (import "test" "func" (func (type 1)))
  )`),
  `unknown type`,
);

// ./test/core/imports.wast:107
let $2 = instantiate(`(module
  (import "spectest" "print_i32" (func \$imported_print (param i32)))
  (func (export "print_i32") (param \$i i32)
    (call \$imported_print (local.get \$i))
  )
)`);

// ./test/core/imports.wast:114
assert_return(() => invoke($2, `print_i32`, [13]), []);

// ./test/core/imports.wast:117
let $3 = instantiate(`(module
  (import "spectest" "print_i32" (func \$imported_print (param i32)))
  (func (export "print_i32") (param \$i i32) (param \$j i32) (result i32)
    (i32.add (local.get \$i) (local.get \$j))
  )
)`);

// ./test/core/imports.wast:124
assert_return(() => invoke($3, `print_i32`, [5, 11]), [value("i32", 16)]);

// ./test/core/imports.wast:126
let $4 = instantiate(`(module (import "test" "func" (func)))`);

// ./test/core/imports.wast:127
let $5 = instantiate(`(module (import "test" "func-i32" (func (param i32))))`);

// ./test/core/imports.wast:128
let $6 = instantiate(`(module (import "test" "func-f32" (func (param f32))))`);

// ./test/core/imports.wast:129
let $7 = instantiate(`(module (import "test" "func->i32" (func (result i32))))`);

// ./test/core/imports.wast:130
let $8 = instantiate(`(module (import "test" "func->f32" (func (result f32))))`);

// ./test/core/imports.wast:131
let $9 = instantiate(`(module (import "test" "func-i32->i32" (func (param i32) (result i32))))`);

// ./test/core/imports.wast:132
let $10 = instantiate(`(module (import "test" "func-i64->i64" (func (param i64) (result i64))))`);

// ./test/core/imports.wast:134
assert_unlinkable(
  () => instantiate(`(module (import "test" "unknown" (func)))`),
  `unknown import`,
);

// ./test/core/imports.wast:138
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "unknown" (func)))`),
  `unknown import`,
);

// ./test/core/imports.wast:143
assert_unlinkable(
  () => instantiate(`(module (import "test" "func" (func (param i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:147
assert_unlinkable(
  () => instantiate(`(module (import "test" "func" (func (result i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:151
assert_unlinkable(
  () => instantiate(`(module (import "test" "func" (func (param i32) (result i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:155
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:159
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32" (func (result i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:163
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32" (func (param f32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:167
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32" (func (param i64))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:171
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32" (func (param i32) (result i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:175
assert_unlinkable(
  () => instantiate(`(module (import "test" "func->i32" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:179
assert_unlinkable(
  () => instantiate(`(module (import "test" "func->i32" (func (param i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:183
assert_unlinkable(
  () => instantiate(`(module (import "test" "func->i32" (func (result f32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:187
assert_unlinkable(
  () => instantiate(`(module (import "test" "func->i32" (func (result i64))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:191
assert_unlinkable(
  () => instantiate(`(module (import "test" "func->i32" (func (param i32) (result i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:195
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32->i32" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:199
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32->i32" (func (param i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:203
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32->i32" (func (result i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:208
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-i32" (func (result i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:212
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-inf" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:216
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:220
assert_unlinkable(
  () => instantiate(`(module (import "test" "tag" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:224
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "global_i32" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:228
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "table" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:232
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "memory" (func)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:237
assert_unlinkable(
  () => instantiate(`(module (tag (import "test" "unknown")))`),
  `unknown import`,
);

// ./test/core/imports.wast:241
assert_unlinkable(
  () => instantiate(`(module (tag (import "test" "tag") (param f32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:245
assert_unlinkable(
  () => instantiate(`(module (tag (import "test" "tag-i32")))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:249
assert_unlinkable(
  () => instantiate(`(module (tag (import "test" "tag-i32") (param f32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:253
assert_unlinkable(
  () => instantiate(`(module (tag (import "test" "func-i32") (param f32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:261
let $11 = instantiate(`(module
  (import "spectest" "global_i32" (global i32))
  (global (import "spectest" "global_i32") i32)

  (import "spectest" "global_i32" (global \$x i32))
  (global \$y (import "spectest" "global_i32") i32)

  (import "spectest" "global_i64" (global i64))
  (import "spectest" "global_f32" (global f32))
  (import "spectest" "global_f64" (global f64))

  (func (export "get-0") (result i32) (global.get 0))
  (func (export "get-1") (result i32) (global.get 1))
  (func (export "get-x") (result i32) (global.get \$x))
  (func (export "get-y") (result i32) (global.get \$y))
  (func (export "get-4") (result i64) (global.get 4))
  (func (export "get-5") (result f32) (global.get 5))
  (func (export "get-6") (result f64) (global.get 6))
)`);

// ./test/core/imports.wast:281
assert_return(() => invoke($11, `get-0`, []), [value("i32", 666)]);

// ./test/core/imports.wast:282
assert_return(() => invoke($11, `get-1`, []), [value("i32", 666)]);

// ./test/core/imports.wast:283
assert_return(() => invoke($11, `get-x`, []), [value("i32", 666)]);

// ./test/core/imports.wast:284
assert_return(() => invoke($11, `get-y`, []), [value("i32", 666)]);

// ./test/core/imports.wast:285
assert_return(() => invoke($11, `get-4`, []), [value("i64", 666n)]);

// ./test/core/imports.wast:286
assert_return(() => invoke($11, `get-5`, []), [value("f32", 666.6)]);

// ./test/core/imports.wast:287
assert_return(() => invoke($11, `get-6`, []), [value("f64", 666.6)]);

// ./test/core/imports.wast:289
let $12 = instantiate(`(module (import "test" "global-i32" (global i32)))`);

// ./test/core/imports.wast:290
let $13 = instantiate(`(module (import "test" "global-f32" (global f32)))`);

// ./test/core/imports.wast:291
let $14 = instantiate(`(module (import "test" "global-mut-i64" (global (mut i64))))`);

// ./test/core/imports.wast:293
assert_unlinkable(
  () => instantiate(`(module (import "test" "unknown" (global i32)))`),
  `unknown import`,
);

// ./test/core/imports.wast:297
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "unknown" (global i32)))`),
  `unknown import`,
);

// ./test/core/imports.wast:302
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-i32" (global i64)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:306
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-i32" (global f32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:310
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-i32" (global f64)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:314
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-i32" (global (mut i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:318
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-f32" (global i32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:322
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-f32" (global i64)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:326
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-f32" (global f64)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:330
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-f32" (global (mut f32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:334
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-mut-i64" (global (mut i32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:338
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-mut-i64" (global (mut f32))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:342
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-mut-i64" (global (mut f64))))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:346
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-mut-i64" (global i64)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:351
assert_unlinkable(
  () => instantiate(`(module (import "test" "func" (global i32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:355
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-inf" (global i32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:359
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (global i32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:363
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "print_i32" (global i32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:367
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "table" (global i32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:371
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "memory" (global i32)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:379
let $15 = instantiate(`(module
  (type (func (result i32)))
  (import "spectest" "table" (table \$tab 10 20 funcref))
  (import "test" "table64-10-inf" (table \$tab64 i64 10 funcref))
  (elem (table \$tab) (i32.const 1) func \$f \$g)

  (func (export "call") (param i32) (result i32)
    (call_indirect \$tab (type 0) (local.get 0))
  )
  (func \$f (result i32) (i32.const 11))
  (func \$g (result i32) (i32.const 22))
)`);

// ./test/core/imports.wast:392
assert_trap(() => invoke($15, `call`, [0]), `uninitialized element`);

// ./test/core/imports.wast:393
assert_return(() => invoke($15, `call`, [1]), [value("i32", 11)]);

// ./test/core/imports.wast:394
assert_return(() => invoke($15, `call`, [2]), [value("i32", 22)]);

// ./test/core/imports.wast:395
assert_trap(() => invoke($15, `call`, [3]), `uninitialized element`);

// ./test/core/imports.wast:396
assert_trap(() => invoke($15, `call`, [100]), `undefined element`);

// ./test/core/imports.wast:399
let $16 = instantiate(`(module
  (type (func (result i32)))
  (table \$tab (import "spectest" "table") 10 20 funcref)
  (table \$tab64 (import "test" "table64-10-inf") i64 10 funcref)
  (elem (table \$tab) (i32.const 1) func \$f \$g)

  (func (export "call") (param i32) (result i32)
    (call_indirect \$tab (type 0) (local.get 0))
  )
  (func \$f (result i32) (i32.const 11))
  (func \$g (result i32) (i32.const 22))
)`);

// ./test/core/imports.wast:412
assert_trap(() => invoke($16, `call`, [0]), `uninitialized element`);

// ./test/core/imports.wast:413
assert_return(() => invoke($16, `call`, [1]), [value("i32", 11)]);

// ./test/core/imports.wast:414
assert_return(() => invoke($16, `call`, [2]), [value("i32", 22)]);

// ./test/core/imports.wast:415
assert_trap(() => invoke($16, `call`, [3]), `uninitialized element`);

// ./test/core/imports.wast:416
assert_trap(() => invoke($16, `call`, [100]), `undefined element`);

// ./test/core/imports.wast:418
let $17 = instantiate(`(module
  (import "spectest" "table" (table 0 funcref))
  (import "spectest" "table" (table 0 funcref))
  (import "test" "table64-10-inf" (table i64 10 funcref))
  (import "test" "table64-10-inf" (table i64 10 funcref))
  (table 10 funcref)
  (table 10 funcref)
  (table i64 10 funcref)
  (table i64 10 funcref)
)`);

// ./test/core/imports.wast:429
let $18 = instantiate(`(module (import "test" "table-10-inf" (table 10 funcref)))`);

// ./test/core/imports.wast:430
let $19 = instantiate(`(module (import "test" "table-10-inf" (table 5 funcref)))`);

// ./test/core/imports.wast:431
let $20 = instantiate(`(module (import "test" "table-10-inf" (table 0 funcref)))`);

// ./test/core/imports.wast:432
let $21 = instantiate(`(module (import "test" "table-10-20" (table 10 funcref)))`);

// ./test/core/imports.wast:433
let $22 = instantiate(`(module (import "test" "table-10-20" (table 5 funcref)))`);

// ./test/core/imports.wast:434
let $23 = instantiate(`(module (import "test" "table-10-20" (table 0 funcref)))`);

// ./test/core/imports.wast:435
let $24 = instantiate(`(module (import "test" "table-10-20" (table 10 20 funcref)))`);

// ./test/core/imports.wast:436
let $25 = instantiate(`(module (import "test" "table-10-20" (table 5 20 funcref)))`);

// ./test/core/imports.wast:437
let $26 = instantiate(`(module (import "test" "table-10-20" (table 0 20 funcref)))`);

// ./test/core/imports.wast:438
let $27 = instantiate(`(module (import "test" "table-10-20" (table 10 25 funcref)))`);

// ./test/core/imports.wast:439
let $28 = instantiate(`(module (import "test" "table-10-20" (table 5 25 funcref)))`);

// ./test/core/imports.wast:440
let $29 = instantiate(`(module (import "test" "table-10-20" (table 0 25 funcref)))`);

// ./test/core/imports.wast:441
let $30 = instantiate(`(module (import "test" "table64-10-inf" (table i64 10 funcref)))`);

// ./test/core/imports.wast:442
let $31 = instantiate(`(module (import "test" "table64-10-inf" (table i64 5 funcref)))`);

// ./test/core/imports.wast:443
let $32 = instantiate(`(module (import "test" "table64-10-inf" (table i64 0 funcref)))`);

// ./test/core/imports.wast:444
let $33 = instantiate(`(module (import "test" "table64-10-20" (table i64 10 funcref)))`);

// ./test/core/imports.wast:445
let $34 = instantiate(`(module (import "test" "table64-10-20" (table i64 5 funcref)))`);

// ./test/core/imports.wast:446
let $35 = instantiate(`(module (import "test" "table64-10-20" (table i64 0 funcref)))`);

// ./test/core/imports.wast:447
let $36 = instantiate(`(module (import "test" "table64-10-20" (table i64 10 20 funcref)))`);

// ./test/core/imports.wast:448
let $37 = instantiate(`(module (import "test" "table64-10-20" (table i64 5 20 funcref)))`);

// ./test/core/imports.wast:449
let $38 = instantiate(`(module (import "test" "table64-10-20" (table i64 0 20 funcref)))`);

// ./test/core/imports.wast:450
let $39 = instantiate(`(module (import "test" "table64-10-20" (table i64 10 25 funcref)))`);

// ./test/core/imports.wast:451
let $40 = instantiate(`(module (import "test" "table64-10-20" (table i64 5 25 funcref)))`);

// ./test/core/imports.wast:452
let $41 = instantiate(`(module (import "test" "table64-10-20" (table i64 0 25 funcref)))`);

// ./test/core/imports.wast:453
let $42 = instantiate(`(module (import "spectest" "table" (table 10 funcref)))`);

// ./test/core/imports.wast:454
let $43 = instantiate(`(module (import "spectest" "table" (table 5 funcref)))`);

// ./test/core/imports.wast:455
let $44 = instantiate(`(module (import "spectest" "table" (table 0 funcref)))`);

// ./test/core/imports.wast:456
let $45 = instantiate(`(module (import "spectest" "table" (table 10 20 funcref)))`);

// ./test/core/imports.wast:457
let $46 = instantiate(`(module (import "spectest" "table" (table 5 20 funcref)))`);

// ./test/core/imports.wast:458
let $47 = instantiate(`(module (import "spectest" "table" (table 0 20 funcref)))`);

// ./test/core/imports.wast:459
let $48 = instantiate(`(module (import "spectest" "table" (table 10 25 funcref)))`);

// ./test/core/imports.wast:460
let $49 = instantiate(`(module (import "spectest" "table" (table 5 25 funcref)))`);

// ./test/core/imports.wast:462
assert_unlinkable(
  () => instantiate(`(module (import "test" "unknown" (table 10 funcref)))`),
  `unknown import`,
);

// ./test/core/imports.wast:466
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "unknown" (table 10 funcref)))`),
  `unknown import`,
);

// ./test/core/imports.wast:471
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-inf" (table 12 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:475
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-inf" (table 10 20 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:479
assert_unlinkable(
  () => instantiate(`(module (import "test" "table64-10-inf" (table i64 12 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:483
assert_unlinkable(
  () => instantiate(`(module (import "test" "table64-10-inf" (table i64 10 20 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:487
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-20" (table 12 20 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:491
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-20" (table 10 18 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:495
assert_unlinkable(
  () => instantiate(`(module (import "test" "table64-10-20" (table i64 12 20 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:499
assert_unlinkable(
  () => instantiate(`(module (import "test" "table64-10-20" (table i64 10 18 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:503
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "table" (table 12 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:507
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "table" (table 10 15 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:512
assert_unlinkable(
  () => instantiate(`(module (import "test" "func" (table 10 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:516
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-i32" (table 10 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:520
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (table 10 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:524
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "print_i32" (table 10 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:529
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-inf" (table i64 10 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:533
assert_unlinkable(
  () => instantiate(`(module (import "test" "table64-10-inf" (table 10 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:537
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-20" (table i64 10 20 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:541
assert_unlinkable(
  () => instantiate(`(module (import "test" "table64-10-20" (table 10 20 funcref)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:549
let $50 = instantiate(`(module
  (import "spectest" "memory" (memory 1 2))
  (import "test" "memory-2-inf" (memory 2))
  (import "test" "memory64-2-inf" (memory i64 2))
  (data (memory 0) (i32.const 10) "\\10")

  (func (export "load") (param i32) (result i32) (i32.load (local.get 0)))
)`);

// ./test/core/imports.wast:558
assert_return(() => invoke($50, `load`, [0]), [value("i32", 0)]);

// ./test/core/imports.wast:559
assert_return(() => invoke($50, `load`, [10]), [value("i32", 16)]);

// ./test/core/imports.wast:560
assert_return(() => invoke($50, `load`, [8]), [value("i32", 1048576)]);

// ./test/core/imports.wast:561
assert_trap(() => invoke($50, `load`, [1000000]), `out of bounds memory access`);

// ./test/core/imports.wast:563
let $51 = instantiate(`(module
  (memory (import "spectest" "memory") 1 2)
  (memory (import "test" "memory-2-inf") 2)
  (memory (import "test" "memory64-2-inf") i64 2)
  (data (memory 0) (i32.const 10) "\\10")

  (func (export "load") (param i32) (result i32) (i32.load (local.get 0)))
)`);

// ./test/core/imports.wast:571
assert_return(() => invoke($51, `load`, [0]), [value("i32", 0)]);

// ./test/core/imports.wast:572
assert_return(() => invoke($51, `load`, [10]), [value("i32", 16)]);

// ./test/core/imports.wast:573
assert_return(() => invoke($51, `load`, [8]), [value("i32", 1048576)]);

// ./test/core/imports.wast:574
assert_trap(() => invoke($51, `load`, [1000000]), `out of bounds memory access`);

// ./test/core/imports.wast:576
let $52 = instantiate(`(module (import "test" "memory-2-inf" (memory 2)))`);

// ./test/core/imports.wast:577
let $53 = instantiate(`(module (import "test" "memory-2-inf" (memory 1)))`);

// ./test/core/imports.wast:578
let $54 = instantiate(`(module (import "test" "memory-2-inf" (memory 0)))`);

// ./test/core/imports.wast:579
let $55 = instantiate(`(module (import "test" "memory-2-4" (memory 2)))`);

// ./test/core/imports.wast:580
let $56 = instantiate(`(module (import "test" "memory-2-4" (memory 1)))`);

// ./test/core/imports.wast:581
let $57 = instantiate(`(module (import "test" "memory-2-4" (memory 0)))`);

// ./test/core/imports.wast:582
let $58 = instantiate(`(module (import "test" "memory-2-4" (memory 2 4)))`);

// ./test/core/imports.wast:583
let $59 = instantiate(`(module (import "test" "memory-2-4" (memory 1 4)))`);

// ./test/core/imports.wast:584
let $60 = instantiate(`(module (import "test" "memory-2-4" (memory 0 4)))`);

// ./test/core/imports.wast:585
let $61 = instantiate(`(module (import "test" "memory-2-4" (memory 2 5)))`);

// ./test/core/imports.wast:586
let $62 = instantiate(`(module (import "test" "memory-2-4" (memory 2 6)))`);

// ./test/core/imports.wast:587
let $63 = instantiate(`(module (import "test" "memory64-2-inf" (memory i64 2)))`);

// ./test/core/imports.wast:588
let $64 = instantiate(`(module (import "test" "memory64-2-inf" (memory i64 1)))`);

// ./test/core/imports.wast:589
let $65 = instantiate(`(module (import "test" "memory64-2-inf" (memory i64 0)))`);

// ./test/core/imports.wast:590
let $66 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 2)))`);

// ./test/core/imports.wast:591
let $67 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 1)))`);

// ./test/core/imports.wast:592
let $68 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 0)))`);

// ./test/core/imports.wast:593
let $69 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 2 4)))`);

// ./test/core/imports.wast:594
let $70 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 1 4)))`);

// ./test/core/imports.wast:595
let $71 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 0 4)))`);

// ./test/core/imports.wast:596
let $72 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 2 5)))`);

// ./test/core/imports.wast:597
let $73 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 1 5)))`);

// ./test/core/imports.wast:598
let $74 = instantiate(`(module (import "test" "memory64-2-4" (memory i64 0 5)))`);

// ./test/core/imports.wast:599
let $75 = instantiate(`(module (import "spectest" "memory" (memory 1)))`);

// ./test/core/imports.wast:600
let $76 = instantiate(`(module (import "spectest" "memory" (memory 0)))`);

// ./test/core/imports.wast:601
let $77 = instantiate(`(module (import "spectest" "memory" (memory 1 2)))`);

// ./test/core/imports.wast:602
let $78 = instantiate(`(module (import "spectest" "memory" (memory 0 2)))`);

// ./test/core/imports.wast:603
let $79 = instantiate(`(module (import "spectest" "memory" (memory 1 3)))`);

// ./test/core/imports.wast:604
let $80 = instantiate(`(module (import "spectest" "memory" (memory 0 3)))`);

// ./test/core/imports.wast:606
assert_unlinkable(
  () => instantiate(`(module (import "test" "unknown" (memory 1)))`),
  `unknown import`,
);

// ./test/core/imports.wast:610
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "unknown" (memory 1)))`),
  `unknown import`,
);

// ./test/core/imports.wast:615
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (memory 0 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:619
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (memory 0 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:623
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (memory 0 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:627
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (memory 2 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:631
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (memory 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:635
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 0 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:639
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 0 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:643
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 0 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:647
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 2 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:651
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 2 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:655
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 3 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:659
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 3 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:663
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 3 5)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:667
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 4 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:671
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 4 5)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:675
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:679
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:683
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory 5)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:687
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-inf" (memory i64 0 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:691
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-inf" (memory i64 0 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:695
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-inf" (memory i64 0 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:699
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-inf" (memory i64 2 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:703
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-inf" (memory i64 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:707
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 0 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:711
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 0 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:715
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 0 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:719
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 2 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:723
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 2 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:727
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 3 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:731
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 3 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:735
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 3 5)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:739
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 4 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:743
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 4 5)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:747
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 3)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:751
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:755
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory i64 5)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:759
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "memory" (memory 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:763
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "memory" (memory 1 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:768
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-inf" (memory i64 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:772
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-inf" (memory 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:776
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory-2-4" (memory i64 2 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:780
assert_unlinkable(
  () => instantiate(`(module (import "test" "memory64-2-4" (memory 2 4)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:785
assert_unlinkable(
  () => instantiate(`(module (import "test" "func-i32" (memory 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:789
assert_unlinkable(
  () => instantiate(`(module (import "test" "global-i32" (memory 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:793
assert_unlinkable(
  () => instantiate(`(module (import "test" "table-10-inf" (memory 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:797
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "print_i32" (memory 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:801
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "global_i32" (memory 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:805
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "table" (memory 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:810
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "memory" (memory 2)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:814
assert_unlinkable(
  () => instantiate(`(module (import "spectest" "memory" (memory 1 1)))`),
  `incompatible import type`,
);

// ./test/core/imports.wast:819
let $81 = instantiate(`(module
  (import "spectest" "memory" (memory 0 3))  ;; actual has max size 2
  (func (export "grow") (param i32) (result i32) (memory.grow (local.get 0)))
)`);

// ./test/core/imports.wast:823
assert_return(() => invoke($81, `grow`, [0]), [value("i32", 1)]);

// ./test/core/imports.wast:824
assert_return(() => invoke($81, `grow`, [1]), [value("i32", 1)]);

// ./test/core/imports.wast:825
assert_return(() => invoke($81, `grow`, [0]), [value("i32", 2)]);

// ./test/core/imports.wast:826
assert_return(() => invoke($81, `grow`, [1]), [value("i32", -1)]);

// ./test/core/imports.wast:827
assert_return(() => invoke($81, `grow`, [0]), [value("i32", 2)]);

// ./test/core/imports.wast:832
assert_malformed(
  () => instantiate(`(func) (import "" "" (func)) `),
  `import after function`,
);

// ./test/core/imports.wast:836
assert_malformed(
  () => instantiate(`(func) (import "" "" (global i64)) `),
  `import after function`,
);

// ./test/core/imports.wast:840
assert_malformed(
  () => instantiate(`(func) (import "" "" (table 0 funcref)) `),
  `import after function`,
);

// ./test/core/imports.wast:844
assert_malformed(
  () => instantiate(`(func) (import "" "" (memory 0)) `),
  `import after function`,
);

// ./test/core/imports.wast:849
assert_malformed(
  () => instantiate(`(global i64 (i64.const 0)) (import "" "" (func)) `),
  `import after global`,
);

// ./test/core/imports.wast:853
assert_malformed(
  () => instantiate(`(global i64 (i64.const 0)) (import "" "" (global f32)) `),
  `import after global`,
);

// ./test/core/imports.wast:857
assert_malformed(
  () => instantiate(`(global i64 (i64.const 0)) (import "" "" (table 0 funcref)) `),
  `import after global`,
);

// ./test/core/imports.wast:861
assert_malformed(
  () => instantiate(`(global i64 (i64.const 0)) (import "" "" (memory 0)) `),
  `import after global`,
);

// ./test/core/imports.wast:866
assert_malformed(
  () => instantiate(`(table 0 funcref) (import "" "" (func)) `),
  `import after table`,
);

// ./test/core/imports.wast:870
assert_malformed(
  () => instantiate(`(table 0 funcref) (import "" "" (global i32)) `),
  `import after table`,
);

// ./test/core/imports.wast:874
assert_malformed(
  () => instantiate(`(table 0 funcref) (import "" "" (table 0 funcref)) `),
  `import after table`,
);

// ./test/core/imports.wast:878
assert_malformed(
  () => instantiate(`(table 0 funcref) (import "" "" (memory 0)) `),
  `import after table`,
);

// ./test/core/imports.wast:883
assert_malformed(
  () => instantiate(`(memory 0) (import "" "" (func)) `),
  `import after memory`,
);

// ./test/core/imports.wast:887
assert_malformed(
  () => instantiate(`(memory 0) (import "" "" (global i32)) `),
  `import after memory`,
);

// ./test/core/imports.wast:891
assert_malformed(
  () => instantiate(`(memory 0) (import "" "" (table 1 3 funcref)) `),
  `import after memory`,
);

// ./test/core/imports.wast:895
assert_malformed(
  () => instantiate(`(memory 0) (import "" "" (memory 1 2)) `),
  `import after memory`,
);

// ./test/core/imports.wast:903
let $82 = instantiate(`(module)`);

// ./test/core/imports.wast:904
register($82, `not wasm`);

// ./test/core/imports.wast:905
assert_unlinkable(
  () => instantiate(`(module
    (import "not wasm" "overloaded" (func))
    (import "not wasm" "overloaded" (func (param i32)))
    (import "not wasm" "overloaded" (func (param i32 i32)))
    (import "not wasm" "overloaded" (func (param i64)))
    (import "not wasm" "overloaded" (func (param f32)))
    (import "not wasm" "overloaded" (func (param f64)))
    (import "not wasm" "overloaded" (func (result i32)))
    (import "not wasm" "overloaded" (func (result i64)))
    (import "not wasm" "overloaded" (func (result f32)))
    (import "not wasm" "overloaded" (func (result f64)))
    (import "not wasm" "overloaded" (global i32))
    (import "not wasm" "overloaded" (global i64))
    (import "not wasm" "overloaded" (global f32))
    (import "not wasm" "overloaded" (global f64))
    (import "not wasm" "overloaded" (table 0 funcref))
    (import "not wasm" "overloaded" (memory 0))
  )`),
  `unknown import`,
);
